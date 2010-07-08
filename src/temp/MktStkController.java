package mbot;

import mbot.Controller;

//
// This is a controller responsible for handling the real-time
// stock market datafeed (MktStk). 
//

public class MktStkController extends Controller
{

    HashMap activeSymbolFeeds = null;;

    String[] datafeeds = { "TickString", 
                           "TickSize",
                           "TickPrice",
                           "TickGeneric" };

    public MktStkController( SimpleCassandraClient cassandraClient,
                             int twsClientIdStart,
                             int twsClientIdEnd )
    {

        this.controllerName = "MktStk";
        this.cassandraKeyspace = "MktStk";

        this.cassandraClient = cassandraClient;

        this.twsClientIdStart = twsClientIdStart;
        this.twsClientIdEnd = twsClientIdStart;
        this.twsClientIdCurrentUnassigned = this.twsClientIdEnd;

    }

    //
    // All market requests should be canceled before this is run.
    // Otherwise they are going to barf when we reset the hashtable.
    // Use 'refresh' on a running server instead.
    //
    public void init()
    {

        //
        // Clear out the hashmap and client ID space so we can 
        // refill with the fresh metadata we are about to get
        //
        activeSymbolFeeds = new HashMap();
        twsClientIdCurrentUnassigned = twsClientIdStart;

        //
        // Load controller metadata
        //
        List<ColumnOrSuperColumn> controlColumns;

        controlColumns = cassandraClient.getSlice( controlKeyspace, controllerName, "Active" );

        for (ColumnOrSuperColumn column : controlColumns)
        {
            //
            // Map a clientId to a symbol returned from the database.
            // The actual clientId->mapping doesn't matter between runs,
            // so the order columns are returned doesn't matter.
            //
            activeSymbolFeeds.add( new Integer(twsClientIdCurrentUnassigned++),
                                   new String(column.value) );

        }

    }

    //
    // This is like a version of init() that will cancel datafeeds, reread
    // metadata from the database, and then restart the datafeeds.
    //
    //
    public void refresh()
    {
        //
        // Cancel datafeeds
        //

        init();

        //
        // Restart the datafeeds
        //

    }

    public void beginFeeds()
    {

    }

    public void cancelFeeds( boolean quiesceFirst )
    {

        if( quiesceFirst == true )
        {
            // wait for all pending requests to finish
        }
        else
        {
            // cancel all jobs
        }
    }

}