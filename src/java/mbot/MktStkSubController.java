package mbot;

import scc.*;

// IB imports
import com.ib.client.*;
import samples.base.StkContract;

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
// This is a controller responsible for handling the real-time
// stock market datafeed (MktStk). 
//

public class MktStkSubController extends SubController
{

    //
    // Constructor
    //
    public MktStkSubController( SystemController parent,
                                Scheduler scheduler,
                                SimpleCassandraClient cassandraClient,
                                EClientSocket twsClient )
    {

        this.parent = parent;
        this.scheduler = scheduler;
        this.cassandraClient = cassandraClient;
        this.twsClient = twsClient;

        controllerName = "MktStk";
        cassandraKeyspace = "MktStk";

        pendingJobs = new LinkedList<Job>();

    }

    //
    // All market requests should be canceled before this is run.
    // Otherwise they are going to barf when we reset the hashtable.
    // Use 'refresh' on a running server instead.
    //
    public synchronized void initFromControl()
        throws CassandraException
    {

        clientIdToSymbol = new Hashtable<Integer, String>();
        symbolToClientId = new Hashtable<String, Integer>();

        //
        // Load controller metadata
        //
        List<ColumnOrSuperColumn> controlColumns;

        controlColumns = cassandraClient.getSlice( controlKeyspace, controllerName, "Active" );

        for( ColumnOrSuperColumn column : controlColumns )
        {

            String symbol = new String( column.column.value );
            Integer clientId = new Integer( getNextValidTwsClientId() );

            //
            // Map a clientId to a symbol returned from the database.
            // The actual clientId->mapping doesn't matter between runs,
            // so the order columns are returned doesn't matter.
            //
            createMapping( clientId, symbol );

            //
            // Begin the feed on the new symbol
            //
            beginFeed( clientId );

        }

    }

    //
    // Read from the database as in initFromControl(),
    // but cancel the feeds that are gone and add the 
    // feeds that have been added.
    //
    public synchronized void refreshFromControl()
        throws CassandraException
    {

        //
        // Save the old mapping
        //
        Hashtable<String, Integer> oldSymbolToClientId = symbolToClientId;

        //
        // Create new mappings
        //
        clientIdToSymbol = new Hashtable<Integer, String>();
        symbolToClientId = new Hashtable<String, Integer>();

        //
        // Load controller metadata
        //
        List<ColumnOrSuperColumn> controlColumns;

        controlColumns = cassandraClient.getSlice( controlKeyspace, controllerName, "Active" );

        for( ColumnOrSuperColumn column : controlColumns )
        {

            String symbol = new String( column.column.value );

            //
            // See if this symbol was already being tracked
            //
            if( oldSymbolToClientId.containsKey( symbol ) == true )
            {
                //
                // The symbol exists, re-add it to the new mapping
                //
                Integer clientId = oldSymbolToClientId.get( symbol );

                clientIdToSymbol.put( clientId, symbol );
                symbolToClientId.put( symbol, clientId );

            }
            else
            {
                //
                // The symbol did not exist before, add it.
                //
                Integer clientId = new Integer( getNextValidTwsClientId() );

                createMapping( clientId, symbol );

                //
                // Begin the feed on the new symbol
                //
                beginFeed( clientId );

            }

        }

        //
        // Cancel the old symbols, anything left over
        //
        Enumeration<Integer> clientIds = oldSymbolToClientId.elements();

        while( clientIds.hasMoreElements() )
        {
            Integer clientId = clientIds.nextElement();

            cancelFeed( clientId );
        }

    }

    //
    // Add or remove a symbol to the control set of symbols
    //
    public synchronized void addToControl( String symbol )
        throws CassandraException
    {
        cassandraClient.insert( controlKeyspace, controllerName, "Active", symbol, symbol.getBytes() );
    }

    public synchronized void removeFromControl( String symbol )
        throws CassandraException
    {
        cassandraClient.delete( controlKeyspace, controllerName, "Active", symbol );
    }

