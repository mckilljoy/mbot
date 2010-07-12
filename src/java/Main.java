
import scc.*;

public class Main
{
   
    public static void main (String args[])
    {
        System.out.println("Starting mbot");

        /*
        SimpleCassandraClient scc = new SimpleCassandraClient();

        if( scc.connect( "localhost", 9160 ) )
        {
            System.out.println("Connecting to Cassandra server at localhost/9160");
        }
        else
        {
            System.out.println("Failed to connect to Cassandra server");
            return;
        }
        */

        //SimpleCassandraClient.describe("Keyspace1");

        //System.out.println("Testing Cassandra connection");
        //test();

        //String[] omArgs;
        //omArgs[0] = scc;
        //TestJavaClient.OldMain.main(args);

        //System.out.println("Disconnecting from Cassandra server");
        //SimpleCassandraClient.disconnect();
        
    }
    
    /*
    public static void test( SimpleCassandraClient scc ) 
    {

        if( scc.insert("Keyspace1", "Standard2", "aapl", "2010", "250.0".getBytes()) )
        {
            System.out.println("Insert succeeded");
        }
        else
        {
            System.out.println("Insert failed");
        }

        if( scc.insert("Keyspace1", "Super1", "aapl", "12:00", "2010", "250.0".getBytes()) )
        {
            System.out.println("Insert super succeeded");
        }
        else
        {
            System.out.println("Insert super failed");
        }

        byte[] value;

        if( (value = scc.get("Keyspace1", "Standard2", "aapl", "2010")) != null )
        {
            System.out.println( new String(value) );
        }
        else
        {
            System.out.println("Get failed");
        }

        if( (value = scc.get("Keyspace1", "Super1", "aapl", "12:00", "2010")) != null )
        {
            System.out.println( new String(value) );
        }
        else
        {
            System.out.println("Get super failed");
        }
    }
    */
 
    /*
    static public void inform( final Component parent, final String str)
    {
        TestJavaClient.Main.inform( parent, str );
    }

    static private void showMsg( Component parent, String str, int type)
    {
        TestJavaClient.Main.showMsg( parent, str, type );
    }
    */
}