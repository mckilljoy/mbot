import mbot.Scheduler;
import mbot.TestJob;
import mbot.Job;
//import mbot.*;

public class TestMain
{

    public static void main (String args[])
    {
        System.out.println("Starting test main");

        System.out.println("Creating scheduler");
        Scheduler scheduler = new Scheduler();

        System.out.println("Starting scheduler thread");
        scheduler.startEvaluatorThread();

        
        TestJob tj1 = new TestJob();
        TestJob tj2 = new TestJob();
        TestJob tj3 = new TestJob();

        System.out.println("Queueing jobs");

        scheduler.scheduleNow( tj1 );
        scheduler.scheduleAtTimeDelta( tj2, 10000 );
        scheduler.scheduleAtTimeDelta( tj2, 10000 );
        scheduler.scheduleAtTimeDelta( tj3, 10000 );

        System.out.println("Done queueing jobs");
        
    }
}