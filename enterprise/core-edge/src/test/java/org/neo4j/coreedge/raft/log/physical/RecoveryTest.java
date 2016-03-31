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
        VersionFiles.VersionFile v0 = file( 0, "v0", 30 );
        VersionFiles.VersionFile v1 = file( 1, "v1", 30 );
        VersionFiles.VersionFile v2 = file( 2, "v2", 30 );
        when( versionFiles.filesInVersionOrder() ).thenReturn( asList( v0, v1, v2 ) );

        when( v0.header() ).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( v1.header() ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( v2.header() ).thenReturn( version( 2 ).prevIndex( 19 ).prevTerm( 0 ) );

        when( v2.readEntries(  ) ).thenReturn( cursor(
                offset( 0, 10 ).index( 20 ).term( 0 ).content( valueOf( 120 ) ),
                offset( 10, 20 ).index( 21 ).term( 0 ).content( valueOf( 121 ) ),
                offset( 20, 30 ).index( 22 ).term( 0 ).content( valueOf( 122 ) )
        ) );

        Recovery recovery = new Recovery( versionFiles );

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
        VersionFiles.VersionFile v2 = file( 2, "v2", 30 );
        VersionFiles.VersionFile v3 = file( 3, "v3", 0 );
        when( versionFiles.filesInVersionOrder() ).thenReturn( asList( v2, v3 ) );

        when( v2.header()).thenReturn( version( 2 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( v3.header()).thenReturn( version( 3 ).prevIndex( 19 ).prevTerm( 0 ) );

        when( v3.readEntries() ).thenReturn( cursor() );

        Recovery recovery = new Recovery( versionFiles );

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
        VersionFiles.VersionFile v0 = file( 0, "v0", 0 );
        when( versionFiles.createNewVersionFile( 0 ) ).thenReturn( v0 );

        Recovery recovery = new Recovery( versionFiles );

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
        verify( v0 ).writeHeader( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
    }

    @Test
    public void shouldAddHeaderWhenEmptyFilePresent() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        VersionFiles.VersionFile v0 = file( 0, "v0", 30 );
        VersionFiles.VersionFile v1 = file( 1, "v1", 30 );
        VersionFiles.VersionFile v2 = file( 2, "v2", -HEADER_LENGTH );
        when( versionFiles.filesInVersionOrder() ).thenReturn( asList( v0, v1, v2 ) );

        when( v0.header()).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( v1.header()).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( v2.header()).thenReturn( null );

        when( v1.readEntries() ).thenReturn( cursor(
                new PositionAwareRaftLogAppendRecord( 0, 100,
                        new RaftLogAppendRecord( 10, new RaftLogEntry( 0, valueOf( 110 ) ) ) )
        ) );

        Recovery recovery = new Recovery( versionFiles );

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

        verify( v2 ).writeHeader( version( 2 ).prevIndex( 10 ).prevTerm( 0 ) );
    }

    @Test
    public void shouldFailRecoveryIfThereAreMissingVersionFiles() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        VersionFiles.VersionFile v0 = file( 0, "v0", 30 );
        VersionFiles.VersionFile v1 = file( 1, "v1", 30 );
        VersionFiles.VersionFile v3 = file( 3, "v3", 30 );
        when( versionFiles.filesInVersionOrder() ).thenReturn( asList( v0, v1, v3 ) );

        when( v0.header() ).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( v1.header() ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( v3.header() ).thenReturn( version( 3 ).prevIndex( 10 ).prevTerm( 0 ) );

        Recovery recovery = new Recovery( versionFiles );

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
        VersionFiles.VersionFile foo = file( 2, "foo", 30 );
        when( versionFiles.filesInVersionOrder() ).thenReturn( singletonList( foo ) );

        when( foo.header( )).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );

        Recovery recovery = new Recovery( versionFiles
        );

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
        VersionFiles.VersionFile v0 = file( 0, "v0", 30 );
        VersionFiles.VersionFile v1 = file( 1, "v1", 30 );
        VersionFiles.VersionFile v2 = file( 2, "v2", 30 );
        VersionFiles.VersionFile v3 = file( 3, "v3", 30 );
        when( versionFiles.filesInVersionOrder() ).thenReturn( asList( v0, v1, v2, v3 ) );

        when( v0.header() ).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( v1.header() ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( v2.header() ).thenReturn( null );

        Recovery recovery = new Recovery( versionFiles
        );

        // when
        try
        {
            recovery.recover();
            fail();
        }
        catch ( DamagedLogStorageException e )
        {
            assertThat( e.getMessage(), containsString(
                    "Found empty file [v2] but there are files with higher version numbers" ) );
        }
    }

    @Test
    public void shouldTruncatePartiallyWrittenRecords() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        VersionFiles.VersionFile v0 = file( 0, "v0", 30 );
        VersionFiles.VersionFile v1 = file( 1, "v1", 30 );
        VersionFiles.VersionFile v2 = file( 2, "v2", 31 );
        when( versionFiles.filesInVersionOrder() ).thenReturn( asList( v0, v1, v2 ) );

        when( v0.header() ).thenReturn( version( 0 ).prevIndex( -1 ).prevTerm( -1 ) );
        when( v1.header() ).thenReturn( version( 1 ).prevIndex( 9 ).prevTerm( 0 ) );
        when( v2.header() ).thenReturn( version( 2 ).prevIndex( 19 ).prevTerm( 0 ) );

        when( v2.readEntries() ).thenReturn( cursor(
                offset( 0, 10 ).index( 20 ).term( 0 ).content( valueOf( 120 ) ),
                offset( 10, 20 ).index( 21 ).term( 0 ).content( valueOf( 121 ) ),
                offset( 20, 30 ).index( 22 ).term( 0 ).content( valueOf( 122 ) )
        ) );

        Recovery recovery = new Recovery( versionFiles );

        // when
        recovery.recover();

        // then
        verify( v2 ).truncate( 30 + HEADER_LENGTH );
    }


    private static VersionFiles.VersionFile file( long version, String fileName, long contentSize )
    {
        VersionFiles.VersionFile versionFile = mock(VersionFiles.VersionFile.class);

        when(versionFile.version()).thenReturn( version );
        when(versionFile.file()).thenReturn( new File( fileName ) ) ;
        when(versionFile.size()).thenReturn( contentSize + HEADER_LENGTH );

        return versionFile;

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