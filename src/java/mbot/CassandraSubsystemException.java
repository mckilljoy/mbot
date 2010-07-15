package mbot;

public class CassandraSubsystemException extends Exception
{
    public CassandraSubsystemException( String message )
    {
        super(message);
    }

    public CassandraSubsystemException( Exception e )
    {
        super(e);
    }
}