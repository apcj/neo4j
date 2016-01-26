package org.neo4j.coreedge.raft;

import org.neo4j.coreedge.raft.outcome.Outcome;

public class NullOutcomeLogger implements OutcomeLogger
{
    @Override
    public void info( RaftMessages.Message message, Outcome outcome )
    {

    }
}
