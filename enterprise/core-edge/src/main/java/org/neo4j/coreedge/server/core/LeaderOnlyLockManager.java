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
package org.neo4j.coreedge.server.core;

import org.neo4j.coreedge.raft.replication.Replicator;
import org.neo4j.coreedge.raft.replication.tx.ReplicatedTransactionStateMachine;
import org.neo4j.coreedge.server.core.CurrentLockToken.LockSession;
import org.neo4j.kernel.impl.locking.LockClientAlreadyClosedException;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.storageengine.api.lock.AcquireLockTimeoutException;
import org.neo4j.storageengine.api.lock.ResourceType;

/**
 * Each member of the cluster uses its own {@link LeaderOnlyLockManager} which wraps a local {@link Locks}.
 * Conflict between the local {@link Locks} on different servers is prevented by replicating a {@link LockToken}.
 * The {@link LockTokenTokenMachine} keeps track of these tokens and considers only one to be valid at a time.
 * Meanwhile, {@link ReplicatedTransactionStateMachine}, fails any transactions that are attempted under an
 * invalid token.
 *
 * Before issuing any locks, {@link LeaderOnlyLockManager} checks whether it is leader, and fails if it isn't.
 * It then tries to replicate a unique {@link LockToken} and waits until that token is valid. This is optimistic,
 * because another server could always replicate its own token at any moment. In case of leader switch,
 * and another {@link LeaderOnlyLockManager} replicating its own {@link LockToken}, in flight transactions will
 * ultimately fail, either when trying to acquire another lock, or when they try to commit.
 */
public class LeaderOnlyLockManager<MEMBER> implements Locks
{
    public static final int LOCK_WAIT_TIME = 30000;

    private final MEMBER myself;

    private final Replicator replicator;
    private final Locks local;
    private final LockTokenTokenMachine replicatedLockTokenStateMachine;

    public LeaderOnlyLockManager( MEMBER myself, Replicator replicator, Locks local, LockTokenTokenMachine replicatedLockTokenStateMachine )
    {
        this.myself = myself;
        this.replicator = replicator;
        this.local = local;
        this.replicatedLockTokenStateMachine = replicatedLockTokenStateMachine;
    }

    @Override
    public synchronized Client newClient()
    {
        return new LeaderOnlyLockClient( local.newClient(), replicatedLockTokenStateMachine.currentLockSession() );
    }

    private void waitForToken() throws InterruptedException
    {
        // TODO: Don't even try if we are not the leader.

        try
        {
            replicator.replicate( new LockToken<>( myself, replicatedLockTokenStateMachine.nextId() ));
        }
        catch ( Replicator.ReplicationFailedException e )
        {
            throw new LockClientAlreadyClosedException( "Could not acquire lock session. Leader switch?" );
        }

        synchronized ( replicatedLockTokenStateMachine )
        {
            replicatedLockTokenStateMachine.wait( LOCK_WAIT_TIME );
        }
    }

    @Override
    public void accept( Visitor visitor )
    {
        local.accept( visitor );
    }

    @Override
    public void close()
    {
        local.close();
    }

    boolean isMine(LockToken<MEMBER> lockToken)
    {
        return myself.equals( lockToken.owner );
    }

    private class LeaderOnlyLockClient implements Client
    {
        private final Client localLocks;
        private LockToken<MEMBER> lockToken;
        boolean sessionStarted = false;

        public LeaderOnlyLockClient( Client localLocks, LockToken<MEMBER> lockToken )
        {
            this.localLocks = localLocks;
            this.lockToken = lockToken;
        }

        private void ensureHoldingReplicatedLock()
        {
            if ( !sessionStarted )
            {
                if ( !lockToken.isMine() )
                {
                    try
                    {
                        waitForToken();
                    }
                    catch ( InterruptedException e )
                    {
                        throw new RuntimeException( "Interrupted " );
                    }
                }

                lockToken = replicatedLockStateMachine.currentLockSession();

                if( !lockToken.isMine() )
                {
                    throw new RuntimeException( "Did not manage to acquire valid lock session ID. " + lockToken );
                }

                sessionStarted = true;
            }

            if( !replicatedLockStateMachine.currentLockSession().isMine() )
            {
                throw new RuntimeException( "Local instance lost lock session." );
            }
        }

        @Override
        public void acquireShared( ResourceType resourceType, long resourceId ) throws AcquireLockTimeoutException
        {
            localLocks.acquireShared( resourceType, resourceId );
        }

        @Override
        public void acquireExclusive( ResourceType resourceType, long resourceId ) throws AcquireLockTimeoutException
        {
            ensureHoldingReplicatedLock();
            localLocks.acquireExclusive( resourceType, resourceId );
        }

        @Override
        public boolean tryExclusiveLock( ResourceType resourceType, long resourceId )
        {
            ensureHoldingReplicatedLock();
            return localLocks.tryExclusiveLock( resourceType, resourceId );
        }

        @Override
        public boolean trySharedLock( ResourceType resourceType, long resourceId )
        {
            return localLocks.trySharedLock( resourceType, resourceId );
        }

        @Override
        public void releaseShared( ResourceType resourceType, long resourceId )
        {
            localLocks.releaseShared( resourceType, resourceId );
        }

        @Override
        public void releaseExclusive( ResourceType resourceType, long resourceId )
        {
            localLocks.releaseExclusive( resourceType, resourceId );
        }

        @Override
        public void releaseAll()
        {
            localLocks.releaseAll();
        }

        @Override
        public void stop()
        {
            localLocks.stop();
        }

        @Override
        public void close()
        {
            localLocks.close();
        }

        @Override
        public int getLockSessionId()
        {
            return lockToken.id();
        }
    }
}
