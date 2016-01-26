package org.neo4j.coreedge.raft;

import org.neo4j.coreedge.raft.outcome.Outcome;
import org.neo4j.coreedge.raft.state.RaftState;

import static java.lang.String.format;
import static java.lang.String.valueOf;

public interface OutcomeLogger<MEMBER>
{
    void info( RaftState<MEMBER> state, RaftMessages.Message<MEMBER> message, Outcome<MEMBER> outcome );
}

