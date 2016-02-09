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
package org.neo4j.coreedge.raft.state;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import org.junit.Rule;
import org.junit.Test;

import org.neo4j.coreedge.raft.state.term.InMemoryTermState;
import org.neo4j.coreedge.raft.state.term.OnDiskTermState;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.fs.StoreFileChannel;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.test.TargetDirectory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OnDiskTermStateTest
{
    @Rule
    public TargetDirectory.TestDirectory testDir = TargetDirectory.testDirForTest( getClass() );

    @Test
    public void shouldRoundtripTermState() throws Exception
    {
        // given
        EphemeralFileSystemAbstraction fsa = new EphemeralFileSystemAbstraction();
        fsa.mkdir( testDir.directory() );

        OnDiskTermState storage =
                new OnDiskTermState( fsa, testDir.directory(), 100, mock( Supplier.class ), NullLogProvider.getInstance() );
        InMemoryTermState oldOnDiskTermState = storage.getInitialState();
        oldOnDiskTermState.update( 99 );
        storage.persistStoreData( oldOnDiskTermState );

        // when
        InMemoryTermState newOnDiskTermState = new OnDiskTermState( fsa, testDir.directory(), 100, mock( Supplier.class ),
                NullLogProvider.getInstance() ).getInitialState();

        // then
        assertEquals( oldOnDiskTermState.currentTerm(), newOnDiskTermState.currentTerm() );
    }

    @Test
    public void shouldCallWriteAllAndForceOnVoteUpdate() throws Exception
    {
        // Given
        StoreFileChannel channel = newFileChannelMock();
        FileSystemAbstraction fsa = newFileSystemMock( channel );

        OnDiskTermState storage = new OnDiskTermState( fsa, testDir.directory(), 100, mock( Supplier.class ),
                NullLogProvider.getInstance() );

        // When
        InMemoryTermState state = storage.getInitialState();
        state.update( 100L );
        storage.persistStoreData( state );

        // Then
        verify( channel ).writeAll( any( ByteBuffer.class ) );
        verify( channel ).flush();
    }

    @Test
    public void shouldFlushAndCloseOnShutdown() throws Throwable
    {
        // Given
        StoreFileChannel channel = newFileChannelMock();
        FileSystemAbstraction fsa = newFileSystemMock( channel );

        OnDiskTermState store = new OnDiskTermState( fsa, testDir.directory(), 100, mock( Supplier.class ),
                NullLogProvider.getInstance() );

        // When
        // We shut it down
        store.shutdown();

        // Then
        verify( channel ).flush();
        verify( channel ).close();
    }

    private static StoreFileChannel newFileChannelMock() throws IOException
    {
        StoreFileChannel channel = mock( StoreFileChannel.class );
        when( channel.read( any( ByteBuffer.class ) ) ).thenReturn( -1 );
        return channel;
    }

    private static FileSystemAbstraction newFileSystemMock( StoreFileChannel channel ) throws IOException
    {
        FileSystemAbstraction fsa = mock( FileSystemAbstraction.class );
        when( fsa.open( any( File.class ), anyString() ) ).thenReturn( channel );
        when( fsa.fileExists( any( File.class ) ) ).thenReturn( true );
        return fsa;
    }
}
