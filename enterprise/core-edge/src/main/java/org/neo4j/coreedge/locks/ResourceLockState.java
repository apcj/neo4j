package org.neo4j.coreedge.locks;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * The state of locks for a give lockable resource, consisting of:
 * <ul>
 * <li>The set of sessions who are currently holding a lock on this resource, together with counts
 * of how many shared and exclusive locks that session holds.
 * If any of these sessions hold an exclusive lock, then this set must have only one member</li>
 * <li>Queue of sessions that are waiting to acquire a lock on the resource.</li>
 * </ul>
 */
public class ResourceLockState
{
    private Map<LockSession, LockHolding> currentHolders = new HashMap<>();
    private final Queue<LockSession> waitingSessions = new LinkedList<>();

    public boolean availableFor( LockType lockType, LockSession session )
    {
        if ( currentHolders.isEmpty() )
        {
            return true;
        }
        if ( lockType == LockType.EXCLUSIVE )
        {
            return currentHolders.size() == 1 && currentHolders.containsKey( session );
        }
        for ( LockHolding currentHolder : currentHolders.values() )
        {
            if ( currentHolder.lockType == LockType.EXCLUSIVE )
            {
                return false;
            }
        }
        return true;
    }

    public void issue( LockSession session, LockType lockType )
    {
        LockHolding holding = currentHolders.get( session );
        if ( holding == null )
        {
            holding = new LockHolding( session, lockType );
            currentHolders.put( session, holding );
        }
        holding.increment( lockType );
    }

    public LockSession poll()
    {
        return waitingSessions.poll();
    }

    public void release( LockSession session, LockType lockType )
    {
        LockHolding holding = currentHolders.get( session );
        holding.decrement( lockType );
        if ( holding.isFree() )
        {
            currentHolders.remove( session );
        }
    }

    public boolean hasAnyLock( LockSession session )
    {
        return currentHolders.containsKey( session );
    }

    private static class LockHolding
    {
        private LockSession lockSession;
        private LockType lockType;
        private int sharedCount;
        private int exclusiveCount;

        public LockHolding( LockSession lockSession, LockType lockType )
        {
            this.lockSession = lockSession;
            this.lockType = lockType;
        }

        public void increment( LockType lockType )
        {
            switch ( lockType )
            {
                case SHARED:
                    sharedCount++;
                    break;

                case EXCLUSIVE:
                    exclusiveCount++;
                    break;
            }
        }

        public void decrement( LockType lockType )
        {
            switch ( lockType )
            {
                case SHARED:
                    sharedCount--;
                    break;

                case EXCLUSIVE:
                    exclusiveCount--;
                    break;
            }
        }

        public boolean isFree()
        {
            return (exclusiveCount == 0) && (sharedCount == 0);
        }
    }

    public void enqueue( LockSession lockSession )
    {
        waitingSessions.add( lockSession );
    }
}
