package org.neo4j.coreedge.locks;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.coreedge.locks.CompletableLockRequests.CompletableLockRequest;
import org.neo4j.coreedge.raft.replication.ReplicatedContent;
import org.neo4j.coreedge.raft.replication.Replicator;
import org.neo4j.kernel.DeadlockDetectedException;
import org.neo4j.kernel.impl.locking.community.LockResource;
import org.neo4j.kernel.impl.locking.community.RagManager;

public class DistributedLockStateMachine implements Replicator.ReplicatedContentListener
{
    private final Map<LockResource, ResourceLockState> resourceStates = new HashMap<>();
    private final RagManager ragManager = new RagManager();
    private final CompletableLockRequests lockRequests;

    public DistributedLockStateMachine( CompletableLockRequests lockRequests )
    {
        this.lockRequests = lockRequests;
    }

    @Override
    public void onReplicated( ReplicatedContent content, long logIndex )
    {
        if ( content instanceof LockRequest )
        {
            LockRequest request = (LockRequest) content;
            if ( request.lockOperation() == LockOperation.ACQUIRE )
            {
                tryToAcquireLock( request );
            }
            else if ( request.lockOperation() == LockOperation.RELEASE )
            {
                releaseLock( request );
            }
        }
    }

    private void tryToAcquireLock( LockRequest lockRequest )
    {
        LockResource resource = lockRequest.resource();
        LockSession session = lockRequest.session();

        ResourceLockState lockState = resourceStates.get( resource );
        if ( lockState == null )
        {
            lockState = new ResourceLockState();
            resourceStates.put( resource, lockState );
        }

        if ( lockState.availableFor( lockRequest.lockType(), session ) )
        {
            if ( !lockState.hasAnyLock( session ) )
            {
                ragManager.lockAcquired( resource, session );
            }
            lockState.issue( session, lockRequest.lockType() );

            lockRequests.retrieve( session )
                    .ifPresent( CompletableLockRequest::lockAcquired );
        }
        else
        {
            try
            {
                ragManager.checkWaitOn( resource, session );
            }
            catch ( DeadlockDetectedException e )
            {
                lockRequests.retrieve( session )
                        .ifPresent( pendingLockRequest -> pendingLockRequest.failWithDeadlock( e ) );
                return;
            }
            lockState.enqueue( session );
        }
    }

    private void releaseLock( LockRequest lockRequest )
    {
        LockResource resource = lockRequest.resource();
        LockSession releasingSession = lockRequest.session();

        ResourceLockState lockState = resourceStates.get( resource );

        LockSession waitingSession = lockState.poll();
        if ( waitingSession != null )
        {
            lockState.release( releasingSession, lockRequest.lockType() );
            lockState.issue( waitingSession, lockRequest.lockType() );

            ragManager.stopWaitOn( resource, waitingSession );
            lockRequests.retrieve( waitingSession )
                    .ifPresent( CompletableLockRequest::lockAcquired );
        }
    }
}
