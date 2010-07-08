package mbot;

//
// A job encapsulates a collection of things to do, and 
// might be  be scheduled to run at a certain time.
//
public abstract class Job implements Comparable<Job>, Runnable
{

    //
    // The thread that backs this job
    //
    Thread thread;

    //
    // The scheduler scheduling this job
    //
    Scheduler scheduler;

    //
    // Status defs
    //
    public static int JOB_STATUS_WAITING = 0;
    public static int JOB_STATUS_RUNNING = 1;
    public static int JOB_STATUS_COMPLETE = 2;
    public static int JOB_STATUS_CANCELED = 3;

    public int jobStatus = JOB_STATUS_WAITING;

    //
    // When the job is scheduled to run in MS.
    // A startTime of '0' means immediately.
    // Note that this time is used externally by the scheduler,
    // The job itself doesn't really know/care when it is run.
    //
    public static long JOB_START_IMMEDIATELY = 0;
    public long startTime = JOB_START_IMMEDIATELY;


    //
    // The actual job code will be defined here
    //
    abstract void executeJob();

    //
    // This allows for custom job cleanup if canceled
    //
    void cancelJob()
    {
        // nothing by default
    }


    //
    // Synchronized so we don't start() and cancel() a job at the same time
    //
    public synchronized void start()
    {

        // 
        // We don't want more than 1 thread per job
        //
        if( thread != null )
        {
            return;
        }

        //
        // Spin up a new thread to call the job's run() method
        //
        thread = new Thread( this );

        thread.start();

    }

    //
    // Just a run method to run on our thread
    //
    public void run()
    {

        jobStatus = JOB_STATUS_RUNNING;

        //
        // Wrap the actual doJob method with interrupt exception
        // handling. This mechanism is used to cancel the job.
        //
        try
        {

            //
            // Race condition where a cancelation (i.e. interrupt) can
            // come in before we even reach the try/catch block
            //
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }

            //
            // Actually do the job
            //
            executeJob();

            jobStatus = JOB_STATUS_COMPLETE;

        }
        catch (InterruptedException e)
        {
            //
            // End this job by simply returning
            //
            jobStatus = JOB_STATUS_CANCELED;

        }

        //
        // We are done with this thread. This also allows us to 
        // hypothetically start() this job again if we wish.
        //
        thread = null;

        //
        // Tell the scheduler we are done
        //
        scheduler.completionCallback(this);

    }

    //
    // Synchronized so we don't start and cancel a job at the same time
    //
    public synchronized void cancel()
    {

        if( thread == null )
        {
            return;
        }

        //
        // The InterruptedException handler should exit the thread
        //
        thread.interrupt();

    }

    //
    // Not synchronized -- the scheduler cares about the 
    // start times, so it is responsible for modifying them
    // without screwing itself up.
    //
    public int compareTo( Job j )
    {

        //
        // Compare start times
        //
        if( this.startTime < j.startTime )
        {
            return -1;
        }
        else if( this.startTime > j.startTime )
        {
            return +1;
        }
        else
        {
            //
            // Equal -- this actually isn't so unlikely to happen,
            // since jobs default to start time of '0'
            //
            // We want to differentiate between different jobs
            // that have the same start time, as opposed
            // to the same job.
            //

            if( this == j )
            {
                return 0;
            }
            else
            {

                //
                // If they are different objects, just define
                // one to be the bigger.
                //
                return -1;
            }
        }

    }

}