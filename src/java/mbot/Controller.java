package mbot;

// SCC imports
//import scc.*;

// IB imports
import com.ib.client.*;

// Java imports
import java.util.Date;
import java.util.LinkedList;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


//
// Classes that implement this interface can handle scheduling
// various tasks to accomplish their goals. The actual scheduling
// of jobs is most likely handled by a single (external) scheduler.
//
public abstract class Controller
{

    //
    // The parent system
    //
    //protected SystemController parent;

    //
    // Jobs may be queued to this scheduler object
    //
    protected Scheduler scheduler;

    //
    // Track all jobs we have scheduled
    //
    protected LinkedList<Job> pendingJobs;


    //
    // This is cassandra subsystem
    //
    protected CassandraSubsystem cassandraSubsystem;


    //
    // The controllers name can be used for locating metadata
    //
    protected String controllerName;

    //
    // This is the keyspace for this controller's data
    //
    protected String cassandraKeyspace;

    //
    // All controller metadata is in this keyspace
    //
    protected String controlKeyspace = "Control";


    //
    // The IB API requires a unique client id for each datafeed
    // These must be tracked by the controller to avoid collision
    // A simple range should be good enough for now.
    //
    protected TwsSubsystem twsSubsystem;


    //
    // This is called by the scheduler when a job is complete
    //
    protected abstract void completionCallback( Job job );
 
    //
    // Convenience functions
    //

    public String getName()
    {
        return controllerName;
    }

