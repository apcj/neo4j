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
import java.util.function.Supplier;

import org.junit.Test;

import org.neo4j.coreedge.raft.log.RaftStorageException;
import org.neo4j.coreedge.raft.state.term.InMemoryTermState;
import org.neo4j.coreedge.raft.state.term.OnDiskTermState;
import org.neo4j.coreedge.raft.state.term.TermState;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.logging.NullLogProvider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class TermStateDurabilityTest
{
    public OnDiskTermState createTermStore( EphemeralFileSystemAbstraction fileSystem ) throws IOException
    {
        File directory = new File( "raft-log" );
        fileSystem.mkdir( directory );
        return new OnDiskTermState( fileSystem, directory, 100, mock( Supplier.class ), NullLogProvider.getInstance() );
    }

    @Test
    public void shouldStoreTerm() throws Exception
    {
        EphemeralFileSystemAbstraction fileSystem = new EphemeralFileSystemAbstraction();
        OnDiskTermState storage = createTermStore( fileSystem );

        InMemoryTermState termState = storage.getInitialState();
        termState.update( 23 );
        storage.persistStoreData( termState );

        verifyCurrentLogAndNewLogLoadedFromFileSystem( termState, fileSystem,
                termState1 -> assertEquals( 23, termState1.currentTerm() ) );
    }

    @Test
    public void emptyFileShouldImplyZeroTerm() throws Exception
    {
        EphemeralFileSystemAbstraction fileSystem = new EphemeralFileSystemAbstraction();
        TermState termState = createTermStore( fileSystem ).getInitialState();

        verifyCurrentLogAndNewLogLoadedFromFileSystem( termState, fileSystem,
                termState1 -> assertEquals( 0, termState1.currentTerm() ) );
    }

    private void verifyCurrentLogAndNewLogLoadedFromFileSystem( TermState termState,
                                                                EphemeralFileSystemAbstraction fileSystem,
                                                                TermVerifier termVerifier ) throws RaftStorageException, IOException
    {
        termVerifier.verifyTerm( termState );
        termVerifier.verifyTerm( createTermStore( fileSystem ).getInitialState() );
        fileSystem.crash();
        termVerifier.verifyTerm( createTermStore( fileSystem ).getInitialState() );
    }

    private interface TermVerifier
    {
        void verifyTerm( TermState termState ) throws RaftStorageException;
    }
}
