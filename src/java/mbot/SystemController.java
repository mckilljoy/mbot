package mbot;

import scc.*;

// IB imports
import com.ib.client.*;

// Thrift imports
import org.apache.cassandra.thrift.*;

// Java imports
import java.lang.Thread;
import java.lang.Math;
import java.util.Enumeration;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Collections;

//
// This controls the entire system. It doesn't 
// queue up jobs itself, but contains controllers
// that take care of different subtasks.
//
// It also contains the single TWS client and scheduler
// instances
//
public class SystemController
{

    //
    // Jobs may be queued to this scheduler object
    //
    protected Scheduler scheduler;


    //
    // Tws subsystem
    TwsSubsystem twsSubsystem;

    //
    // Which client Id do we start with
    //
    //static final int TWS_CLIENT_ID_DEFAULT = 0;
    //Integer twsNextValidClientId = new Integer(TWS_CLIENT_ID_DEFAULT);

    //
    // This maps the clientIds to datafeeds 
    //
    //Hashtable<Integer, Controller> clientIdToController;


    //
    // Cassandra subsystem
    //
    CassandraSubsystem cassandraSubsystem;


    //
    // Controllers
    //
    LinkedList<Controller> controllers;

    public SystemController()
        throws SystemControllerException
    {

        try
        {

            //
            // Spin up the scheduler.
            //
            scheduler = new Scheduler();
            scheduler.startEvaluatorThread();

            //
            // Create the Cassandra subsystem
            //
            cassandraSubsystem = new CassandraSubsystem( "localhost", 9160 );
            System.out.println( "Connected to " + cassandraSubsystem.getServerInfo() );

            //
            // Create the Tws subsystem
            //
            twsSubsystem = new TwsSubsystem( "localhost", 7496 );

            System.out.println( "Connected to " + twsSubsystem.getServerInfo() );

            //
            // Initialize sub controllers
            //
            /*
            MktStkController msc =
                new MktStkController( scheduler,
                                      cassandraSubsystem,
                                      twsSubsystem );

            msc.initFromControl();

            System.out.println( "MktStkController initialized" );
            */
            HistoricalStkController hsc =
                new HistoricalStkController( scheduler,
                                             cassandraSubsystem,
                                             twsSubsystem );
        
            System.out.println( "HistoricalController initialized" );

            //Long now = Controller.getTimestampMS() - (6*60*60*1000);
            Long now = Controller.getTimestampMS(); ;            

            System.out.println( "Adding to control: "+now+"-"+HistoricalStkController.TIME_1_YEAR_IN_MS);
            hsc.addToControl( "ATVI", now-HistoricalStkController.TIME_1_YEAR_IN_MS );
            System.out.println( "Pulling work" );
            hsc.pullWorkFromControl();
            System.out.println( "Done" );

            //
            // Hold onto these controllers
            //
            controllers = new LinkedList<Controller>();

            //controllers.add( msc );
            controllers.add( hsc );

        }
        catch( Exception e )
        {

            //
            // Let user know what failed
            //
            if( e instanceof SchedulerException )
            {
                System.out.println("Failed to create scheduler");
            }
            else if( e instanceof CassandraSubsystemException )
            {
                System.out.println("Failed to create Cassandra subsystem");
            }
            else if( e instanceof TwsSubsystemException )
            {
                System.out.println("Failed to create TWS subsystem");
            }
            else
            {
                System.out.println("Failed to create system");
            }

            //
            // Clean up our mess
            //
            if( scheduler != null )
            {
                scheduler.shutdown();
            }

            if( twsSubsystem != null )
            {
                twsSubsystem.shutdown();
            }

            if( cassandraSubsystem != null )
            {
                cassandraSubsystem.shutdown();
            }

            throw new SystemControllerException(e);
        }

    }

    //
    // Shutdown the system
    //
    public void shutdown()
    {

        scheduler.shutdown();

        cassandraSubsystem.shutdown();

        twsSubsystem.shutdown();

    }

    //
    // Control who gets assigned with TWS clientId
    //
    /*
    public synchronized int getNextValidTwsClientId()
    {
        return getNextValidTwsClientId( null );
    }

    public synchronized int getNextValidTwsClientId( SubController controller )
    {

        int clientId = twsNextValidClientId++;

        //
        // Track who is using this clientId
        //
        if( controller != null )
        {
            clientIdToSubcontroller.put( clientId, controller );
        }

        return clientId;

    }
    */

    //
    // EWrapper interface
    //

    //
    // These first ones are handled directly by the system controller
    //
    /*
    public void error( Exception ex )
    {
        String msg = EWrapperMsgGenerator.error( ex );
        System.out.println( msg );
    }

    public void error( String errorMsg )
    {
        String msg = EWrapperMsgGenerator.error( errorMsg );
        System.out.println( msg );
    }

    public void error( int id, int errorCode, String errorMsg )
    {
        String msg = EWrapperMsgGenerator.error( id, errorCode, errorMsg );
        System.out.println( msg );
    }

    public synchronized void nextValidId( int orderId )
    {
        //
        // Always increase the clientId. This is arguably a bit wasteful
        // with the Id space, but shouldn't generally be a problem.
        //
        twsNextValidClientId = Math.max( orderId, twsNextValidClientId );
        System.out.println("TWS: Next valid client id " + twsNextValidClientId );

        //
        // Notfiy a thread (i.e. the constructor) waiting for a valid Id.
        // This is only an issue before subcontrollers are allocated Id.
        //
        if( clientIdToSubcontroller == null )
        {
            synchronized( twsNextValidClientId )
            {
                twsNextValidClientId.notify();
            }
        }
    }

    public synchronized void connectionClosed()
    {
        //String msg = EWrapperMsgGenerator.connectionClosed();
        System.out.println("TWS: Disconnected from TWS server");

        //
        // Null out the twsClient so no one uses it
        //
        twsClient = null;
    }

    //
    // These are passed along to the subcontrollers
    //

    //
    // Main datafeed callbacks
    //
    public void tickPrice( int tickerId, int field, double price, int canAutoExecute )
    {

        Controller controller = clientIdToSubcontroller.get( new Integer( tickerId ) );

        controller.tickPrice( tickerId, field, price, canAutoExecute );

    }

    public void tickSize( int tickerId, int field, int size )
    {

        Controller controller = clientIdToSubcontroller.get( new Integer( tickerId ) );

        controller.tickSize( tickerId, field, size );

    }

    public void tickGeneric( int tickerId, int tickType, double value )
    {

        Controller controller = clientIdToSubcontroller.get( new Integer( tickerId ) );

        controller.tickGeneric( tickerId, tickType, value );

    }

    public void tickString( int tickerId, int tickType, String value )
    {

        Controller controller = clientIdToSubcontroller.get( new Integer( tickerId ) );

        controller.tickString( tickerId, tickType, value );

    }
    */
}