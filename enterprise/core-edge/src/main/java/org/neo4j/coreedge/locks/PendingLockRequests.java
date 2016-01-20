package org.neo4j.coreedge.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.neo4j.kernel.DeadlockDetectedException;

public class PendingLockRequests implements CompletableLockRequests
{
    private final Map<LockSession,PendingLockRequest> map = new HashMap<>();

    public PendingLockRequest register( LockSession session )
    {
        PendingLockRequest request = new PendingLockRequest();
        map.put( session, request );
        return request;
    }

    public Optional<CompletableLockRequest> retrieve( LockSession session )
    {
        return Optional.ofNullable( map.remove( session ) );
    }

    public class PendingLockRequest implements CompletableLockRequest
    {
        public void waitForLock()
        {

        }

        @Override
        public void lockAcquired()
        {

        }

        @Override
        public void lockNotAvailable()
        {

        }

        @Override
        public void failWithDeadlock( DeadlockDetectedException e )
        {

        }
    }
}
