package mbot;

// IB imports
import com.ib.client.*;
import samples.base.StkContract;

// Java imports
import java.util.Hashtable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

//
// This implements a subsystem that interfaces with a TWS instance.
//
public class TwsSubsystem implements EWrapper
{

    String name = "TwsSubsystem";

    //
    // The EClientSocket
    //
    EClientSocket twsClient;

    String serverHostname;
    int serverPort;

    //
    // Which client Id do we start with
    //
    final static int CLIENT_ID_DEFAULT = 0;

    Integer nextValidClientId = new Integer(CLIENT_ID_DEFAULT);

    //
    // Generic ticks
    final static String ALL_GENERIC_TICK_TAGS = "mdoff,100,101,104,105,106,107,165,221,225,233,236,258";
    private String genericTicks = ALL_GENERIC_TICK_TAGS;

    //
    // This maps the datafeeds to clientIds
    //
    Hashtable<Integer, SymbolControllerPair> clientIdToPair;

    //
    // SymbolControllerPair is just a container class
    //
    Hashtable<SymbolControllerPair, Integer> pairToClientId;

    //
    // Constructor
    //
    public TwsSubsystem()
        throws TwsSubsystemException
    {
        this( "localhost", 7496 );
    }

    public TwsSubsystem( String serverHostname,
                         int serverPort )
        throws TwsSubsystemException
    {
        
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;

        //
        // Connect to TWS
        //
        try
        {

            int clientId = getNextValidClientId();

            twsClient = new EClientSocket( this );
            twsClient.eConnect( serverHostname, serverPort, clientId );

            if( !twsClient.isConnected() )
            {
                //System.out.println( controllerName + ": Connected to TWS server version " +
                //twsClient.serverVersion() + " at " +
                //twsClient.TwsConnectionTime());
                throw new TwsSubsystemException("Failed to connect to TWS server");
            }

            //
            // Wait for the nextValidId() to come in. Not critical.
            //
            try
            {
                synchronized( nextValidClientId )
                {
                    nextValidClientId.wait(10000);
                }
            }
            catch( InterruptedException e )
            {
                // nothing
            }

            //
            // clientIdToPair must be set affter we wait on nextValidClientId
            //
            clientIdToPair = new Hashtable<Integer, SymbolControllerPair>();
            pairToClientId = new Hashtable<SymbolControllerPair, Integer>();

        }
        catch( TwsSubsystemException e )
        {
            throw e;
        }
        catch( Exception e )
        {
            throw new TwsSubsystemException( e );
        }
    }

    //
    // Convenience
    //
    public String getServerInfo()
    {
        return "TWS server version " + twsClient.serverVersion() + " at " +
            serverHostname + "/" + serverPort;
    }

    //
    // Shutdown the subsystem
    //
    public void shutdown()
    {
        twsClient.eDisconnect();
    }

    //
    // Tws uses a specially formated date string for input,
    // and a Long timestamp (stored as String) for output.

    //
    // Long -> TwsString
    //
    // TwsString format is "yyyyMMdd HH:mm:ss"
    //
    private String convertDateLongToTwsString( Long time )
    {
        Date date = new Date( time );
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

        return formatter.format( date );
    }

    //
    // TwsStringLong -> Long
    //
    // TwsStringLong is the seconds since 1/1/1970 GMT, stored as a string.
    // Must be converted to milliseconds before returning.
    //
    private Long convertDateTwsStringLongToLong( String time )
    {
        Long timeLong = Long.valueOf( time );

        return timeLong * 1000;
    }



    //
    // This can be made more sophisticated later,
    // e.g. to be more efficient with id space.
    //
    private int getNextValidClientId()
    {
        return nextValidClientId++;
    }

    //
    // This gets the symbol mapped to a specifc client id
    //
    private synchronized SymbolControllerPair lookupPairFromClientId( int clientId )
    {
        SymbolControllerPair scp = clientIdToPair.get( clientId );
        return scp;
    }

    //
    // This gets the symbol mapped to a specifc client id
    //
    private synchronized Controller lookupControllerFromClientId( int clientId )
    {
        SymbolControllerPair scp = clientIdToPair.get( clientId );
        return scp.controller;
    }

    //
    // This gets the controller mapped to a specifc client id
    //
    private synchronized String lookupSymbolFromClientId( int clientId )
    {
        SymbolControllerPair scp = clientIdToPair.get( clientId );
        return scp.symbol;
    }

    //
    // This lookup the mapping between controller+symbol and id
    //
    private synchronized int lookupClientIdFromControllerSymbol( Controller controller, String symbol )
    {
        SymbolControllerPair scp = new SymbolControllerPair( symbol, controller );
        return pairToClientId.get( scp );
    }

