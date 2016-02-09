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
package org.neo4j.coreedge.raft.state.id_allocation;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Supplier;

import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.kernel.impl.store.id.IdType;
import org.neo4j.io.fs.StoreChannel;
import org.neo4j.kernel.internal.DatabaseHealth;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.test.TargetDirectory;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.neo4j.coreedge.raft.state.id_allocation.InMemoryIdAllocationState
        .InMemoryIdAllocationStateChannelMarshal.NUMBER_OF_BYTES_PER_WRITE;
import static org.neo4j.coreedge.raft.state.id_allocation.OnDiskIdAllocationStateStorage.FILENAME;

public class OnDiskIdAllocationStateStorageTest
{
    IdType someType = IdType.NODE;

    @Rule
    public TargetDirectory.TestDirectory testDir = TargetDirectory.testDirForTest( getClass() );

    @Test
    public void shouldRoundtripIdAllocationState() throws Exception
    {
        // given
        EphemeralFileSystemAbstraction fsa = new EphemeralFileSystemAbstraction();
        fsa.mkdir( testDir.directory() );

        OnDiskIdAllocationStateStorage store = new OnDiskIdAllocationStateStorage( fsa, testDir.directory(), 1,
                mock( Supplier.class ), NullLogProvider.getInstance() );

        InMemoryIdAllocationState state = store.getInitialState();
        state.firstUnallocated( someType, 1024 );
        state.logIndex( 1 );
        store.persistStoreData( state );

        // when
        OnDiskIdAllocationStateStorage restoredOne = new OnDiskIdAllocationStateStorage( fsa, testDir
                .directory(), 1, mock( Supplier.class ), NullLogProvider.getInstance() );

        // then
        assertEquals( 1024, restoredOne.getInitialState().firstUnallocated( someType ) );
    }

    @Test
    public void shouldAppendEntriesToStoreFile() throws Exception
    {
        // given
        EphemeralFileSystemAbstraction fsa = new EphemeralFileSystemAbstraction();
        fsa.mkdir( testDir.directory() );

        OnDiskIdAllocationStateStorage store = new OnDiskIdAllocationStateStorage( fsa, testDir.directory(), 100,
                mock( Supplier.class ), NullLogProvider.getInstance() );

        InMemoryIdAllocationState state = store.getInitialState();

        final int numberOfAllocationsToAppend = 3;

        // when
        for ( int i = 1; i <= numberOfAllocationsToAppend; i++ )
        {
            state.firstUnallocated( someType, 1024 * i );
            state.logIndex( i );
            store.persistStoreData( state );
        }

        // then
        assertEquals( numberOfAllocationsToAppend * NUMBER_OF_BYTES_PER_WRITE, fsa.getFileSize( stateFileA() ) );
    }

    @Test
    public void shouldRestoreLastStateOutOfManyWrittenOnSingleFile() throws Exception
    {
        // given
        final int numberOfEntiesBeforeSwitchingFiles = 10;
        EphemeralFileSystemAbstraction fsa = new EphemeralFileSystemAbstraction();
        fsa.mkdir( testDir.directory() );

        OnDiskIdAllocationStateStorage store = new OnDiskIdAllocationStateStorage( fsa, testDir.directory(),
                numberOfEntiesBeforeSwitchingFiles, mock( Supplier.class ), NullLogProvider.getInstance() );

        InMemoryIdAllocationState state = store.getInitialState();

        // and a store that has two entries in the store file
        for ( int i = 1; i < 3; i++ )
        {
            state.lastIdRangeLength( someType, i * 1024 );
            state.logIndex( i );
            store.persistStoreData( state );
        }

        // then
        // we restore it and see the last of the two entries
        InMemoryIdAllocationState restoredOne = new OnDiskIdAllocationStateStorage( fsa, testDir.directory(), 1,
                mock( Supplier.class ), NullLogProvider.getInstance() ).getInitialState();

        assertEquals( 1024 * 2, restoredOne.lastIdRangeLength( someType ) );
        assertEquals( 2, restoredOne.logIndex() );
    }

    @Test
    public void shouldSwitchToWritingToPreviouslyInactiveFileOnRecovery() throws Exception
    {
        // given
        EphemeralFileSystemAbstraction fsa = new EphemeralFileSystemAbstraction();
        fsa.mkdir( testDir.directory() );

        OnDiskIdAllocationStateStorage store = new OnDiskIdAllocationStateStorage( fsa, testDir.directory(), 10,
                mock( Supplier.class ), NullLogProvider.getInstance() );

        InMemoryIdAllocationState state = store.getInitialState();

        for ( int i = 1; i <= 3; i++ )
        {
            state.firstUnallocated( someType, 1024 * i );
            state.logIndex( i );
            store.persistStoreData( state );
        }

        // when
        store = new OnDiskIdAllocationStateStorage( fsa, testDir.directory(), 10, mock( Supplier.class ),
                NullLogProvider.getInstance() );

        state = store.getInitialState();
        state.firstUnallocated( someType, 1024 * 4 );
        state.logIndex( 4 );
        store.persistStoreData( state );

        // then
        assertEquals( 3 * NUMBER_OF_BYTES_PER_WRITE,
                fsa.getFileSize( new File( testDir.directory(), "id.allocation.a" ) ) );


        assertEquals( NUMBER_OF_BYTES_PER_WRITE,
                fsa.getFileSize( new File( testDir.directory(), "id.allocation.b" ) ) );
    }

    @Test
    public void shouldPanicAndThrowExceptionIfCannotPersistToDisk() throws Exception
    {
        // given
        EphemeralFileSystemAbstraction fsa = new ExplodingFileSystemAbstraction();
        fsa.mkdir( testDir.directory() );

        final DatabaseHealth databaseHealth = mock( DatabaseHealth.class );
        Supplier<DatabaseHealth> supplier = () -> databaseHealth;

        OnDiskIdAllocationStateStorage store = new OnDiskIdAllocationStateStorage( fsa, testDir.directory(), 100, supplier,
                NullLogProvider.getInstance() );

        // when
        try
        {
            store.persistStoreData( new InMemoryIdAllocationState() );
            // then
            fail( "should have thrown exception" );
        }
        catch ( IOException e )
        {
            // expected
        }

        verify( databaseHealth ).panic( any( Throwable.class ) );
    }

    private class ExplodingFileSystemAbstraction extends EphemeralFileSystemAbstraction
    {
        @Override
        public synchronized StoreChannel open( File fileName, String mode ) throws IOException
        {
            final StoreChannel mock = mock( StoreChannel.class );
            when( mock.read( any( ByteBuffer.class ) ) ).thenReturn( -1 );
            doThrow( new IOException( "boom!" ) ).when( mock ).flush();
            return mock;
        }
    }

    private File stateFileA()
    {
        return new File( testDir.directory(), FILENAME + "a" );
    }
}
