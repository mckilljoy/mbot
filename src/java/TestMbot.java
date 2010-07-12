
import scc.*;

//import mbot.Scheduler;
//import mbot.TestJob;
//import mbot.Job;
import mbot.*;
import mbot.Scheduler;

import java.lang.Thread;

public class TestMbot
{

    public static void main (String args[])
        throws SystemControllerException
    {
        System.out.println("Starting test main");
        testSystemController();
    }

    static void testSystemController()
        throws SystemControllerException
    {

        SystemController sc = new SystemController();

    }

    static void testMktStkController()
    {

        /*
        SimpleCassandraClient scc = new SimpleCassandraClient();
        scc.connect( "localhost", 9160 );

        Scheduler scheduler = new Scheduler();
        scheduler.startEvaluatorThread();

        MktStkSubController msc = new MktStkController( scheduler, scc );
        MktStkController msc2 = new MktStkController( scheduler, scc );

        msc.connectToTws();
        msc2.connectToTws();
        msc.initFromControl();
        msc2.initFromControl();

        try
        {
            Thread.sleep(10000);
        }
        catch ( Exception e )
        {
            //derrr
        }

        msc.addToControl( "MSFT" );
        msc.removeFromControl("AAPL");

        msc.refreshFromControl();

        msc.addToControl( "AAPL" );
        msc.removeFromControl("MSFT");
        */
    }

    void testJobScheduler()
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