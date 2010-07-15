package mbot;

import scc.*;

// Java imports
import java.util.HashMap;

//
// This implements a subsystem that interfaces with a cassandra db.
//

public class CassandraSubsystem
{

    String name = "CassandraSubsystem";

    //
    // The actually cassandra client
    //
    SimpleCassandraClient cassandraClient;

    String serverHostname;
    int serverPort;

    //
    // Constructor
    //
    public CassandraSubsystem()
        throws CassandraSubsystemException
    {
        this( "localhost", 9160 );
    }

    public CassandraSubsystem( String serverHostname,
                               int serverPort )
        throws CassandraSubsystemException
    {
        
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;

        try
        {
            
            cassandraClient = new SimpleCassandraClient();
            cassandraClient.connect( serverHostname, serverPort );

            if( !cassandraClient.isConnected() )
            {
                throw new CassandraSubsystemException("Failed to connect to Cassandra Server");
            }

        }
        //catch( CassandraException e )
        //{
        //    throw new CassandraSubsystemException("Cassandra exception: " + e.getMessage() );
        //}
        //catch( CassandraSubsystemException e )
        //{
        //    throw e;
        //}
        catch( Exception e )
        {
            throw new CassandraSubsystemException( e );
        }
    }

    //
    // Convenience
    //
    public String getServerInfo()
    {
        return "Cassandra server '" + cassandraClient.clusterName + "' at " +
            serverHostname + "/" + serverPort;
    }

    //
    // Shutdown the subsystem
    //
    public void shutdown()
    {
        cassandraClient.disconnect();
    }

    //
    // The public insert method
    //
    public void insert( String keyspace,
                        String columnFamily,
                        String key,
                        String column,
                        byte[] value )
        throws CassandraSubsystemException
    {
        insert( keyspace, columnFamily, key, null, column, value );
    }

    public void insert( String keyspace,
                        String columnFamily,
                        String key,
                        String superColumn,
                        String column,
                        byte[] value )
        throws CassandraSubsystemException
    {

        try
        {

            cassandraClient.insert( keyspace,
                                    columnFamily,
                                    key,
                                    superColumn,
                                    column,
                                    value );

        }
        //catch( CassandraException e )
        //{
        //    throw new CassandraSubsystemException("Cassandra exception: " + e.getMessage() );
        //}
        //catch( Exception e )
        //{
        //   throw new CassandraSubsystemException( e );
        //}
        catch( Exception e )
        {
            throw new CassandraSubsystemException( "Exception inserting " +
                                                   keyspace + "|" +
                                                   columnFamily + "|" +
                                                   key + "|" +
                                                   superColumn + "|" +
                                                   column + "|" +
                                                   value + ": " +
                                                   e.getMessage() );
        }


    }

    //
    // The public delete method
    //
    public void delete( String keyspace,
                        String columnFamily,
                        String key,
                        String column )
        throws CassandraSubsystemException
    {
        delete( keyspace, columnFamily, key, null, column );
    }

    public void delete( String keyspace,
                        String columnFamily,
                        String key,
                        String superColumn,
                        String column )
        throws CassandraSubsystemException
    {

        try
        {

            cassandraClient.delete( keyspace,
                                    columnFamily,
                                    key,
                                    superColumn,
                                    column );

        }
        //catch( CassandraException e )
        //{
        //    throw new CassandraSubsystemException("Cassandra exception: " + e.getMessage() );
        //}
        //catch( Exception e )
        //{
        //    throw new CassandraSubsystemException( e );
        //}
        catch( Exception e )
        {
            throw new CassandraSubsystemException( "Exception deleting " +
                                                   keyspace + "|" +
                                                   columnFamily + "|" +
                                                   key + "|" +
                                                   superColumn + "|" +
                                                   column + ": " +
                                                   e.getMessage() );
        }


    }


    //
    // GET
    //

    //
    // The public get method
    //
    public byte[] get( String keyspace,
                       String columnFamily,
                       String key,
                       String column )
        throws CassandraSubsystemException
    {
        return get( keyspace, columnFamily, key, null, column );
    }

    public byte[] get( String keyspace,
                       String columnFamily,
                       String key,
                       String superColumn,
                       String column )
        throws CassandraSubsystemException
    {

        try
        {
            return cassandraClient.get( keyspace,
                                        columnFamily,
                                        key,
                                        superColumn,
                                        column );

        }
        //catch( CassandraException e )
        //{
        //    throw new CassandraSubsystemException("Cassandra exception: " + e.getMessage() );
        //}
        //catch( Exception e )
        //{
        //    throw new CassandraSubsystemException( e );
        //}
        catch( Exception e )
        {
            throw new CassandraSubsystemException( "Exception getting " +
                                                   keyspace + "|" +
                                                   columnFamily + "|" +
                                                   key + "|" +
                                                   superColumn + "|" +
                                                   column + ": " +
                                                   e.getMessage() );
        }

    }

    //
    // GET_SLICE
    //

    //
    // The public getSlice method
    //
    public HashMap getSlice( String keyspace,
                             String columnFamily,
                             String key )
        throws CassandraSubsystemException
    {
        return getSlice( keyspace, columnFamily, key, null );
    }

    public HashMap getSlice( String keyspace,
                             String columnFamily,
                             String key,
                             String superColumn )
        throws CassandraSubsystemException
    {

        try
        {

            return cassandraClient.getSlice( keyspace,
                                             columnFamily,
                                             key,
                                             superColumn );

        }
        //catch( CassandraException e )
        //{
        //    throw new CassandraSubsystemException("Cassandra exception: " + e.getMessage() );
        //}
        catch( Exception e )
        {
            throw new CassandraSubsystemException( "Exception getting slice " +
                                                   keyspace + "|" +
                                                   columnFamily + "|" +
                                                   key + "|" +
                                                   superColumn + ": " +
                                                   e.getMessage() );
        }

    }

}