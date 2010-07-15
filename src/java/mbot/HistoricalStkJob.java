package mbot;

// IB imports
import samples.base.StkContract;
import com.ib.client.EClientSocket;

//
// This class represents all jobs relating to a historical stk request
//
public class HistoricalStkJob extends Job
{

    //
    // Job defs
    //
    public static final int JOB_INVALID = 0;
    public static final int JOB_CANCEL_REQUEST = 1;
    public static final int JOB_REQUEST_1SEC_TRADES = 2;

    private int jobType = JOB_INVALID;

    //
    // TWS EClientSock object to make IB API calls
    //
    private TwsSubsystem twsSubsystem;

    //
    // The controller that issued this job
    //
    private Controller controller;


    //
    // Request info
    //

    //
    // Info to use for the request, must be converted to a Contract
    //
    private String symbol;

    //
    // the format yyyymmdd hh:mm:ss tmz
    //
    private Long endDateTime;

    //
    // "<number> <duration"
    // Valid durations: S (seconds), D (days), W (weeks), M (months), Y (years)
    //
    private String duration;

    //
    // "<number> <duration>"
    // Durations: secs, min(s), hour(s), day(s), week(s), month(s), year
    //

    private String barSize;

    //
    // TRADES, MIDPOINT, BID, ASK, BID_ASK,
    // HISTORICAL_VOLATILITY, OPTION_IMPLIED_VOLATILITY, OPTION_VOLUME
    //
    private String whatToShow;

    //
    // 0 -- false
    // 1 -- true
    //
    private int regularHours = 0;

    //
    // 1 -- yyyymmdd{space}{space}hh:mm:dd
    // 2 -- number of seconds since 1/1/1970 GMT
    //
    private int formatDate = 2;

    //
    // Contstructor
    //
    public HistoricalStkJob( int jobType,
                             TwsSubsystem twsSubsystem,
                             Controller controller,
                             String symbol,
                             Long endDateTime )
        throws JobException
    {

        this.jobType = jobType;
        this.twsSubsystem = twsSubsystem;
        this.controller = controller;
        this.symbol = symbol;
        this.endDateTime = endDateTime;

        if( jobType == JOB_REQUEST_1SEC_TRADES )
        {
            
            //
            // Limited to 2000 bars per request.
            // 30 minutes @ 1 second bars (1800 bars)
            //
            duration = "1800 S";
            barSize = "1 secs";

            whatToShow = "TRADES";

            regularHours = 0;
            formatDate = 2;
            
        }
        else
        {
            throw new JobException("Invalid job type");
        }

    }

    //
    // Only cancel job
    //
    public HistoricalStkJob( int jobType,
                             TwsSubsystem twsSubsystem,
                             Controller controller,
                             String symbol )
        throws JobException
    {

        this.jobType = jobType;
        this.twsSubsystem = twsSubsystem;
        this.controller = controller;
        this.symbol = symbol;

        if( jobType != JOB_CANCEL_REQUEST )
        {
            throw new JobException("Invalid job type");
        }
    }

    //
    // Implement job::executeJob()
    //
    public void executeJob()
        throws JobException
    {

        switch( jobType )
        {
        case JOB_CANCEL_REQUEST:
            cancelRequest();
            break;
        case JOB_REQUEST_1SEC_TRADES:
            requestData();
            break;
        default:
            // do nothing
        }

    }

    public void cancelJob()
        throws JobException
    {

        switch( jobType )
        {
        default:
            cancelRequest();
        }

    }

    //
    // Request data
    //
    private void requestData()
        throws JobException
    {

        //
        // Make the IB API call
        //
        try
        {
            twsSubsystem.requestHistoricalStkData( controller,
                                                   symbol,
                                                   endDateTime,
                                                   duration,
                                                   barSize,
                                                   whatToShow );
        }
        catch( TwsSubsystemException e )
        {
            throw new JobException("Exception requesting data: " + e.getMessage() );
        }

    }

    private void cancelRequest()
        throws JobException
    {
        
        //
        // Make the IB API call
        //
        try
        {
            twsSubsystem.cancelHistoricalStkData( controller, symbol );
        }
        catch( TwsSubsystemException e )
        {
            throw new JobException("Exception canceling request: " + e.getMessage() );
        }
        
    }

}