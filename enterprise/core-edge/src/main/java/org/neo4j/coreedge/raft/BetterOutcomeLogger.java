package org.neo4j.coreedge.raft;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.neo4j.coreedge.raft.outcome.Outcome;

import static java.lang.String.format;
import static java.lang.String.valueOf;

public class BetterOutcomeLogger implements OutcomeLogger
{
    private PrintWriter printWriter;
    private DateFormat dateFormat = new SimpleDateFormat( "HH:mm:ss.SSS" );

    public BetterOutcomeLogger( PrintWriter printWriter )
    {
        this.printWriter = printWriter;
    }

    @Override
    public void info( RaftMessages.Message message, Outcome outcome )
    {
        printWriter.println( format( "%s -->%s: %s",
                dateFormat.format( new Date() ),
                message.getClass().getSimpleName(),
                valueOf( message )) );

        printWriter.println( format( "%s -->%s",
                dateFormat.format( new Date() ),
                outcome.logString() ) );
        printWriter.println("-----");
        printWriter.flush();
    }
}
