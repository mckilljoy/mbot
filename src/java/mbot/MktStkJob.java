package mbot;

// IB imports
import samples.base.StkContract;
import com.ib.client.EClientSocket;

//
// This class represents all jobs relating to the MktStk datafeed
//
public class MktStkJob extends Job
{

    //
    // Job defs
    //
    public static final int JOB_INVALID = 0;
    public static final int JOB_START_FEED = 1;
    public static final int JOB_STOP_FEED = 2;

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
    // The client Id to use for this job
    //
    //private int twsClientId;

    //
    // Contract object
    //
    //private StkContract twsContract;
    private String symbol;

    //
    // Generic ticks determine what kind of feed data we get.
    // The default ones (basically all ticks) should be good enough.
    //
    //final static String ALL_GENERIC_TICK_TAGS = "mdoff,100,101,104,105,106,107,165,221,225,233,236,258";
    //private String twsGenericTicks = ALL_GENERIC_TICK_TAGS;

    //
    // Whether or not to take just a snapshot of mkt data
    //
    // private boolean twsSnapshotMktData;
    //
    // Always false for now
    //
    private boolean snapshot = false;

    //
    // Contstructor
    //
    /*
    public MktStkJob( int jobType,
                      TwsSubsystem twsSubsystem,
                      int twsClientId )
    {

        this.jobType = jobType;
        this.twsSubsystem = twsSubsystem;
        this.twsClientId = twsClientId;

    }

    public MktStkJob( int jobType,
                      TwsSubsystem twsSubsystem,
                      int twsClientId,
                      StkContract twsContract,
                      boolean twsSnapshotMktData )
    {

        this.jobType = jobType;
        this.twsSubsystem = twsSubsystem;
        this.twsClientId = twsClientId;
        this.twsContract = twsContract;
        this.twsSnapshotMktData = twsSnapshotMktData;

        //
        // If we are only taking a snapshot, generic ticks must be empty
        //
        if( twsSnapshotMktData == true )
        {
            this.twsGenericTicks = "";
        }

    }
    */
    public MktStkJob( int jobType, TwsSubsystem twsSubsystem, Controller controller, String symbol )
    {

        this.jobType = jobType;
        this.twsSubsystem = twsSubsystem;
        this.controller = controller;
        this.symbol = symbol;

    }

    //
    // Implement job::executeJob()
    //
    public void executeJob()
    {

        switch( jobType )
        {
        case JOB_START_FEED:
            startFeed();
            break;
        case JOB_STOP_FEED:
            stopFeed();
            break;
        default:
            // do nothing
        }

    }

    public void cancelJob()
    {
        //
        // Only take action if the job type is start feed.
        // Otherwise, sending another cancelation isn't necessary.
        //
        switch( jobType )
        {
        case JOB_START_FEED:
            stopFeed();
            break;
        default:
            // do nothing
        }

    }

    //
    // Start datafeed
    //
    private void startFeed()
    {

        //
        // Make the IB API call
        //
        try
        {
            twsSubsystem.requestMktStkData( controller, symbol );
        }
        catch( TwsSubsystemException e )
        {
            System.out.println("Exception requesting mkt stk data: " + e.getMessage() );
        }

    }

    //
    // Stop datafeed
    //
    private void stopFeed()
    {

        //
        // Make the IB API call
        //
        try
        {
            twsSubsystem.cancelMktStkData( controller, symbol );
        }
        catch( TwsSubsystemException e )
        {
            System.out.println("Exception cancelling mkt stk data: " + e.getMessage() );
        }

    }

}