package mbot;

import scc.*;

// IB imports
import com.ib.client.*;

// Thrift imports
import org.apache.cassandra.thrift.*;

// Java imports
import java.lang.Math;
import java.util.Enumeration;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;

//
// This is a controller responsible for handling the real-time
// stock market datafeed (MktStk). 
//

public class HistoricalStkController extends Controller
{

    //
    // Availability information.
    // Historical data requests are subject to the following limitations:
    // -Historical data requests can go back one full calendar year.
    // -Each request is restricted to duration and bar size values that return no more than 2000 bars (2000 bars per request).
    //

    //
    // Throttling information.
    // The following conditions can cause a pacing violation:
    // -Making identical historical data requests within 15 seconds;
    // -Making six or more historical data requests for the same Contract, Exchange and Tick Type within two seconds.
    //
    // Also, observe the following limitation when requesting historical data:
    // -Do not make more than 60 historical data requests in any ten-minute period.
    //

    //
    // The last time we scheduled a request to run
    //
    long lastRequestTimeMS = 0;

    //
    // How long to wait before another request can be issued.
    // From above, the rate should allow once every 10 seconds,
    // but we'll do once every 12 seconds to be safe.
    //
    long interRequestDelayMS = 12 * 1000;

    //
    // Helpful units of time 
    //
    public static final long TIME_30_MINUTES_IN_MS = 30*60*1000;
    public static final long TIME_1_YEAR_IN_MS = 31556926000L;//365*24*60*60*1000;

    //
    // The current symbol we are working on
    //
    String currentSymbol = null;

    //
    // Constructor
    //
    public HistoricalStkController( Scheduler scheduler,
                                    CassandraSubsystem cassandraSubsystem,
                                    TwsSubsystem twsSubsystem )
    {

        this.scheduler = scheduler;
        this.cassandraSubsystem = cassandraSubsystem;
        this.twsSubsystem = twsSubsystem;

        controllerName = "HistoricalStk";
        cassandraKeyspace = "HistoricalStk";

        pendingJobs = new LinkedList<Job>();

    }

    //
    // Cassandra stores values in byte[].
    // Java natively represents time as a Long, in milliseconds.
    //

    //
    // CasByte[] -> Long
    //
    private Long convertDateCasByteToLong( byte[] time )
    {
        String timeString = new String( time );

        return Long.valueOf( timeString );
    }

    //
    // Long -> CasByte[]
    //
    private byte[] convertDateLongToCasByte( Long time )
    {
        String timeString = time.toString();

        return timeString.getBytes();
    }

    //
    // We get work (i.e. symbols to request data) from the cassandra control data
    //
    public synchronized void pullWorkFromControl()
        throws ControllerException
    {

        //
        // Load controller metadata
        //
        try
        {
            HashMap controlColumns = cassandraSubsystem.getSlice( controlKeyspace, controllerName, "1SEC_TRADES" );
            Collection<String> symbols = controlColumns.keySet();

            /*
            Collection<byte[]> vals = controlColumns.values();
            for( byte[] v : vals )
            {
                System.out.println("Vals" +v + " " + new String(v));
            }
            Collection<byte[]> keys = controlColumns.keySet();
            for( byte[] k : keys )
            {
                System.out.println("Keys" +k + " " + new String(k) + " " + controlColumns.get(k));
                if( currentSymbol != null)
                {
                    byte[] casByteTime = (byte[]) controlColumns.get( currentSymbolBytes );
                    System.out.println("asdf "+currentSymbolBytes+" "+new String(currentSymbolBytes)+" "+casByteTime);
                }
            }
            */
            //Iterator<byte[]> symbol = symbols.iterator();

            for( String symbol : symbols )
            {

                //
                // If we aren't already working on a symbol, get one
                //
                if( currentSymbol == null )
                {

                    //
                    // Just pull one symbol and use that, order doesn't matter
                    //
                    currentSymbol = symbol;

                }

                //
                // Get the endDate
                //
                byte[] casByteTime = (byte[]) controlColumns.get( currentSymbol );

                Long endDateTime = convertDateCasByteToLong( casByteTime );

                Long now = getTimestampMS();

                //
                // We don't want to request historical data that .. well, doesn't exist.
                // Break off on this attempt and try the next symbol.
                //
                if( endDateTime > now )
                {
                    currentSymbol = null;
                    continue;
                }

                //
                // Queue work item
                //
                queueRequest( currentSymbol, endDateTime );

                break;
            }

        }
        catch( Exception e )
        {
            throw new ControllerException("Exception pulling work from control: " + e.getMessage());
        }

    }

    //
    // Queue up a work item, while respecting the throttling rates.
    // When the job is queued to run isn't quite the same as when the 
    // request actually goes to the TWS, so there is a bit of fuzziness
    // here, and we could hit the throttling rate. To account for this,
    // the Control data isn't updated until we we have all the data.
    //
    private synchronized void queueRequest( String symbol, Long endDateTime )
        throws ControllerException
    {

        System.out.println("Requesting " + symbol+ " " + endDateTime);

        //
        // Figure out when we can run this job
        //
        long nowMS = getTimestampMS();

        long nextRequestTimeMS = lastRequestTimeMS + interRequestDelayMS;

        long queueTime = Math.max( nowMS, nextRequestTimeMS );

        HistoricalStkJob job = null;

        //
        // Make and schedule the job
        //
        try
        {
            job = new HistoricalStkJob( HistoricalStkJob.JOB_REQUEST_1SEC_TRADES,
                                                         twsSubsystem,
                                                         this,
                                                         symbol,
                                                         endDateTime );
        }
        catch( Exception e )
        {
            throw new ControllerException("Exception creating job: " + e.getMessage());
        }

        try
        {
            scheduler.scheduleAtTime( job, queueTime );
        }
        catch( Exception e )
        {
            throw new ControllerException("Exception scheduling job: " + e.getMessage());
        }

        //
        // Remember this queueTime for later
        //
        lastRequestTimeMS = queueTime;

    }

