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
import org.neo4j.coreedge.raft.log.RaftLogAppendRecord;
import org.neo4j.coreedge.raft.log.RaftLogEntry;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
                .thenReturn( asList( file( 0, "v0" ), file( 1, "v1" ), file( 2, "v2" ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v0" ) ) ).thenReturn( new Header( 0, -1, -1 ) );
        when( headerReader.readHeader( new File( "v1" ) ) ).thenReturn( new Header( 1, 9, 0 ) );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( new Header( 2, 19, 0 ) );

        EntryReader entryReader = mock( EntryReader.class );
        when( entryReader.readEntriesInVersion( 2 ) ).thenReturn( cursor(
                new RaftLogAppendRecord( 20, new RaftLogEntry( 0, valueOf( 120 ) ) ),
                new RaftLogAppendRecord( 21, new RaftLogEntry( 0, valueOf( 121 ) ) ),
                new RaftLogAppendRecord( 22, new RaftLogEntry( 0, valueOf( 122 ) ) )
        ) );

        Recovery recovery = new Recovery( versionFiles, headerReader, entryReader, mock( HeaderWriter.class ) );

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
                .thenReturn( asList( file( 2, "v2" ), file( 3, "v3" ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( new Header( 2, 9, 0 ) );
        when( headerReader.readHeader( new File( "v3" ) ) ).thenReturn( new Header( 3, 19, 0 ) );

        EntryReader entryReader = mock( EntryReader.class );
        when( entryReader.readEntriesInVersion( 3 ) ).thenReturn( cursor() );

        Recovery recovery = new Recovery( versionFiles, headerReader, entryReader, mock( HeaderWriter.class ) );

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
        when( versionFiles.createNewVersionFile( 0 ) ).thenReturn( file( 0, "v0" ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        HeaderWriter headerWriter = mock( HeaderWriter.class );
        Recovery recovery = new Recovery( versionFiles, headerReader, mock( EntryReader.class ), headerWriter );

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
        verify( headerWriter ).write( new File( "v0" ), new Header( 0, -1, -1 ) );
    }

    @Test
    public void shouldAddHeaderWhenEmptyFilePresent() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( file( 0, "v0" ), file( 1, "v1" ), file( 2, "v2" ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v0" ) ) ).thenReturn( new Header( 0, -1, -1 ) );
        when( headerReader.readHeader( new File( "v1" ) ) ).thenReturn( new Header( 1, 9, 0 ) );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( null );

        EntryReader entryReader = mock( EntryReader.class );
        when( entryReader.readEntriesInVersion( 1 ) ).thenReturn( cursor(
                new RaftLogAppendRecord( 10, new RaftLogEntry( 0, valueOf( 110 ) ) )
        ) );


        HeaderWriter headerWriter = mock( HeaderWriter.class );
        Recovery recovery = new Recovery( versionFiles, headerReader, entryReader, headerWriter );

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

        verify( headerWriter ).write( new File( "v2" ), new Header( 2, 10, 0 ) );
    }

    @Test
    public void shouldFailRecoveryIfThereAreMissingVersionFiles() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( asList( file( 0, "v0" ), file( 1, "v1" ), file( 3, "v3" ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v0" ) ) ).thenReturn( new Header( 0, -1, -1 ) );
        when( headerReader.readHeader( new File( "v1" ) ) ).thenReturn( new Header( 1, 9, 0 ) );
        when( headerReader.readHeader( new File( "v3" ) ) ).thenReturn( new Header( 3, 10, 0 ) );

        Recovery recovery = new Recovery( versionFiles, headerReader,
                mock( EntryReader.class ), mock( HeaderWriter.class ) );

        // when
        try
        {
            recovery.recover();
            fail();
        }
        catch ( DamagedLogStorageException e )
        {
            System.out.println( "e.getMessage() = " + e.getMessage() );
            assertThat( e.getMessage(), containsString( "Missing expected log file version 2" ) );
        }
    }

    @Test
    public void shouldFailRecoveryIfVersionNumberDoesNotMatchVersionInHeader() throws Exception
    {
        // given
        VersionFiles versionFiles = mock( VersionFiles.class );
        when( versionFiles.filesInVersionOrder() )
                .thenReturn( singletonList( file( 2, "foo" ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "foo" ) ) ).thenReturn( new Header( 1, 9, 0 ) );

        Recovery recovery = new Recovery( versionFiles, headerReader, 
                mock( EntryReader.class ), mock( HeaderWriter.class ) );

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
                .thenReturn( asList( file( 0, "v0" ), file( 1, "v1" ), file( 2, "v2" ), file( 3, "v3" ) ) );

        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v0" ) ) ).thenReturn( new Header( 0, -1, -1 ) );
        when( headerReader.readHeader( new File( "v1" ) ) ).thenReturn( new Header( 1, 9, 0 ) );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( null );

        Recovery recovery = new Recovery( versionFiles, headerReader,
                mock( EntryReader.class ), mock( HeaderWriter.class ) );

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
    
    private static VersionFiles.VersionFile file( long version, String fileName )
    {
        return new VersionFiles.VersionFile( version, new File(fileName) );
        
    }
}