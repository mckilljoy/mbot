package mbot;

public class SystemControllerException extends Exception
{
    public SystemControllerException( String message )
    {
        super(message);
    }

    public SystemControllerException( Exception e )
    {
        super(e);
    }
}