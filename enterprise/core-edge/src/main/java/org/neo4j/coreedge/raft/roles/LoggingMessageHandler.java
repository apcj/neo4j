package org.neo4j.coreedge.raft.roles;

import org.neo4j.coreedge.raft.OutcomeLogger;
import org.neo4j.coreedge.raft.RaftMessageHandler;
import org.neo4j.coreedge.raft.RaftMessages;
import org.neo4j.coreedge.raft.log.RaftStorageException;
import org.neo4j.coreedge.raft.outcome.Outcome;
import org.neo4j.coreedge.raft.state.ReadableRaftState;
import org.neo4j.logging.Log;

public class LoggingMessageHandler implements RaftMessageHandler
{
    private RaftMessageHandler handler;
    private OutcomeLogger outcomeLogger;

    public LoggingMessageHandler( RaftMessageHandler handler, OutcomeLogger outcomeLogger )
    {
        this.handler = handler;
        this.outcomeLogger = outcomeLogger;
    }

    @Override
    public <MEMBER> Outcome<MEMBER> handle( RaftMessages.Message<MEMBER> message, ReadableRaftState<MEMBER> context,
                                            Log log ) throws RaftStorageException
    {
        Outcome<MEMBER> outcome = handler.handle( message, context, log );

        outcomeLogger.info( message, outcome );

        return outcome;

    }
}
