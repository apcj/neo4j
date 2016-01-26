package org.neo4j.coreedge.raft;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.neo4j.coreedge.raft.outcome.Outcome;

import static java.lang.String.format;
import static java.lang.String.valueOf;

public interface OutcomeLogger<MEMBER>
{
    void info( RaftMessages.Message<MEMBER> message, Outcome<MEMBER> outcome );
}

