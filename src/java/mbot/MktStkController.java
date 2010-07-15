package mbot;

import scc.*;

// IB imports
import com.ib.client.*;
import samples.base.StkContract;

// Thrift imports
//import org.apache.cassandra.thrift.*;

// Java imports
import java.lang.Math;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Collection;

//
// This is a controller responsible for handling the real-time
// stock market datafeed (MktStk). 
//

public class MktStkController extends Controller
{

    //
    // List of symbols we are receive feeds on
    //
    LinkedList<String> activeSymbols;

    //
    // Constructor
    //
    public MktStkController( Scheduler scheduler,
                             CassandraSubsystem cassandraSubsystem,
                             TwsSubsystem twsSubsystem )
    {

        this.scheduler = scheduler;
        this.cassandraSubsystem = cassandraSubsystem;
        this.twsSubsystem = twsSubsystem;

        controllerName = "MktStk";
        cassandraKeyspace = "MktStk";

        pendingJobs = new LinkedList<Job>();

        activeSymbols = new LinkedList<String>();

    }

    //
    // All market requests should be canceled before this is run.
    // Otherwise they are going to barf when we reset the hashtable.
    // Use 'refresh' on a running server instead.
    //
    public synchronized void initFromControl()
        throws CassandraSubsystemException, SchedulerException
    {

        //clientIdToSymbol = new Hashtable<Integer, String>();
        //symbolToClientId = new Hashtable<String, Integer>();

        //
        // Load controller metadata
        //
        HashMap controlColumns;

        controlColumns = cassandraSubsystem.getSlice( controlKeyspace, controllerName, "Active" );

        Collection<byte[]> symbols = controlColumns.values();

        for( byte[] symbol : symbols )
        {
            //String symbol = new String ( columnNames.nextElement() );

            //Integer clientId = new Integer( getNextValidTwsClientId() );

            //
            // Map a clientId to a symbol returned from the database.
            // The actual clientId->mapping doesn't matter between runs,
            // so the order columns are returned doesn't matter.
            //
            //createMapping( clientId, symbol );

            //
            // Begin the feed on the new symbol
            //
            //beginFeed( clientId );

            beginFeed( new String(symbol) );

        }

    }

    //
    // Read from the database as in initFromControl(),
    // but cancel the feeds that are gone and add the 
    // feeds that have been added.
    //
    public synchronized void refreshFromControl()
        throws CassandraSubsystemException, SchedulerException, Exception
    {

        //
        // Save the old mapping
        //
        //Hashtable<String, Integer> oldSymbolToClientId = symbolToClientId;
        LinkedList<String> oldActiveSymbols = activeSymbols;
        activeSymbols = new LinkedList<String>();

        //
        // Create new mappings
        //
        //clientIdToSymbol = new Hashtable<Integer, String>();
        //symbolToClientId = new Hashtable<String, Integer>();

        //
        // Load controller metadata
        //
        HashMap controlColumns = new HashMap();

        controlColumns = cassandraSubsystem.getSlice( controlKeyspace, controllerName, "Active" );

        Collection<String> columnNames = controlColumns.values();

        for( String symbol : columnNames )
        {

            //String symbol = new String ( columnNames.nextElement() );

            //
            // See if this symbol was already being tracked
            //
            //if( oldSymbolToClientId.containsKey( symbol ) == true )
            if( oldActiveSymbols.contains( symbol ) == true )
            {
                //
                // The symbol exists, re-add it to the new mapping
                //
                //Integer clientId = oldSymbolToClientId.get( symbol );

                //clientIdToSymbol.put( clientId, symbol );
                //symbolToClientId.put( symbol, clientId );

                activeSymbols.add( symbol );
                oldActiveSymbols.remove( symbol );

            }
            else
            {
                //
                // The symbol did not exist before, add it.
                //
                //Integer clientId = new Integer( getNextValidTwsClientId() );

                //createMapping( clientId, symbol );

                //
                // Begin the feed on the new symbol
                //
                //beginFeed( clientId );

                beginFeed( symbol );

            }

        }

        //
        // Cancel the old symbols, anything left over
        //
        //Enumeration<Integer> clientIds = oldSymbolToClientId.elements();

        //while( clientIds.hasMoreElements() )
        //{
        //Integer clientId = clientIds.nextElement();

        //cancelFeed( clientId );
        //}

        for( String symbol : oldActiveSymbols )
        {
            cancelFeed( symbol );
        }

    }

    //
    // Add or remove a symbol to the control set of symbols
    //
    public synchronized void addToControl( String symbol )
        throws CassandraSubsystemException
    {
        cassandraSubsystem.insert( controlKeyspace,
                                   controllerName,
                                   "Active",
                                   symbol,
                                   symbol.getBytes() );
    }

    public synchronized void removeFromControl( String symbol )
        throws CassandraSubsystemException
    {
        cassandraSubsystem.delete( controlKeyspace,
                                   controllerName,
                                   "Active",
                                   symbol );
    }

    //
    // Send a request to begin a feed of a specific symbol
    //
    //private synchronized void beginFeed( Integer clientId )
    private synchronized void beginFeed( String symbol )
        throws SchedulerException
    {

        //String symbol = new String( clientIdToSymbol.get( clientId ) );

        System.out.println( "Beginning feed for " + symbol );

        //Contract contract = createMktStkContract( symbol );
        //StkContract contract = new StkContract( symbol );

        //Job job = new MktStkJob( MktStkJob.JOB_START_FEED,
        //twsSubsystem,
        //clientId,
        //contract,
        //false );
    
        Job job = new MktStkJob( MktStkJob.JOB_START_FEED,
                                 twsSubsystem,
                                 this,
                                 symbol );
        //
        // Track the active symbols
        //
        activeSymbols.add( symbol );

        //
        // Add the job to the queue so we can get it later
        //
        pendingJobs.add( job );

        //
        // Schedule the job 
        //
        scheduler.scheduleNow( job );

    }

