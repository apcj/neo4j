package org.neo4j.coreedge.raft;

import org.neo4j.coreedge.raft.outcome.Outcome;
import org.neo4j.coreedge.raft.state.RaftState;

public class NullOutcomeLogger implements OutcomeLogger
{
    @Override
    public void info( RaftState state, RaftMessages.Message message, Outcome outcome )
    {

    }
}
