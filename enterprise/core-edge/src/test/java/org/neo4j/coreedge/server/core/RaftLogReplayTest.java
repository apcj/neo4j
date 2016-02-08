package org.neo4j.coreedge.server.core;

import org.junit.Test;

import org.neo4j.coreedge.raft.ReplicatedInteger;
import org.neo4j.coreedge.raft.log.InMemoryRaftLog;
import org.neo4j.coreedge.raft.log.RaftLogEntry;
import org.neo4j.coreedge.raft.state.StateMachine;
import org.neo4j.logging.NullLogProvider;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class RaftLogReplayTest
{
    @Test
    public void shouldReplayLastCommittedEntry() throws Throwable
    {
        // given
        StateMachine stateMachine = mock( StateMachine.class );
        InMemoryRaftLog raftLog = new InMemoryRaftLog();
        raftLog.append( new RaftLogEntry( 0, ReplicatedInteger.valueOf( 0 ) ) );
        raftLog.append( new RaftLogEntry( 0, ReplicatedInteger.valueOf( 1 ) ) );
        raftLog.append( new RaftLogEntry( 0, ReplicatedInteger.valueOf( 2 ) ) );
        raftLog.commit( 2 );
        raftLog.append( new RaftLogEntry( 0, ReplicatedInteger.valueOf( 3 ) ) );

        RaftLogReplay replayer = new RaftLogReplay( stateMachine, raftLog, NullLogProvider.getInstance() );

        // when
        replayer.start();

        // then
        verify( stateMachine ).applyCommand( ReplicatedInteger.valueOf( 2 ), 2 );
    }
}