    //
    // Send a request to end a feed
    //
    private synchronized void cancelFeed( String symbol )
        throws SchedulerException
    {

        //String symbol = new String( clientIdToSymbol.get( clientId ) );

        System.out.println( "Canceling feed for " + symbol );

        Job job = new MktStkJob( MktStkJob.JOB_STOP_FEED,
                                 twsSubsystem,
                                 this,
                                 symbol );

        //
        // Add the job to the queue so we can get it later
        //
        pendingJobs.add( job );

        //
        // Schedule the job 
        //
        scheduler.scheduleNow( job );

    }

    //
    // Called by the scheduler when the job is complete
    //
    public synchronized void completionCallback( Job job )
    {
        pendingJobs.remove( job );

    }


    //
    // Psuedo-EWrapper interface
    //

    //
    // Main datafeed callbacks
    //

    public void callbackOptionVolume( String symbol, String tickType, Integer size )
    {
        System.out.println("OptionVolume: "+symbol+" "+tickType+" "+size);
    }

    public void callbackOptionOpenInterest( String symbol, String tickType, Integer size )
    {
        System.out.println("OptionOpenInterest: "+symbol+" "+tickType+" "+size);
    }

    public void callbackOptionHistoricalVolatility( String symbol, String tickType, Double value )
    {
        System.out.println("OptionHistoricalVolatility: "+symbol+" "+tickType+" "+value);
    }

    public void callbackOptionImpliedVolatility( String symbol, String tickType, Double value )
    {
        System.out.println("OptionImpliedVolatility: "+symbol+" "+tickType+" "+value);
    }

    //
    // Index Future Premium -- not implemented
    //

    public void callbackMiscellaneous( String symbol, String tickType, Double price, boolean canAutoExecute )
    {
        System.out.println("Miscellaneous: "+symbol+" "+tickType+" "+price+" "+canAutoExecute);
    }

    public void callbackMiscellaneous( String symbol, String tickType, Integer size )
    {
        System.out.println("Miscellaneous: "+symbol+" "+tickType+" "+size);
    }

    //
    // Mark Price -- not implemented
    //

    //
    // Auction -- not implemented
    //

    public void callbackRTVolume( String symbol, Double lastPrice, Integer lastSize,
                                  Long lastTimeMS, Integer totalVolume, Double vwap, boolean isSingleTrade )
    {
        System.out.println("RTVolume: "+symbol+" "+symbol+" "+lastPrice+" "+lastSize+" "+
                           lastTimeMS+" "+totalVolume+" "+vwap+" "+isSingleTrade );
    }

    public void callbackShortable( String symbol, String tickType, Double value )
    {
        System.out.println("Shortable: "+symbol+" "+tickType+" "+value);
    }

    //
    // Inventory -- not implemented
    //

    //
    // Fudamentals -- not implemented
    //

    //
    // This seems to return just trailing price data, e.g. 13/26/52 week high/low
    //
    // TickTypes can be:
    //  52WeekLow
    //  52WeekHigh
    //  26WeekLow
    //  26WeekHigh
    //  13WeekLow
    //  13WeekHigh
    //
    public void tickPriceCallback( String symbol,
                                   String tickType,
                                   Double price,
                                   boolean canAutoExecute )
    {
        //
        // Get the symbol based on this feeds clientId
        //
        System.out.println("TickPrice: "+symbol+" "+tickType+" "+price+" "+canAutoExecute);

        try
        {
            /*
            cassandraSubsystem.insert( cassandraKeyspace,
                                       "TickPrice",
                                       symbol,
                                       date,
                                       timeStamp,
                                       msg.getBytes() );
            */
        }
        catch ( Exception e )
        {
            // As a callback, the caller isn't prepared to error handle
        }

    }

    //
    // This returns options volume data, apparently after each trade
    //
    // TickTypes can be:
    //  OptionPutVolume
    //  OptionCallVolume
    //  AvgVolume -- sum of put/calls, unclear how long this is averaged over
    //  OptionPutOpenInterest
    //  OptionCallOpenInterest
    //
    public void tickSizeCallback( String symbol, String tickType, Integer size )
    {
        //
        // Get the symbol based on this feeds clientId
        //
        System.out.println("TickSize: "+symbol+" "+tickType+" "+size);

        try
        {
            /*
            cassandraSubsystem.insert( cassandraKeyspace,
                                       "TickSize",
                                       symbol,
                                       date,
                                       timeStamp,
                                       msg.getBytes() );
            */
        }
        catch ( Exception e )
        {

        }

    }

    //public void tickGeneric( int tickerId, int tickType, double value )
    public void tickGenericCallback( String symbol, String tickType, Double value )
    {
        //
        // Get the symbol based on this feeds clientId
        //
        System.out.println("TickGeneric: "+symbol+" "+tickType+" "+value);

        try
        {
            /*
            cassandraSubsystem.insert( cassandraKeyspace,
                                       "TickGeneric",
                                       symbol,
                                       date,
                                       timeStamp,
                                       msg.getBytes() );
            */
        }
        catch ( Exception e )
        {

        }

    }

    //public void tickString( int tickerId, int tickType, String value )
    public void tickStringCallback( String symbol, String tickType, String value )
    {
        //
        // Get the symbol based on this feeds clientId
        //
        System.out.println("TickString: "+symbol+" "+tickType+" "+value);

        try
        {
            /*
            cassandraSubsystem.insert( cassandraKeyspace,
                                       "TickString",
                                       symbol,
                                       date,
                                       timeStamp,
                                       msg.getBytes() );
            */
        }
        catch ( Exception e )
        {

        }

    }

}