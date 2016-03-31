/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.raft.log.physical;

import java.io.File;
import java.util.Collections;

import org.junit.Test;

import org.neo4j.coreedge.raft.log.EntryReader;
import org.neo4j.coreedge.raft.log.PositionAwareRaftLogAppendRecord;
import org.neo4j.coreedge.raft.log.RaftLogAppendRecord;
import org.neo4j.coreedge.raft.log.RaftLogEntry;
import org.neo4j.coreedge.raft.replication.ReplicatedContent;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.neo4j.coreedge.raft.ReplicatedInteger.valueOf;
import static org.neo4j.coreedge.raft.log.physical.HeaderReader.HEADER_LENGTH;
import static org.neo4j.coreedge.raft.log.physical.RecoveryTest.HeaderBuilder.version;
import static org.neo4j.coreedge.raft.log.physical.RecoveryTest.RecordBuilder.offset;
import static org.neo4j.kernel.impl.util.IOCursors.cursor;

public class RecoveryTest
{
    @Test
    public void shouldRecoverStateFromFiles() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( file( 0, "v0", 30 ), file( 1, "v1", 30 ), file( 2, "v2", 30 ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v0" ) ) ).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( headerReader.readHeader( new File( "v1" ) ) ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( version( 2 ).prevIndex( 19 ).prevTerm( 0 ) );

        EntryReader entryReader = mock( EntryReader.class );
        when( entryReader.readEntriesInVersion( 2 ) ).thenReturn( cursor(
                offset( 0, 10 ).index( 20 ).term( 0 ).content( valueOf( 120 ) ),
                offset( 10, 20 ).index( 21 ).term( 0 ).content( valueOf( 121 ) ),
                offset( 20, 30 ).index( 22 ).term( 0 ).content( valueOf( 122 ) )
        ) );

        Recovery recovery = new Recovery( versionFiles, headerReader, entryReader, mock( HeaderWriter.class ), mock(
                LogFileTruncater.class ) );

        // when
        Recovery.LogState state = recovery.recover();

        // then
        assertEquals( -1, state.prevIndex );
        assertEquals( -1, state.prevTerm );
        assertEquals( 22, state.appendIndex );
        assertEquals( 0, state.term );
        assertEquals( 2, state.currentVersion );
        assertEquals( 0, state.ranges.lowestVersion() );
        assertEquals( 2, state.ranges.highestVersion() );
    }

    @Test
    public void shouldRecoverStateFromPrunedFiles() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( file( 2, "v2", 30 ), file( 3, "v3", 0 ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( version( 2 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( headerReader.readHeader( new File( "v3" ) ) ).thenReturn( version( 3 ).prevIndex( 19 ).prevTerm( 0 ) );

        EntryReader entryReader = mock( EntryReader.class );
        when( entryReader.readEntriesInVersion( 3 ) ).thenReturn( cursor() );

        Recovery recovery = new Recovery( versionFiles, headerReader, entryReader, mock( HeaderWriter.class ), mock( LogFileTruncater.class ) );

        // when
        Recovery.LogState state = recovery.recover();

        // then
        assertEquals( 9, state.prevIndex );
        assertEquals( 0, state.prevTerm );
        assertEquals( 19, state.appendIndex );
        assertEquals( 0, state.term );
        assertEquals( 3, state.currentVersion );
        assertEquals( 2, state.ranges.lowestVersion() );
        assertEquals( 3, state.ranges.highestVersion() );
    }

    @Test
    public void shouldCreateNewEmptyFileWhenNoFilesPresent() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() ).thenReturn( Collections.emptyList() );
        when( versionFiles.createNewVersionFile( 0 ) ).thenReturn( file( 0, "v0", 0 ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        HeaderWriter headerWriter = mock( HeaderWriter.class );
        Recovery recovery = new Recovery( versionFiles, headerReader, mock( EntryReader.class ), headerWriter, mock( LogFileTruncater.class ) );

        // when
        Recovery.LogState state = recovery.recover();

        // then
        assertEquals( -1, state.prevIndex );
        assertEquals( -1, state.prevTerm );
        assertEquals( -1, state.appendIndex );
        assertEquals( -1, state.term );
        assertEquals( 0, state.currentVersion );
        assertEquals( 0, state.ranges.lowestVersion() );
        assertEquals( 0, state.ranges.highestVersion() );

        verify( versionFiles ).createNewVersionFile( 0 );
        verify( headerWriter ).write( new File( "v0" ), version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
    }

    @Test
    public void shouldAddHeaderWhenEmptyFilePresent() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( file( 0, "v0", 30 ), file( 1, "v1", 30 ), file( 2, "v2", -HEADER_LENGTH ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v0" ) ) ).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( headerReader.readHeader( new File( "v1" ) ) ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( null );

        EntryReader entryReader = mock( EntryReader.class );
        when( entryReader.readEntriesInVersion( 1 ) ).thenReturn( cursor(
                new PositionAwareRaftLogAppendRecord( 0, 100, new RaftLogAppendRecord( 10, new RaftLogEntry( 0,
                        valueOf( 110 ) ) ) )
        ) );


        HeaderWriter headerWriter = mock( HeaderWriter.class );
        Recovery recovery = new Recovery( versionFiles, headerReader, entryReader, headerWriter, mock( LogFileTruncater.class ) );

        // when
        Recovery.LogState state = recovery.recover();

        // then
        assertEquals( -1, state.prevIndex );
        assertEquals( -1, state.prevTerm );
        assertEquals( 10, state.appendIndex );
        assertEquals( 0, state.term );
        assertEquals( 2, state.currentVersion );
        assertEquals( 0, state.ranges.lowestVersion() );
        assertEquals( 2, state.ranges.highestVersion() );

        verify( headerWriter ).write( new File( "v2" ), version( 2 ).prevIndex( 10 ).prevTerm( 0 ) );
    }

    @Test
    public void shouldFailRecoveryIfThereAreMissingVersionFiles() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( file( 0, "v0", 30 ), file( 1, "v1", 30 ), file( 3, "v3", 30 ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v0" ) ) ).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( headerReader.readHeader( new File( "v1" ) ) ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( headerReader.readHeader( new File( "v3" ) ) ).thenReturn( version( 3 ).prevIndex( 10 ).prevTerm( 0 ) );

        Recovery recovery = new Recovery( versionFiles, headerReader,
                mock( EntryReader.class ), mock( HeaderWriter.class ), mock( LogFileTruncater.class ) );

        // when
        try
        {
            recovery.recover();
            fail();
        }
        catch ( DamagedLogStorageException e )
        {
            assertThat( e.getMessage(), containsString( "Missing expected log file version 2" ) );
        }
    }

    @Test
    public void shouldFailRecoveryIfVersionNumberDoesNotMatchVersionInHeader() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( singletonList( file( 2, "foo", 30 ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "foo" ) ) ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );

        Recovery recovery = new Recovery( versionFiles, headerReader,
                mock( EntryReader.class ), mock( HeaderWriter.class ), mock( LogFileTruncater.class ) );

        // when
        try
        {
            recovery.recover();
            fail();
        }
        catch ( DamagedLogStorageException e )
        {
            assertThat( e.getMessage(),
                    containsString( "Expected file [foo] to contain log version 2, but contained log version 1" ) );
        }
    }

    @Test
    public void shouldFailRecoveryIfAdditionalFilesPresentAfterAnEmptyFile() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( file( 0, "v0", 30 ), file( 1, "v1", 30 ), file( 2, "v2", 30 ), file( 3, "v3", 30 ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v0" ) ) ).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( headerReader.readHeader( new File( "v1" ) ) ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( null );

        Recovery recovery = new Recovery( versionFiles, headerReader,
                mock( EntryReader.class ), mock( HeaderWriter.class ), mock( LogFileTruncater.class ) );

        // when
        try
        {
            recovery.recover();
            fail();
        }
        catch ( DamagedLogStorageException e )
        {
            assertThat( e.getMessage(), containsString(
                    "Found empty file [v2] but there are files with higher version numbers: [3: v3]" ) );
        }
    }

    @Test
    public void shouldTruncatePartiallyWrittenRecords() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( file( 0, "v0", 30 ), file( 1, "v1", 30 ), file( 2, "v2", 31 ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v0" ) ) ).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( headerReader.readHeader( new File( "v1" ) ) ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( version( 2 ).prevIndex( 19 ).prevTerm( 0 ) );

        EntryReader entryReader = mock( EntryReader.class );
        when( entryReader.readEntriesInVersion( 2 ) ).thenReturn( cursor(
                offset( 0, 10 ).index( 20 ).term( 0 ).content( valueOf( 120 ) ),
                offset( 10, 20 ).index( 21 ).term( 0 ).content( valueOf( 121 ) ),
                offset( 20, 30 ).index( 22 ).term( 0 ).content( valueOf( 122 ) )
        ) );

        LogFileTruncater truncater = mock( LogFileTruncater.class );
        Recovery recovery = new Recovery( versionFiles, headerReader, entryReader, mock( HeaderWriter.class ), truncater );

        // when
        recovery.recover();

        // then
        verify( truncater).truncate( new File( "v2" ), 30 + HEADER_LENGTH ) ;
    }


    private static VersionFiles.VersionFile file( long version, String fileName, long contentSize )
    {
        return new VersionFiles.VersionFile( version, new File( fileName ), contentSize + HEADER_LENGTH );

    }

    static class RecordBuilder
    {
        private long startPosition;
        private long endPosition;
        private long index;
        private long term;

        public static RecordBuilder offset( long startPosition, long endPosition )
        {
            return new RecordBuilder( startPosition + HEADER_LENGTH, endPosition + HEADER_LENGTH );
        }

        public RecordBuilder( long startPosition, long endPosition )
        {
            this.startPosition = startPosition;
            this.endPosition = endPosition;
        }

        public RecordBuilder index( long index )
        {
            this.index = index;
            return this;
        }

        public RecordBuilder term( long term )
        {
            this.term = term;
            return this;
        }

        public PositionAwareRaftLogAppendRecord content( ReplicatedContent content )
        {
            return new PositionAwareRaftLogAppendRecord( startPosition, endPosition,
                    new RaftLogAppendRecord( index, new RaftLogEntry( term, content ) ) );
        }
    }

    static class HeaderBuilder
    {
        public long version;
        public long prevIndex;
        public long prevTerm;

        public static HeaderBuilder version(long version)
        {
            return new HeaderBuilder( version );
        }

        public HeaderBuilder( long version )
        {
            this.version = version;
        }

        public HeaderBuilder prevIndex( long prevIndex )
        {
            this.prevIndex = prevIndex;
            return this;
        }

        public Header prevTerm( long prevTerm )
        {
            this.prevTerm = prevTerm;
            return new Header( version, prevIndex, prevTerm );
        }
    }
}