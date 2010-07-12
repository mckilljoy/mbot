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
// This is a controller for the entire system. It doesn't 
// queue up and jobs itself, but contains subcontrollers
// that take care of different subtasks.
//
// It also contains the single TWS client and scheduler
// instances
//
public class SystemController extends Controller
{

    //
    // Which client Id do we start with
    //
    static final int TWS_CLIENT_ID_DEFAULT = 0;

    //int twsNextValidClientId = TWS_CLIENT_ID_DEFAULT;
    Integer twsNextValidClientId = new Integer(TWS_CLIENT_ID_DEFAULT);

    //
    // This maps the datafeeds to clientIds
    //
    Hashtable<Integer, Controller> clientIdToSubcontroller;

    //
    // Subcontrollers
    //
    LinkedList<SubController> subcontrollers;

    public SystemController()
        throws SystemControllerException
    {

        try
        {
            controllerName = "System";
            cassandraKeyspace = "Control";

            //
            // Spin up the scheduler.
            //
            scheduler = new Scheduler();
            scheduler.startEvaluatorThread();

            //
            // Connect to the cassandra server
            //
            cassandraClient = new SimpleCassandraClient();
            cassandraClient.connect( "localhost", 9160 );

            if( cassandraClient.isConnected() )
            {
                System.out.println( controllerName + ": Connected to Cassandra Server '" +
                                    cassandraClient.clusterName + "' on " +
                                    cassandraClient.server + "/" + 
                                    cassandraClient.port);
            }
            else
            {
                throw new Exception("Failed to connect to Cassandra Server");
            }

            //
            // Connect to TWS
            //
            int clientId = getNextValidTwsClientId();

            twsClient = new EClientSocket( this );
            twsClient.eConnect( "localhost", 7496, clientId );

            if( twsClient.isConnected() )
            {
                System.out.println( controllerName + ": Connected to TWS server version " +
                                    twsClient.serverVersion() + " at " +
                                    twsClient.TwsConnectionTime());
                //
                // Wait for the nextValidId() to come in. Not critical.
                //
                try
                {
                    synchronized( twsNextValidClientId )
                    {
                        twsNextValidClientId.wait(10000);
                    }
                }
                catch ( InterruptedException e )
                {
                    // nothing
                }

                System.out.println(controllerName + ": Using client id " + twsNextValidClientId );

            }
            else
            {
                throw new Exception("Failed to connect to TWS Server");
            }

            //
            // Initialize sub controllers
            //
            MktStkSubController mssc =
                new MktStkSubController( this,
                                         scheduler,
                                         cassandraClient,
                                         twsClient );

            HistoricalStkSubController hssc =
                new HistoricalStkSubController( this,
                                                scheduler,
                                                cassandraClient,
                                                twsClient );
        
            subcontrollers = new LinkedList<SubController>();

            subcontrollers.add( mssc );
            subcontrollers.add( hssc );

            //
            // Misc
            //
            clientIdToSubcontroller = new Hashtable<Integer, Controller>();

        }
        catch( Exception e )
        {
            if( scheduler != null )
            {
                scheduler.endEvaluatorThread();
            }

            if( cassandraClient != null )
            {
                cassandraClient.disconnect();
            }

            if( twsClient != null )
            {
                twsClient.eDisconnect();
            }

            throw new SystemControllerException( e.getMessage() );

        }
    }

    //
    // Control who gets assigned with TWS clientId
    //
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

    //
    // EWrapper interface
    //

    //
    // These first ones are handled directly by the system controller
    //
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

}