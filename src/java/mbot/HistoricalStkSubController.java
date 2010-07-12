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
// This is a controller responsible for handling the real-time
// stock market datafeed (MktStk). 
//

public class HistoricalStkSubController extends SubController
{

    //
    // Constructor
    //
    public HistoricalStkSubController( SystemController parent,
                                       Scheduler scheduler,
                                       SimpleCassandraClient cassandraClient,
                                       EClientSocket twsClient )
    {

        this.parent = parent;
        this.scheduler = scheduler;
        this.cassandraClient = cassandraClient;
        this.twsClient = twsClient;

        controllerName = "HistoricalStk";
        cassandraKeyspace = "HistoricalStk";

        pendingJobs = new LinkedList<Job>();

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
    // Create contracts
    //
    private Contract createHistoricalStkContract( String symbol )
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

            /*
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
            */
        }
        catch( Exception e )
        {
            return null;
        }

        return contract;

    }

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

        /*
        cassandraClient.insert( cassandraKeyspace,
                                "TickPrice",
                                symbol,
                                date,
                                timeStamp,
                                msg.getBytes() );
        */
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
        /*
        cassandraClient.insert( cassandraKeyspace,
                                "TickSize",
                                symbol,
                                date,
                                timeStamp,
                                msg.getBytes() );
        */
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
        /*
        cassandraClient.insert( cassandraKeyspace,
                                "TickGeneric",
                                symbol,
                                date,
                                timeStamp,
                                msg.getBytes() );
        */
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
        /*
        cassandraClient.insert( cassandraKeyspace,
                                "TickString",
                                symbol,
                                date,
                                timeStamp,
                                msg.getBytes() );
        */
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

}