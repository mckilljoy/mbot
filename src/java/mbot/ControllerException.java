package mbot;

public class ControllerException extends Exception
{
    public ControllerException( String message )
    {
        super(message);
    }

    public ControllerException( Exception e )
    {
        super(e);
    }
}