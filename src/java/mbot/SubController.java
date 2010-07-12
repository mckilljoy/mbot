package mbot;

// SCC imports
import scc.*;

// IB imports
import com.ib.client.*;

// Java imports
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.Hashtable;

//
// Classes that implement this interface can handle scheduling
// various tasks to accomplish their goals. The actual scheduling
// of jobs is most likely handled by a single (external) scheduler.
//
public abstract class SubController extends Controller
{

    //
    // The parent controller
    //
    protected SystemController parent;

    //
    // Track all jobs we have scheduled
    //
    protected LinkedList<Job> pendingJobs;

    //
    // This maps the datafeeds to clientIds
    //
    // Hacky to have both directions, any better way?
    //
    protected Hashtable<Integer, String> clientIdToSymbol;
    protected Hashtable<String, Integer> symbolToClientId;

    //
    // Convenience method to create double mapping
    //
    protected void createMapping( Integer clientId, String symbol )
    {
        clientIdToSymbol.put( clientId, symbol );
        symbolToClientId.put( symbol, clientId );
    }

    //
    // This returns a valid clientId for TWS requests
    //
    protected int getNextValidTwsClientId()
    {
        return parent.getNextValidTwsClientId( this );
    }

    //
    // This is called by the scheduler when a job is complete
    //
    protected abstract void completionCallback( Job job );

}