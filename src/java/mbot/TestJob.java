package mbot;

//
// This class represents all jobs relating to the MktStk datafeed
//
public class TestJob extends Job
{

    public void executeJob()
    {
        System.out.println("Job is running at " + startTime);
    }

}