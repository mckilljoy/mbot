package mbot;

import scc.SimpleCassandraServer;

//
// Classes that implement this interface can handle scheduling
// various tasks to accomplish their goals. The actual scheduling
// of jobs is most likely handled by a single (external) scheduler.
//
public class abstract Controller
{

    //
    // The controllers name can be used for locating metadata
    //
    String controllerName;
    //
    // This is the keyspace for this controller's data
    //
    String cassandraKeyspace;

    //
    // All controller metadata is in this keyspace
    //
    String controlKeyspace = "Control";

    //
    // This is the open connection to the cassandra instance
    //
    SimpleCassandraClient cassandraClient;

    //
    // The IB API requires a unique client id for each datafeed
    // These must be tracked by the controller to avoid collision
    // A simple range should be good enough for now.
    //
    int twsClientIdStart;
    int twsClientIdEnd;
    int twsClientIdCurrentUnassigned;

    //
    // Read control metadata from cassandra and start doing stuff
    //
    public init();

    //
    // Refresh metadata from cassandra. Might just map to init()
    //
    public refresh();
}