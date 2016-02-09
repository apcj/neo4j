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
package org.neo4j.coreedge.server.core.locks;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import org.neo4j.coreedge.raft.state.ChannelMarshal;
import org.neo4j.coreedge.raft.state.StatePersister;
import org.neo4j.coreedge.raft.state.StateRecoveryManager;
import org.neo4j.coreedge.raft.state.StateStorage;
import org.neo4j.coreedge.server.core.locks.InMemoryReplicatedLockTokenState.InMemoryReplicatedLockStateChannelMarshal;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.internal.DatabaseHealth;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.logging.LogProvider;

public class OnDiskReplicatedLockTokenState<MEMBER> extends LifecycleAdapter implements StateStorage<InMemoryReplicatedLockTokenState<MEMBER>>

{
    public static final String DIRECTORY_NAME = "lock-token-state";
    public static final String FILENAME = "lock-token.";
    private InMemoryReplicatedLockTokenState<MEMBER> initialState;
    private final StatePersister<InMemoryReplicatedLockTokenState<MEMBER>> statePersister;

    public OnDiskReplicatedLockTokenState( FileSystemAbstraction fileSystemAbstraction, File stateDir,
                                           int numberOfEntriesBeforeRotation, ChannelMarshal<MEMBER> channelMarshal,
                                           Supplier<DatabaseHealth> databaseHealthSupplier, LogProvider logProvider )
            throws IOException
    {
        File fileA = new File( stateDir, FILENAME + "a" );
        File fileB = new File( stateDir, FILENAME + "b" );

        InMemoryReplicatedLockStateChannelMarshal<MEMBER> marshal =
                new InMemoryReplicatedLockStateChannelMarshal<>( channelMarshal );

        StateRecoveryManager<InMemoryReplicatedLockTokenState<MEMBER>> recoveryManager =
                new StateRecoveryManager<>( fileSystemAbstraction, marshal );

        StateRecoveryManager.RecoveryStatus recoveryStatus = recoveryManager.recover( fileA, fileB );

        this.initialState = recoveryManager.readLastEntryFrom( recoveryStatus.previouslyActive() );

        this.statePersister = new StatePersister<>( fileA, fileB, fileSystemAbstraction, numberOfEntriesBeforeRotation,
                marshal, recoveryStatus.previouslyInactive(), databaseHealthSupplier );

        logProvider.getLog( getClass() ).info( "State restored, last index is %d",
                initialState.logIndex() );
    }

    @Override
    public InMemoryReplicatedLockTokenState<MEMBER> getInitialState()
    {
        return initialState;
    }

    @Override
    public void persistStoreData( InMemoryReplicatedLockTokenState<MEMBER> memberInMemoryReplicatedLockTokenState )
            throws IOException
    {
        statePersister.persistStoreData( memberInMemoryReplicatedLockTokenState );
    }

    public void close() throws IOException
    {
        statePersister.close();
    }
}
