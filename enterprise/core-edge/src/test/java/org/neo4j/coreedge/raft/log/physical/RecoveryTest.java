package org.neo4j.coreedge.raft.log.physical;

import java.io.File;

import org.junit.Test;

import org.neo4j.coreedge.raft.log.EntryReader;
import org.neo4j.coreedge.raft.log.RaftLogAppendRecord;
import org.neo4j.coreedge.raft.log.RaftLogEntry;

import static java.util.Arrays.asList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.neo4j.coreedge.raft.ReplicatedInteger.valueOf;
import static org.neo4j.kernel.impl.util.IOCursors.cursor;

public class RecoveryTest
{
    @Test
    public void shouldRecoverStateFromFiles() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( new File( "v1 " ), new File( "v2 " ), new File( "v3 " ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v1 " ) ) ).thenReturn( new Header( 1, -1, -1 ) );
        when( headerReader.readHeader( new File( "v2 " ) ) ).thenReturn( new Header( 2, 9, 0 ) );
        when( headerReader.readHeader( new File( "v3 " ) ) ).thenReturn( new Header( 3, 19, 0 ) );

        EntryReader entryReader = mock( EntryReader.class );
        when( entryReader.readEntriesInVersion( 3 ) ).thenReturn( cursor(
                new RaftLogAppendRecord( 20, new RaftLogEntry( 0, valueOf( 120 ) ) ),
                new RaftLogAppendRecord( 21, new RaftLogEntry( 0, valueOf( 121 ) ) ),
                new RaftLogAppendRecord( 22, new RaftLogEntry( 0, valueOf( 122 ) ) )
        ) );

        Recovery recovery = new Recovery( versionFiles, headerReader, entryReader );

        // when
        Recovery.LogState state = recovery.recover();

        // then
        assertEquals( -1, state.prevIndex );
        assertEquals( -1, state.prevTerm );
        assertEquals( 22, state.appendIndex );
        assertEquals( 3, state.currentVersion );
        assertEquals( 1, state.ranges.lowestVersion() );
        assertEquals( 3, state.ranges.highestVersion() );
    }

    @Test
    public void shouldRecoverStateFromPrunedFiles() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( new File( "v2 " ), new File( "v3 " ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v2 " ) ) ).thenReturn( new Header( 2, 9, 0 ) );
        when( headerReader.readHeader( new File( "v3 " ) ) ).thenReturn( new Header( 3, 19, 0 ) );

        EntryReader entryReader = mock( EntryReader.class );
        when( entryReader.readEntriesInVersion( 3 ) ).thenReturn( cursor() );

        Recovery recovery = new Recovery( versionFiles, headerReader, entryReader );

        // when
        Recovery.LogState state = recovery.recover();

        // then
        assertEquals( 9, state.prevIndex );
        assertEquals( 0, state.prevTerm );
        assertEquals( 19, state.appendIndex );
        assertEquals( 3, state.currentVersion );
        assertEquals( 2, state.ranges.lowestVersion() );
        assertEquals( 3, state.ranges.highestVersion() );
    }
}