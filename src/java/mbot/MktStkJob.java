package mbot;

// IB imports
import com.ib.client.Contract;
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
    private EClientSocket twsClient = null;

    //
    // The client Id to use for this job
    //
    private int twsClientId;

    //
    // Contract object
    //
    private Contract twsContract;

    //
    // Generic ticks determine what kind of feed data we get.
    // The default ones (basically all ticks) should be good enough.
    //
    final static String ALL_GENERIC_TICK_TAGS = "mdoff,100,101,104,105,106,107,165,221,225,233,236,258";
    private String twsGenericTicks = ALL_GENERIC_TICK_TAGS;

    //
    // Whether or not to take just a snapshot of mkt data
    //
    private boolean twsSnapshotMktData;

    //
    // Contstructor
    //
    public MktStkJob( int jobType,
                      EClientSocket twsClient,
                      int twsClientId )
    {

        this.jobType = jobType;
        this.twsClient = twsClient;
        this.twsClientId = twsClientId;

    }

    public MktStkJob( int jobType,
                      EClientSocket twsClient,
                      int twsClientId,
                      Contract twsContract,
                      boolean twsSnapshotMktData )
    {

        this.jobType = jobType;
        this.twsClient = twsClient;
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
            cancelFeed();
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
        twsClient.reqMktData( twsClientId,
                              twsContract,
                              twsGenericTicks,
                              twsSnapshotMktData);

    }

    //
    // Stop datafeed
    //
    private void stopFeed()
    {

        //
        // Make the IB API call
        //
        twsClient.cancelMktData( twsClientId );

    }

    //
    // Cancel an outstanding datafeed request
    //
    private void cancelFeed()
    {

        //
        // The same as stopping a feed
        //
        stopFeed();

    }


}