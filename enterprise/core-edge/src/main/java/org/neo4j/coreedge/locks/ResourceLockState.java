package org.neo4j.coreedge.locks;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The state of locks for a give lockable resource, consisting of:
 * <ul>
 * <li>The set of sessions who are currently holding a lock on this resource, together with counts
 * of how many shared and exclusive locks that session holds.
 * If any of these sessions hold an exclusive lock, then this set must have only one member.</li>
 * <li>Queue of sessions that are waiting to acquire a lock on the resource.</li>
 * </ul>
 */
public class ResourceLockState
{
    private final Map<LockSession, LockHolding> currentHolders = new HashMap<>();
    private final Queue<LockSession> waitingSessions = new LinkedList<>();

    public boolean availableFor( LockType lockType, LockSession session )
    {
        if ( lockType == LockType.EXCLUSIVE )
        {
            return currentHolders.isEmpty() || lockedOnlyBySession( session );
        }
        return !lockedExclusivelyByAnotherSession( session );
    }

    private boolean lockedOnlyBySession( LockSession session )
    {
        return currentHolders.size() == 1 && currentHolders.containsKey( session );
    }

    private boolean lockedExclusivelyByAnotherSession( LockSession session )
    {
        for ( Map.Entry<LockSession, LockHolding> entry : currentHolders.entrySet() )
        {
            if ( entry.getValue().lockType == LockType.EXCLUSIVE )
            {
                return !session.equals( entry.getKey() );
            }
        }
        return false;
    }

    public boolean hasAnyLock( LockSession session )
    {
        return currentHolders.containsKey( session );
    }

    public void issue( LockSession session, LockType lockType )
    {
        LockHolding holding = currentHolders.get( session );
        if ( holding == null )
        {
            holding = new LockHolding( lockType );
            currentHolders.put( session, holding );
        }
        holding.counter( lockType ).incrementAndGet();
    }

    public void release( LockSession session, LockType lockType )
    {
        LockHolding holding = currentHolders.get( session );
        holding.counter( lockType ).decrementAndGet();
        if ( holding.isFree() )
        {
            currentHolders.remove( session );
        }
    }

    public void enqueue( LockSession lockSession )
    {
        waitingSessions.add( lockSession );
    }

    public LockSession poll()
    {
        return waitingSessions.poll();
    }

    private static class LockHolding
    {
        private LockType lockType;
        private AtomicInteger sharedCount = new AtomicInteger( 0 );
        private AtomicInteger exclusiveCount = new AtomicInteger( 0 );

        public LockHolding( LockType lockType )
        {
            this.lockType = lockType;
        }

        private AtomicInteger counter( LockType lockType )
        {
            return lockType == LockType.EXCLUSIVE ? exclusiveCount : sharedCount;
        }

        public boolean isFree()
        {
            return (exclusiveCount.intValue() == 0) && (sharedCount.intValue() == 0);
        }

    }
}
