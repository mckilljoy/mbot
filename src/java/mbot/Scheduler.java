package mbot;

// Java imports
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.TreeSet;


//
// The scheduler allows for jobs to be scheduled over time.
// They are store in a sorted queue and run in order as 
//
//
public class Scheduler implements Runnable
{

    //
    // The backing thread for the scheduler
    //
    Thread thread;

    //
    // This is where jobs wait until they are scheduled to run
    //
    TreeSet<Job> waitingQueue = null;

    //
    // This is where running jobs are stored
    //
    TreeSet<Job> runningQueue = null;

    //
    // Complete jobs are put here, awaiting cleanup
    //
    TreeSet<Job> completedQueue = null;

    //
    // Constructor
    //
    public Scheduler()
    {
        waitingQueue = new TreeSet<Job>();
        runningQueue = new TreeSet<Job>();
        completedQueue = new TreeSet<Job>();
    }

    //
    // Shutdown the subsystem
    //
    public void shutdown()
    {
        endEvaluatorThread();
    }

    //
    // Start the evaluator thread
    //
    public void startEvaluatorThread()
    {
        thread = new Thread(this);

        thread.start();
    }

    //
    // Call this when our class is being closed
    //
    private void endEvaluatorThread()
    {
        thread.interrupt();
    }

    //
    // Wake up the evaluator thread to see if there
    // is anything to run right now
    //
    private void notifyEvaluatorThread()
    {
        notify();
    }

    //
    // Scheduled immediately
    //
    public synchronized void scheduleNow( Job job )
        throws SchedulerException
    {

        //
        // Set the start time (to zero)
        //
        job.startTime = Job.JOB_START_IMMEDIATELY;

        //
        // Add this job with its start time to the queue
        //
        boolean result = waitingQueue.add( job );

        if( result == false )
        {
            throw new SchedulerException("Failed to add job to wait queue");
        }

        //
        // Notify the evaluator that something new is waiting
        //
        notifyEvaluatorThread();
        
    }

    //
    // Schedule for a specific time
    //
    public synchronized void scheduleAtTime( Job job, long startTime )
        throws SchedulerException
    {

        //
        // Set the start time of the job
        //
        job.startTime = startTime;

        //
        // Add this job with its start time to the queue
        //
        boolean result = waitingQueue.add( job );

        if( result == false )
        {
            throw new SchedulerException("Failed to add job to wait queue");
        }

        //
        // Notify the evaluator that something new is waiting
        //
        notifyEvaluatorThread();
    }

    //
    // Run after a certain delta from now
    //
    public synchronized void scheduleAtTimeDelta( Job job, long startTimeDelta )
        throws SchedulerException
    {

        //
        // Get the current time
        //
        long now = currentTime();

        //
        // Add the delta to the now time
        //
        job.startTime = now + startTimeDelta;

        //
        // Add this job with its start time to the queue
        //
        boolean result = waitingQueue.add( job );

        if( result == false )
        {
            throw new SchedulerException("Failed to add job to wait queue");
        }

        //
        // Notify the evaluator that something new is waiting
        //
        notifyEvaluatorThread();

    }

    //
    // run the job
    //
    private synchronized void startJob( Job job )
    {

        job.scheduler = this;

        //
        // Track the running job on the queue
        //
        runningQueue.add( job );

        //
        // The job itself will take care of spinning a thread up
        //
        job.start();

    }

    private synchronized void cancelRunningJob( Job job )
    {
        job.cancel();
    }

    //
    // The job calls this to signify it is done.
    //
    public synchronized void completionCallback( Job job )
    {
        //
        // Currently we don't care what the status is (e.g. canceled)
        //
        runningQueue.remove( job );

        //
        // The completedQueue isn't used for now
        //
        //completedQueue.add( job );
    }

    public synchronized void run()
    {

        try
        {
            //
            // Main event loop
            //
            while( true )
            {

                long timeout = 0;

                //
                // There are jobs waiting, figure out if any
                // can be run immediately
                //
                if( waitingQueue.size() > 0 )
                {

                    //
                    // Find out when we need to wakeup next
                    //
                    Job job = waitingQueue.first();

                    long wakeupTime = job.startTime;
                    long now = currentTime();

                    if( wakeupTime > now )
                    {
                        //
                        // It is not time for this job yet, wait
                        //
                        timeout = wakeupTime - now;
                    }
                    else
                    {
                        //
                        // It is time to run this job
                        //
                        waitingQueue.remove( job );
                        
                        startJob( job );

                        continue;

                    }

                }

                //
                // We failed to get a job that can be run immediately, 
                // so we must wait() on the queue.
                //
                //
                // If timeout is zero, wait indefinitely
                //
                wait( timeout );

            }

        }
        catch ( InterruptedException e )
        {
            //
            // This ends the thread
            //
            return;
        }

    }

    //
    // Return the current time in MS
    //
    public long currentTime()
    {
        return System.currentTimeMillis();
    }

}