    //
    // Pretty print a date in Long MS form
    //
    protected static String convertTimeLongToDate( Long time )
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        Date date = new Date( time );
        return dateFormat.format(date);
    }

    //
    // Current time in microseconds -- does not have US resolution
    //
    protected static long getTimestampUS()
    {
        return System.currentTimeMillis() * 1000;
    }

    protected static String getTimestampStringUS()
    {
        return new Long( System.currentTimeMillis() * 1000 ).toString();
    }

    //
    // Current time in milliseconds
    //
    protected static long getTimestampMS()
    {
        return System.currentTimeMillis();
    }

    protected static String getTimestampStringMS()
    {
        return new Long( System.currentTimeMillis() ).toString();
    }

    //
    // Current time in seconds, calculated
    //
    protected static long getTimestampS()
    {
        return System.currentTimeMillis()/1000;
    }

    protected static String getTimestampStringS()
    {
        return new Long( System.currentTimeMillis() / 1000 ).toString();
    }

    //
    // Create a string of the current date in YYYMMDD format
    //
    protected static String getDate()
    {
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        Date date = new Date();
        return dateFormat.format(date);
    }


    //
    // Thunk layer for convenience
    //
    protected void thunk()
    {
        notImplemented();
    }

    //
    // A no-op for callbacks we don't need
    //
    protected void notImplemented()
    {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String caller = stack[3].getMethodName();

        if( caller != null )
        {
            System.out.println(controllerName + ": EWrapper callback '" + caller + "' not implemented");
        }
        else
        {
            System.out.println(controllerName + ": Unknown EWrapper callback not implemented");
        }
    }

    //
    // Callbacks for the TWS subsystem to use
    //
    //
    // Main datafeed callbacks
    //

    public void callbackOptionVolume( String symbol, String tickType, Integer size ) { thunk(); }

    public void callbackOptionOpenInterest( String symbol, String tickType, Integer size ) { thunk(); }

    public void callbackOptionHistoricalVolatility( String symbol, String tickType, Double value ) { thunk(); }

    public void callbackOptionImpliedVolatility( String symbol, String tickType, Double value ) { thunk(); }

    //
    // Index Future Premium -- not implemented
    //

    public void callbackMiscellaneous( String symbol, String tickType, Double price, boolean canAutoExecute ) { thunk(); }
    public void callbackMiscellaneous( String symbol, String tickType, Integer size ) { thunk(); }

    //
    // Mark Price -- not implemented
    //

    //
    // Auction -- not implemented
    //

    public void callbackRTVolume( String symbol, Double lastPrice, Integer lastSize,
                                  Long lastTimeMS, Integer totalVolume, Double vwap, boolean isSingleTrade ) { thunk(); }

    public void callbackShortable( String symbol, String tickType, Double value ) { thunk(); }

    //
    // AnyWrapper interface definitions
    //

    public void connectionClosed() { thunk(); }
    public void error( Exception ex ) { thunk(); }
    public void error( String errorMsg ) { thunk(); }
    public void error( int id, int errorCode, String errorMsg ) { thunk(); }

    //
    // EWrapper interface definitions
    //
    //public void tickPrice( int tickerId, int field, double price, int canAutoExecute ) { thunk(); }
    public void tickPriceCallback( String symbol, String field, Double price, boolean canAutoExecute ) { thunk(); }

    //public void tickSize( int tickerId, int field, int size ) { thunk(); }
    public void tickSizeCallback( String symbol, String tickType, Integer size ) { thunk(); }

    public void tickOptionComputation( int tickerId, int field, double impliedVol,
                                       double delta, double modelPrice, double pvDividend ) { thunk(); }

    //public void tickGeneric( int tickerId, int tickType, double value ) { thunk(); }
    public void tickGenericCallback( String symbol, String tickType, Double value ) { thunk(); }
    

    //public void tickString( int tickerId, int tickType, String value ) { thunk(); }
    public void tickStringCallback( String symbol, String tickType, String value ) { thunk(); }

    public void tickEFP( int tickerId, int tickType, double basisPoints,
                         String formattedBasisPoints, double impliedFuture, int holdDays,
                         String futureExpiry, double dividendImpact, double dividendsToExpiry ) { thunk(); }
    public void orderStatus( int orderId, String status, int filled, int remaining,
                             double avgFillPrice, int permId, int parentId, double lastFillPrice,
                             int clientId, String whyHeld ) { thunk(); }
    public void openOrder( int orderId, Contract contract, Order order, OrderState orderState ) { thunk(); }
    public void openOrderEnd() { thunk(); }
    public void updateAccountValue( String key, String value, String currency, String accountName ) { thunk(); }
    public void updatePortfolio( Contract contract, int position, double marketPrice, double marketValue,
                                 double averageCost, double unrealizedPNL, double realizedPNL, String accountName ) { thunk(); }
    public void updateAccountTime( String timeStamp ) { thunk(); }
    public void accountDownloadEnd( String accountName ) { thunk(); }
    public void nextValidId( int orderId ) { thunk(); }
    public void contractDetails( int reqId, ContractDetails contractDetails ) { thunk(); }
    public void bondContractDetails( int reqId, ContractDetails contractDetails ) { thunk(); }
    public void contractDetailsEnd( int reqId ) { thunk(); }
    public void execDetails( int reqId, Contract contract, Execution execution ) { thunk(); }
    public void execDetailsEnd( int reqId ) { thunk(); }
    public void updateMktDepth( int tickerId, int position, int operation, int side, double price, int size ) { thunk(); }
    public void updateMktDepthL2( int tickerId, int position, String marketMaker, int operation,
                                  int side, double price, int size ) { thunk(); }
    public void updateNewsBulletin( int msgId, int msgType, String message, String origExchange ) { thunk(); }
    public void managedAccounts( String accountsList ) { thunk(); }
    public void receiveFA( int faDataType, String xml ) { thunk(); }

    //public void historicalData( int reqId, String date, double open, double high, double low,
    //                            double close, int volume, int count, double WAP, boolean hasGaps ) { thunk(); }
    public void callbackHistoricalData( String symbol, Long startDate, Double open, Double high, Double low,
                                        Double close, Integer volume, Integer count, Double WAP, boolean hasGaps ) { thunk(); }
    public void callbackHistoricalDataFinished( String symbol ) { thunk(); }

    public void scannerParameters( String xml ) { thunk(); }
    public void scannerData( int reqId, int rank, ContractDetails contractDetails, String distance,
                             String benchmark, String projection, String legsStr ) { thunk(); }
    public void scannerDataEnd( int reqId ) { thunk(); }
    public void realtimeBar( int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count ) { thunk(); }
    public void currentTime( long time ) { thunk(); }
    public void fundamentalData( int reqId, String data ) { thunk(); }
    public void deltaNeutralValidation( int reqId, UnderComp underComp ) { thunk(); }
    public void tickSnapshotEnd( int reqId ) { thunk(); }

    /*

    ///////////////////////////////////////////////////////////////////////
    // EWrapper Interface methods
    ///////////////////////////////////////////////////////////////////////
    void tickPrice( int tickerId, int field, double price, int canAutoExecute );
    void tickSize( int tickerId, int field, int size );
    void tickOptionComputation( int tickerId, int field, double impliedVol,
                                double delta, double modelPrice, double pvDividend );
    void tickGeneric( int tickerId, int tickType, double value );
    void tickString( int tickerId, int tickType, String value );
    void tickEFP( int tickerId, int tickType, double basisPoints,
                  String formattedBasisPoints, double impliedFuture, int holdDays,
                  String futureExpiry, double dividendImpact, double dividendsToExpiry );
    void orderStatus( int orderId, String status, int filled, int remaining,
                      double avgFillPrice, int permId, int parentId, double lastFillPrice,
                      int clientId, String whyHeld );
    void openOrder( int orderId, Contract contract, Order order, OrderState orderState );
    void openOrderEnd();
    void updateAccountValue( String key, String value, String currency, String accountName );
    void updatePortfolio( Contract contract, int position, double marketPrice, double marketValue,
                          double averageCost, double unrealizedPNL, double realizedPNL, String accountName );
    void updateAccountTime( String timeStamp );
    void accountDownloadEnd( String accountName );
    void nextValidId( int orderId );
    void contractDetails( int reqId, ContractDetails contractDetails );
    void bondContractDetails( int reqId, ContractDetails contractDetails );
    void contractDetailsEnd( int reqId );
    void execDetails( int reqId, Contract contract, Execution execution );
    void execDetailsEnd( int reqId );
    void updateMktDepth( int tickerId, int position, int operation, int side, double price, int size );
    void updateMktDepthL2( int tickerId, int position, String marketMaker, int operation,
                           int side, double price, int size );
    void updateNewsBulletin( int msgId, int msgType, String message, String origExchange );
    void managedAccounts( String accountsList );
    void receiveFA( int faDataType, String xml );
    void historicalData( int reqId, String date, double open, double high, double low,
                         double close, int volume, int count, double WAP, boolean hasGaps );
    void scannerParameters( String xml );
    void scannerData( int reqId, int rank, ContractDetails contractDetails, String distance,
                      String benchmark, String projection, String legsStr );
    void scannerDataEnd( int reqId );
    void realtimeBar( int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count );
    void currentTime( long time );
    void fundamentalData( int reqId, String data );
    void deltaNeutralValidation( int reqId, UnderComp underComp );
    void tickSnapshotEnd( int reqId );
    */
}