    //
    // Controls access to valid client Ids. This helps routing
    // of responses from TWS to the proper controller.
    //
    private synchronized int mapClientId( Controller controller, String symbol )
    {

        int clientId = getNextValidClientId();

        //
        // Track who is using this clientId
        //
        SymbolControllerPair scp = new SymbolControllerPair( symbol, controller );

        clientIdToPair.put( clientId, scp );
        pairToClientId.put( scp, clientId );

        return clientId;

    }

    //
    // This clears the mapping between id and controller, so it will stop
    // getting messages from TWS.
    //
    private synchronized void unmapClientId( Controller controller, String symbol )
    {

        //
        // Stop tracking this mapping
        //
        SymbolControllerPair scp = new SymbolControllerPair( symbol, controller );

        Integer clientId = pairToClientId.remove( scp );

        if( clientId != null )
        {
            clientIdToPair.remove( clientId );
        }

    }

    //
    // Interface functions called by jobs
    //

    //
    // MktStk data requests
    //
    public synchronized void requestMktStkData( Controller controller, String symbol )
        throws TwsSubsystemException
    {

        //
        // Controller must be recorded
        //
        if( controller == null )
        {
            throw new TwsSubsystemException("Controller must not be null");
        }

        if( symbol  == null )
        {
            throw new TwsSubsystemException("Symbol must not be null");
        }

        try
        {
            int clientId = mapClientId( controller, symbol );

            StkContract contract = new StkContract( symbol );

            twsClient.reqMktData( clientId,
                                  contract,
                                  genericTicks,
                                  false );
        }
        catch( Exception e )
        {
            throw new TwsSubsystemException("Exception requesting mkt stk data: " + e.getMessage());
        }

    }

    public synchronized void cancelMktStkData( Controller controller, String symbol )
        throws TwsSubsystemException
    {

        //
        // Conroller must be recorded
        //
        if( controller == null )
        {
            throw new TwsSubsystemException("Controller must not be null");
        }

        if( symbol  == null )
        {
            throw new TwsSubsystemException("Symbol must not be null");
        }

        try
        {
            int clientId = lookupClientIdFromControllerSymbol( controller, symbol );

            unmapClientId( controller, symbol );

            twsClient.cancelMktData( clientId );
        }
        catch( Exception e )
        {
            throw new TwsSubsystemException("Exception canceling mkt stk data: " + e.getMessage());
        }

    }

    //
    // HistoricalStk requests
    //
    public synchronized void requestHistoricalStkData( Controller controller,
                                                       String symbol,
                                                       Long endDateTime,
                                                       String durationStr,
                                                       String barSizeSetting,
                                                       String whatToShow )
        throws TwsSubsystemException
    {

        //
        // FormatDate of 2 means use seconds since 1970
        //
        int formatDate = 2;
        int useRegularTradingHours = 0;

        //
        // Controller must be recorded
        //
        if( controller == null )
        {
            throw new TwsSubsystemException("Controller must not be null");
        }

        if( symbol  == null )
        {
            throw new TwsSubsystemException("Symbol must not be null");
        }

        try
        {
            int clientId = mapClientId( controller, symbol );

            StkContract contract = new StkContract( symbol );

            String endDateTimeString = convertDateLongToTwsString( endDateTime );

            twsClient.reqHistoricalData( clientId,
                                         contract,
                                         endDateTimeString,
                                         durationStr,
                                         barSizeSetting,
                                         whatToShow,
                                         useRegularTradingHours, 
                                         formatDate );
        }
        catch( Exception e )
        {
            throw new TwsSubsystemException("Exception requesting historical stk data: " + e.getMessage());
        }


    }

    public synchronized void cancelHistoricalStkData( Controller controller, String symbol )
        throws TwsSubsystemException
    {

        //
        // Conroller must be recorded
        //
        if( controller == null )
        {
            throw new TwsSubsystemException("Controller must not be null");
        }

        if( symbol  == null )
        {
            throw new TwsSubsystemException("Symbol must not be null");
        }

        try
        {
            int clientId = lookupClientIdFromControllerSymbol( controller, symbol );

            unmapClientId( controller, symbol );

            twsClient.cancelHistoricalData( clientId );
        }
        catch( Exception e )
        {
            throw new TwsSubsystemException("Exception canceling historical stk data: " + e.getMessage());
        }
    }



    //
    // EClientSocket wrapper functions
    //


    //
    // A no-op for callbacks we don't need
    //
    protected void notImplemented()
    {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String caller = stack[2].getMethodName();

        if( caller != null )
        {
            System.out.println( name + ": EWrapper callback '" + caller + "' not implemented");
        }
        else
        {
            System.out.println( name +": Unknown EWrapper callback not implemented");
        }
    }

    //
    // AnyWrapper interface definitions
    //

