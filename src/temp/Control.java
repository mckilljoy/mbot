package mbot;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.EClientSocket;
import com.ib.client.EWrapper;
import com.ib.client.EWrapperMsgGenerator;
import com.ib.client.Execution;
import com.ib.client.Order;
import com.ib.client.OrderState;
import com.ib.client.UnderComp;
import com.ib.client.Util;

//
// This connects a real-time financial datafeed to a high-throughput database.
//
// For input, it uses the IB API to pull data from a TWS instance.
// For output, it uses the Thrift interface to send data a Cassandra instance.
//
public abstract class Control implements EWrapper
{

    //
    // The EClientSocket implements an IB API to request data.
    //
    EClientSocket client = new EClientSocket(this);

    //
    // TWS related variables
    //
    String twsServername = "localhost";
    int twsPort = 7496;
    int twsClientId = 0;

    //
    // Cassandra related variables
    //
    String cassandraServername = "localhost";
    int cassandraPort = 9160;

    //
    // Constructors
    //

    //
    // Just use the default server/port values
    //
    public Control()
    {
        // Do nothing
    }

    //
    // Let caller specify non-default server/port values
    //
    public Control( String twsServername, int twsPort, int twsClientId,
                    String cassandraServername, int cassandraPort )
    {
        this.twsServername = twsServername;
        this.twsPort = twsPort;
        this.twsClientId = twsClientId;

        this.cassandraServername = cassandraServername;
        this.cassandraPort = cassandraPort;

    }

    //
    // Connect to the TWS instance using the EClientSocket
    //
    public boolean twsConnect()
    {
        // Connect to TWS
        client.eConnect( twsServername, twsPort, twsClientId );

        if(client.isConnected())
        {

            System.out.println("Connected to Tws server version " +
                               client.serverVersion() + " at " +
                               client.TwsConnectionTime());

            return true;

        }
        else
        {

            System.out.println("Failed to connect to server");

            return false;

        }
    }

    //
    // Disconnect from the TWS instance using the EClientSocket
    //
    public boolean twsDisconnect()
    {

        client.eDisconnect();

        if(!client.isConnected())
        {

            System.out.println("Client is disconnected");

            return true;

        }
        else
        {

            System.out.println("Client failed to disconnect");

            return false;

        }

    }

    //
    // Connect to the Cassandra interface using SCC
    //
    public boolean cassandraConnect()
    {



    //
    // These methods are defined in the EWrapper interface.
    //
    // They act as event handlers for datafeeds subscribed to
    // earlier using the API implemented in EClientSocket.
    //

    ///////////////////////////////////////////////////////////////////////
    // Interface methods
    ///////////////////////////////////////////////////////////////////////
    void tickPrice( int tickerId, int field, double price, int canAutoExecute);
    void tickSize( int tickerId, int field, int size);
    void tickOptionComputation( int tickerId, int field, double impliedVol,
                                double delta, double modelPrice, double pvDividend);
    void tickGeneric(int tickerId, int tickType, double value);
    void tickString(int tickerId, int tickType, String value);
    void tickEFP(int tickerId, int tickType, double basisPoints,
                 String formattedBasisPoints, double impliedFuture, int holdDays,
                 String futureExpiry, double dividendImpact, double dividendsToExpiry);
    void orderStatus( int orderId, String status, int filled, int remaining,
                      double avgFillPrice, int permId, int parentId, double lastFillPrice,
                      int clientId, String whyHeld);
    void openOrder( int orderId, Contract contract, Order order, OrderState orderState);
    void openOrderEnd();
    void updateAccountValue(String key, String value, String currency, String accountName);
    void updatePortfolio(Contract contract, int position, double marketPrice, double marketValue,
                         double averageCost, double unrealizedPNL, double realizedPNL, String accountName);
    void updateAccountTime(String timeStamp);
    void accountDownloadEnd(String accountName);
    void nextValidId( int orderId);
    void contractDetails(int reqId, ContractDetails contractDetails);
    void bondContractDetails(int reqId, ContractDetails contractDetails);
    void contractDetailsEnd(int reqId);
    void execDetails( int reqId, Contract contract, Execution execution);
    void execDetailsEnd( int reqId);
    void updateMktDepth( int tickerId, int position, int operation, int side, double price, int size);
    void updateMktDepthL2( int tickerId, int position, String marketMaker, int operation,
                           int side, double price, int size);
    void updateNewsBulletin( int msgId, int msgType, String message, String origExchange);
    void managedAccounts( String accountsList);
    void receiveFA(int faDataType, String xml);
    void historicalData(int reqId, String date, double open, double high, double low,
                        double close, int volume, int count, double WAP, boolean hasGaps);
    void scannerParameters(String xml);
    void scannerData(int reqId, int rank, ContractDetails contractDetails, String distance,
                     String benchmark, String projection, String legsStr);
    void scannerDataEnd(int reqId);
    void realtimeBar(int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count);
    void currentTime(long time);
    void fundamentalData(int reqId, String data);
    void deltaNeutralValidation(int reqId, UnderComp underComp);
    void tickSnapshotEnd(int reqId);

}