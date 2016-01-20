package org.neo4j.coreedge.locks;

import org.neo4j.coreedge.raft.replication.Replicator;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.kernel.impl.locking.community.LockResource;
import org.neo4j.storageengine.api.lock.AcquireLockTimeoutException;
import org.neo4j.storageengine.api.lock.ResourceType;

import static org.neo4j.coreedge.locks.LockOperation.ACQUIRE;
import static org.neo4j.coreedge.locks.LockType.EXCLUSIVE;

public class DistributedLockClient implements Locks.Client
{
    private final PendingLockRequests lockRequests;
    private final Replicator replicator;
    private final LockSession lockSession;

    public DistributedLockClient( PendingLockRequests lockRequests, Replicator replicator )
    {
        this.lockRequests = lockRequests;
        this.replicator = replicator;
        this.lockSession = new LockSession();
    }

    @Override
    public void acquireExclusive( ResourceType resourceType, long resourceId ) throws AcquireLockTimeoutException
    {
        PendingLockRequests.PendingLockRequest pendingLockRequest = lockRequests.register( lockSession );
        try
        {
            replicator.replicate( new LockRequest( ACQUIRE, EXCLUSIVE, new LockResource( resourceType, resourceId ), lockSession ) );
        }
        catch ( Replicator.ReplicationFailedException e )
        {
            throw new AcquireLockTimeoutException( e, "Unable to replicate lock request" );
        }
        pendingLockRequest.waitForLock();
    }

    @Override
    public void acquireShared( ResourceType resourceType, long resourceId ) throws AcquireLockTimeoutException
    {

    }

    @Override public boolean tryExclusiveLock( ResourceType resourceType, long resourceId )
    {
        return false;
    }

    @Override public boolean trySharedLock( ResourceType resourceType, long resourceId )
    {
        return false;
    }

    @Override public void releaseShared( ResourceType resourceType, long resourceId )
    {

    }

    @Override public void releaseExclusive( ResourceType resourceType, long resourceId )
    {

    }

    @Override public void releaseAll()
    {

    }

    @Override public void stop()
    {

    }

    @Override public void close()
    {

    }

    @Override public int getLockSessionId()
    {
        return 0;
    }
}