    public void connectionClosed()
    {

        //
        // Null out the twsClient so no one uses it
        //
        twsClient = null;

        //
        // todo -- notify the parent system of critical error
        //
    }

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

    //
    // EWrapper interface definitions
    //

    //
    // All of these call the appropriate controller callback to indicate the data upwards
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
    public void tickPrice( int tickerId, int field, double price, int canAutoExecute )
    {
        Controller controller = lookupControllerFromClientId( tickerId );
        String symbol = lookupSymbolFromClientId( tickerId );

        String f = TickType.getField( field );
        Double p = new Double( price );
        boolean cae = (canAutoExecute == 0) ? false : true;

        switch( field )
        {
        case TickType.LOW_13_WEEK:
        case TickType.HIGH_13_WEEK:
        case TickType.LOW_26_WEEK:
        case TickType.HIGH_26_WEEK:
        case TickType.LOW_52_WEEK:
        case TickType.HIGH_52_WEEK:
            controller.callbackMiscellaneous( symbol, f, p, cae );
            break;
        default:
            controller.tickPriceCallback( symbol, f, p, cae );
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
    public void tickSize( int tickerId, int field, int size )
    {

        Controller controller = lookupControllerFromClientId( tickerId );
        String symbol = lookupSymbolFromClientId( tickerId );

        String f = TickType.getField( field );
        Integer s = new Integer( size );

        switch( field )
        {
        case TickType.OPTION_CALL_VOLUME:
        case TickType.OPTION_PUT_VOLUME:
            controller.callbackOptionVolume( symbol, f, s );
            break;
        case TickType.OPTION_CALL_OPEN_INTEREST:
        case TickType.OPTION_PUT_OPEN_INTEREST:
            controller.callbackOptionOpenInterest( symbol, f, s );
            break;
        case TickType.AVG_VOLUME:
            controller.callbackMiscellaneous( symbol, f, s );
            break;
        default:
            controller.tickSizeCallback( symbol, f, s );
        }
    }

    public void tickOptionComputation( int tickerId, int field, double impliedVol,
                                       double delta, double modelPrice, double pvDividend ) { notImplemented(); }

    //
    // Returns 'generic' data specified when the feed is request (we request everything)
    //
    // TickTypes can be:
    //  shortable
    //  OptionImpliedVolatility
    //  OptionHistoricalVolatility
    //
    // Shortable maps to the following:
    //  3.0 -- There are at least 1000 shares available for a short sale
    //  2.0 -- This contract will be available for short sale if shares can be located
    //  1.0 -- Not available for short sale
    public void tickGeneric( int tickerId, int tickType, double value )
    {
        Controller controller = lookupControllerFromClientId( tickerId );
        String symbol = lookupSymbolFromClientId( tickerId );

        String f = TickType.getField( tickType );
        Double v = new Double( value );

        switch( tickType )
        {
        case TickType.OPTION_HISTORICAL_VOL:
            controller.callbackOptionHistoricalVolatility( symbol, f, v );
            break;
        case TickType.OPTION_IMPLIED_VOL:
            controller.callbackOptionImpliedVolatility( symbol, f, v );
            break;
        case TickType.SHORTABLE:
            controller.callbackShortable( symbol, f, v );
            break;
        default:
            controller.tickGenericCallback( symbol, f, v );
        }
    }

    //
    // Returns a string with useful trade information
    //
    // TickTypes can be:
    //  RTVolume
    //
    // RTVolume has format:
    //  lastPrice;lastSize;lastTimeMS;totalVolume;VWAP;isSingleTrade
    //
    public void tickString( int tickerId, int tickType, String value )
    {
        Controller controller = lookupControllerFromClientId( tickerId );
        String symbol = lookupSymbolFromClientId( tickerId );

        String f = TickType.getField( tickType );

        StringTokenizer st = new StringTokenizer( value, ";" );

        Double lastPrice = Double.valueOf( st.nextToken() );
        Integer lastSize = Integer.valueOf( st.nextToken() );
        Long lastTimeMS = Long.valueOf( st.nextToken() );
        Integer totalVolume = Integer.valueOf( st.nextToken() );
        Double vwap = Double.valueOf( st.nextToken() );
        boolean isSingleTrade = (st.nextToken() == "true") ? true : false;

        switch( tickType )
        {
        case TickType.RT_VOLUME:
            controller.callbackRTVolume( symbol, lastPrice, lastSize, 
                                         lastTimeMS, totalVolume, vwap, isSingleTrade );
            break;
        default:
            controller.tickStringCallback( symbol, f, value );
        }
    }

    public void tickEFP( int tickerId, int tickType, double basisPoints,
                         String formattedBasisPoints, double impliedFuture, int holdDays,
                         String futureExpiry, double dividendImpact, double dividendsToExpiry ) { notImplemented(); }
    public void orderStatus( int orderId, String status, int filled, int remaining,
                             double avgFillPrice, int permId, int parentId, double lastFillPrice,
                             int clientId, String whyHeld ) { notImplemented(); }
    public void openOrder( int orderId, Contract contract, Order order, OrderState orderState ) { notImplemented(); }
    public void openOrderEnd() { notImplemented(); }
    public void updateAccountValue( String key, String value, String currency, String accountName ) { notImplemented(); }
    public void updatePortfolio( Contract contract, int position, double marketPrice, double marketValue,
                                 double averageCost, double unrealizedPNL, double realizedPNL, String accountName ) { notImplemented(); }
    public void updateAccountTime( String timeStamp ) { notImplemented(); }
    public void accountDownloadEnd( String accountName ) { notImplemented(); }

    //
    // Informs us (the client) what the next valid client id is
    //
    public synchronized void nextValidId( int clientId )
    {
        //
        // Always increase the clientId. This is arguably a bit wasteful
        // with the Id space, but shouldn't generally be a problem.
        //
        nextValidClientId = Math.max( clientId, nextValidClientId );

        //
        // Notfiy a thread (i.e. the constructor) waiting for a valid Id.
        // This is only an issue before subcontrollers are allocated Id.
        //
        if( clientIdToPair == null )
        {
            synchronized( nextValidClientId )
            {
                nextValidClientId.notify();
            }
        }
    }

    public void contractDetails( int reqId, ContractDetails contractDetails ) { notImplemented(); }
    public void bondContractDetails( int reqId, ContractDetails contractDetails ) { notImplemented(); }
    public void contractDetailsEnd( int reqId ) { notImplemented(); }
    public void execDetails( int reqId, Contract contract, Execution execution ) { notImplemented(); }
    public void execDetailsEnd( int reqId ) { notImplemented(); }
    public void updateMktDepth( int tickerId, int position, int operation, int side, double price, int size ) { notImplemented(); }
    public void updateMktDepthL2( int tickerId, int position, String marketMaker, int operation,
                                  int side, double price, int size ) { notImplemented(); }
    public void updateNewsBulletin( int msgId, int msgType, String message, String origExchange ) { notImplemented(); }
    public void managedAccounts( String accountsList ) { notImplemented(); }
    public void receiveFA( int faDataType, String xml ) { notImplemented(); }

    //
    // Receiving data from a historical data request. This represents a single bar of data.
    //
    public void historicalData( int reqId,
                                String date,
                                double open,
                                double high,
                                double low,
                                double close,
                                int volume,
                                int count,
                                double WAP,
                                boolean hasGaps )
    {

        Controller controller = lookupControllerFromClientId( reqId );
        String symbol = lookupSymbolFromClientId( reqId );

        //
        // The date field is overloaded with the 'finish' message
        //
        if( date.indexOf("finished") == -1 )
        {
            Long startDate = convertDateTwsStringLongToLong( date );

            controller.callbackHistoricalData( symbol,
                                               startDate,
                                               new Double( open ),
                                               new Double( high ),
                                               new Double( low ),
                                               new Double( close ),
                                               new Integer( volume ),
                                               new Integer( count ),
                                               new Double( WAP ),
                                               hasGaps );
        }
        else
        {
            controller.callbackHistoricalDataFinished( symbol );
        }

    }

    public void scannerParameters( String xml ) { notImplemented(); }
    public void scannerData( int reqId, int rank, ContractDetails contractDetails, String distance,
                             String benchmark, String projection, String legsStr ) { notImplemented(); }
    public void scannerDataEnd( int reqId ) { notImplemented(); }
    public void realtimeBar( int reqId, long time, double open, double high, double low, double close, long volume, double wap, int count ) { notImplemented(); }
    public void currentTime( long time ) { notImplemented(); }
    public void fundamentalData( int reqId, String data ) { notImplemented(); }
    public void deltaNeutralValidation( int reqId, UnderComp underComp ) { notImplemented(); }
    public void tickSnapshotEnd( int reqId ) { notImplemented(); }

    private class SymbolControllerPair implements Comparable<SymbolControllerPair>
    {

        public String symbol;
        public Controller controller;

        public SymbolControllerPair( String symbol, Controller controller )
        {
            this.symbol = symbol;
            this.controller = controller;
        }

        //
        // Ordering doesn't matter -- we'll just alphabetize on symbol
        //
        public int compareTo( SymbolControllerPair scp )
        {

            //
            // if cont == cont, just test the string
            //
            if( this.controller == scp.controller )
            {
                return this.symbol.compareTo( scp.symbol );
            }

            //
            // we don't want different conts but same strs to return 0
            //
            if( this.symbol != scp.symbol )
            {
                return this.symbol.compareTo( scp.symbol );
            }

            //
            // return -1 to break the tie
            //
            return -1;
        }
    }
}
