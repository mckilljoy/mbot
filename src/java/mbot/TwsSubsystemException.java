package mbot;

public class TwsSubsystemException extends Exception
{
    public TwsSubsystemException( String message )
    {
        super(message);
    }

    public TwsSubsystemException( Exception e )
    {
        super(e);
    }
}