    //
    // Send a request to begin a feed of a specific symbol
    //
    private synchronized void beginFeed( Integer clientId )
    {

        String symbol = new String( clientIdToSymbol.get( clientId ) );

        System.out.println("Tracking symbol " + symbol + " on clientId " + clientId);

        //Contract contract = createMktStkContract( symbol );
        StkContract contract = new StkContract( symbol );

        Job job = new MktStkJob( MktStkJob.JOB_START_FEED,
                                 twsClient,
                                 clientId,
                                 contract,
                                 false );

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
    private synchronized void cancelFeed( Integer clientId )
    {

        //String symbol = new String( clientIdToSymbol.get( clientId ) );

        System.out.println("Canceling feed for clientId " + clientId);

        Job job = new MktStkJob( MktStkJob.JOB_STOP_FEED,
                                 twsClient,
                                 clientId );

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

    /*
    //
    // Create contracts
    //
    private Contract createMktStkContract( String symbol )
    {

        Contract contract = new Contract();

        try {
            // set contract fields
            contract.m_conId = 0;
            contract.m_symbol = symbol;
            contract.m_secType = new String("STK");
            contract.m_expiry = new String();
            contract.m_strike = 0.0;
            contract.m_right = new String();
            contract.m_multiplier = new String();
            contract.m_exchange = new String("SMART");
            contract.m_primaryExch = new String("ISLAND");
            contract.m_currency = new String();
            contract.m_localSymbol = new String();
            contract.m_includeExpired = false;

            contract.m_secIdType = new String();
            contract.m_secId = new String();

            
            // set order fields
            m_order.m_action = m_action.getText();
            m_order.m_totalQuantity = Integer.parseInt( m_totalQuantity.getText() );
            m_order.m_orderType = m_orderType.getText();
            m_order.m_lmtPrice = Double.parseDouble( m_lmtPrice.getText() );
            m_order.m_auxPrice = Double.parseDouble( m_auxPrice.getText() );
            m_order.m_goodAfterTime = m_goodAfterTime.getText();
            m_order.m_goodTillDate = m_goodTillDate.getText();

            m_order.m_faGroup = m_faGroup;
            m_order.m_faProfile = m_faProfile;
            m_order.m_faMethod = m_faMethod;
            m_order.m_faPercentage = m_faPercentage;

            // set historical data fields
            m_backfillEndTime = m_BackfillEndTime.getText();
            m_backfillDuration = m_BackfillDuration.getText();
            m_barSizeSetting = m_BarSizeSetting.getText();
            m_useRTH = Integer.parseInt( m_UseRTH.getText() );
            m_whatToShow = m_WhatToShow.getText();
            m_formatDate = Integer.parseInt( m_FormatDate.getText() );
            m_exerciseAction = Integer.parseInt( m_exerciseActionTextField.getText() );
            m_exerciseQuantity = Integer.parseInt( m_exerciseQuantityTextField.getText() );
            m_override = Integer.parseInt( m_overrideTextField.getText() );;

            // set market depth rows
            m_marketDepthRows = Integer.parseInt( m_marketDepthRowTextField.getText() );
            m_genericTicks = m_genericTicksTextField.getText();
            m_snapshotMktData = m_snapshotMktDataTextField.isSelected();
            
        }
        catch( Exception e )
        {
            return null;
        }

        return contract;

    }
    */

    //
    // EWrapper interface
    //

    //
    // Main datafeed callbacks
    //
    public void tickPrice( int tickerId, int field, double price, int canAutoExecute )
    {
        //
        // Get the symbol based on this feeds clientId
        //
        String symbol = clientIdToSymbol.get( tickerId );

        String timeStamp = getTimestampStringMS();

        String date = getDate();

    	String msg = EWrapperMsgGenerator.tickPrice( tickerId, field, price, canAutoExecute );

        try
        {
            cassandraClient.insert( cassandraKeyspace,
                                    "TickPrice",
                                    symbol,
                                    date,
                                    timeStamp,
                                    msg.getBytes() );
        }
        catch ( CassandraException e )
        {

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String function = stack[0].getMethodName();

            if( function != null )
            {
                System.out.println( "Exception caught in " + function  );
            }
            else
            {
                System.out.println( e.getMessage() );
            }

        }

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

        try
        {
            cassandraClient.insert( cassandraKeyspace,
                                    "TickSize",
                                    symbol,
                                    date,
                                    timeStamp,
                                    msg.getBytes() );
        }
        catch ( CassandraException e )
        {

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String function = stack[0].getMethodName();

            if( function != null )
            {
                System.out.println( "Exception caught in " + function  );
            }
            else
            {
                System.out.println( e.getMessage() );
            }

        }

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

        try
        {
            cassandraClient.insert( cassandraKeyspace,
                                    "TickGeneric",
                                    symbol,
                                    date,
                                    timeStamp,
                                    msg.getBytes() );
        }
        catch ( CassandraException e )
        {

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String function = stack[0].getMethodName();

            if( function != null )
            {
                System.out.println( "Exception caught in " + function  );
            }
            else
            {
                System.out.println( e.getMessage() );
            }

        }

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

        try
        {
            cassandraClient.insert( cassandraKeyspace,
                                    "TickString",
                                    symbol,
                                    date,
                                    timeStamp,
                                    msg.getBytes() );
        }
        catch ( CassandraException e )
        {

            StackTraceElement[] stack = Thread.currentThread().getStackTrace();
            String function = stack[0].getMethodName();

            if( function != null )
            {
                System.out.println( e.getClass().getName() + " caught in " + function  );
            }
            else
            {
                System.out.println( e.getMessage() );
            }

        }

    }

}