    //
    // Add or remove a symbol to the control set of symbols
    //
    public synchronized void addToControl( String symbol, Long endDate )
        throws ControllerException
    {
        try
        {
            cassandraSubsystem.insert( controlKeyspace,
                                       controllerName,
                                       "1SEC_TRADES",
                                       symbol,
                                       convertDateLongToCasByte( endDate ) );
        }
        catch( Exception e )
        {
            throw new ControllerException("Exception adding to control: " + e.getMessage());
        }
    }

    public synchronized void removeFromControl( String symbol )
        throws ControllerException
    {
        try
        {
            cassandraSubsystem.delete( controlKeyspace,
                                       controllerName,
                                       "1SEC_TRADES",
                                       symbol );
        }
        catch( Exception e )
        {
            throw new ControllerException("Exception adding to control: " + e.getMessage());
        }
    }

    //
    // Adjust the endDate stored in control to reflect our current progress
    //
    private synchronized void adjustControl( String symbol, long adjustment )
    {

        try
        {

            byte[] casByteTime = (byte[]) cassandraSubsystem.get( controlKeyspace, controllerName, "1SEC_TRADES", symbol );
            System.out.println("ttt "+casByteTime);
            Long endDateTime = convertDateCasByteToLong( casByteTime );
            System.out.println("ttt "+endDateTime);

            endDateTime += adjustment;
            System.out.println("ttt "+endDateTime);

            casByteTime = convertDateLongToCasByte( endDateTime );
            System.out.println("ttt "+casByteTime);

            cassandraSubsystem.insert( controlKeyspace, controllerName, "1SEC_TRADES", symbol, casByteTime );
            System.out.println("ttt "+symbol );

        }
        catch( Exception e )
        {
            System.out.println( e.getMessage() );
        }

    }

    //
    // Called by the scheduler when the job is complete
    //
    public synchronized void completionCallback( Job job )
    {
        pendingJobs.remove( job );

        //
        // Wakeup anything waiting on pendingJobs to finish,
        // e.g. cancelFeeds()
        //
        notify();
    }


    //
    // Psuedo-EWrapper interface
    //

    //
    // Main historical data callbacks
    //
    
    public void callbackHistoricalData( String symbol,
                                        Long startDate,
                                        Double open,
                                        Double high,
                                        Double low,
                                        Double close,
                                        Integer volume,
                                        Integer count,
                                        Double WAP,
                                        boolean hasGaps )
    {
        System.out.println( "HistoricalData: "+symbol+
                            " "+convertTimeLongToDate(startDate)+
                            " "+open+
                            " "+high+
                            " "+low+
                            " "+close+
                            " "+volume+
                            " "+count+
                            " "+WAP+
                            " "+hasGaps );
    }
    
    public void callbackHistoricalDataFinished( String symbol )
    {
        System.out.println( "HistoricalDataFinished: "+symbol );

        adjustControl( symbol, TIME_30_MINUTES_IN_MS );

        try
        {
            pullWorkFromControl();
        }
        catch( Exception e )
        {
            System.out.print("Failed to pull work: "+e.getMessage());
        }

    }
    

    /*
    public void tickPrice( int tickerId, int field, double price, int canAutoExecute )
    {
        //
        // Get the symbol based on this feeds clientId
        //
        String symbol = clientIdToSymbol.get( tickerId );

        String timeStamp = getTimestampStringMS();

        String date = getDate();

    	String msg = EWrapperMsgGenerator.tickPrice( tickerId, field, price, canAutoExecute );

        cassandraClient.insert( cassandraKeyspace,
                                "TickPrice",
                                symbol,
                                date,
                                timeStamp,
                                msg.getBytes() );
    }

    public void tickSize( int tickerId, int field, int size )
    {
        //
        // Get the symbol based on this feeds clientId
        //
        String symbol = clientIdToSymbol.get( tickerId );

        String timeStamp = getTimestampStringMS();

        String date = getDate();

    	String msg = EWrapperMsgGenerator.tickSize( tickerId, field, size );

        cassandraClient.insert( cassandraKeyspace,
                                "TickSize",
                                symbol,
                                date,
                                timeStamp,
                                msg.getBytes() );
    }

    public void tickGeneric( int tickerId, int tickType, double value )
    {
        //
        // Get the symbol based on this feeds clientId
        //
        String symbol = clientIdToSymbol.get( tickerId );

        String timeStamp = getTimestampStringMS();

        String date = getDate();

    	String msg = EWrapperMsgGenerator.tickGeneric( tickerId, tickType, value );

        cassandraClient.insert( cassandraKeyspace,
                                "TickGeneric",
                                symbol,
                                date,
                                timeStamp,
                                msg.getBytes() );
    }

    public void tickString( int tickerId, int tickType, String value )
    {
        //
        // Get the symbol based on this feeds clientId
        //
        String symbol = clientIdToSymbol.get( tickerId );

        String timeStamp = getTimestampStringMS();

        String date = getDate();

    	String msg = EWrapperMsgGenerator.tickString( tickerId, tickType, value );

        cassandraClient.insert( cassandraKeyspace,
                                "TickString",
                                symbol,
                                date,
                                timeStamp,
                                msg.getBytes() );
    }

    public void connectionClosed()
    {
        //String msg = EWrapperMsgGenerator.connectionClosed();
        System.out.println("Disconnected from TWS server");

        //
        // Null out the twsClient so no one uses it
        //
        twsClient = null;
    }
    */

}