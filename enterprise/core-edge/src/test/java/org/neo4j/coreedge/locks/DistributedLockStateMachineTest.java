package org.neo4j.coreedge.locks;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.mockito.InOrder;

import org.neo4j.coreedge.locks.CompletableLockRequests.CompletableLockRequest;
import org.neo4j.kernel.impl.locking.ResourceTypes;
import org.neo4j.kernel.impl.locking.community.LockResource;
import org.neo4j.logging.NullLogProvider;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.neo4j.coreedge.locks.LockOperation.ACQUIRE;
import static org.neo4j.coreedge.locks.LockOperation.RELEASE;
import static org.neo4j.coreedge.locks.LockType.EXCLUSIVE;
import static org.neo4j.coreedge.locks.LockType.SHARED;

public class DistributedLockStateMachineTest
{
    private final LockResource resource = new LockResource( ResourceTypes.NODE, 0 );

    @Test
    public void shouldIssueExclusiveLockIfNotHeld() throws Exception
    {
        // given
        LockSession session = new LockSession();
        StubCompletableLockRequests requests = new StubCompletableLockRequests();
        CompletableLockRequest request = requests.register( session );

        DistributedLockStateMachine stateMachine = new DistributedLockStateMachine( requests,
                NullLogProvider.getInstance() );

        // when
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, session ), 0 );

        // then
        verify( request ).lockAcquired();
    }

    @Test
    public void shouldNotIssueExclusiveLockIfAnotherSessionHoldsExclusiveLock() throws Exception
    {
        // given
        LockSession sessionA = new LockSession();
        LockSession sessionB = new LockSession();
        StubCompletableLockRequests requests = new StubCompletableLockRequests();
        CompletableLockRequest requestA = requests.register( sessionA );
        CompletableLockRequest requestB = requests.register( sessionB );

        DistributedLockStateMachine stateMachine = new DistributedLockStateMachine( requests,
                NullLogProvider.getInstance() );

        // when
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionB ), 0 );

        // then
        verify( requestA ).lockAcquired();
        verify( requestB, never() ).lockAcquired();
    }

    @Test
    public void shouldIssueExclusiveLockOnceReleasedByPreviousHolder() throws Exception
    {
        // given
        LockSession sessionA = new LockSession();
        LockSession sessionB = new LockSession();
        StubCompletableLockRequests requests = new StubCompletableLockRequests();
        CompletableLockRequest requestB = requests.register( sessionB );

        DistributedLockStateMachine stateMachine = new DistributedLockStateMachine( requests,
                NullLogProvider.getInstance() );

        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource,
                sessionB ), 1 );

        // when
        stateMachine.onReplicated( new LockRequest( RELEASE, EXCLUSIVE, resource, sessionA ), 0 );

        // then
        verify( requestB ).lockAcquired();
    }

    @Test
    public void shouldIssueSharedLockWhenAnotherSessionHoldsSharedLock() throws Exception
    {
        // given
        LockSession sessionA = new LockSession();
        LockSession sessionB = new LockSession();
        StubCompletableLockRequests requests = new StubCompletableLockRequests();
        CompletableLockRequest requestA = requests.register( sessionA );
        CompletableLockRequest requestB = requests.register( sessionB );

        DistributedLockStateMachine stateMachine = new DistributedLockStateMachine( requests,
                NullLogProvider.getInstance() );

        // when
        stateMachine.onReplicated( new LockRequest( ACQUIRE, SHARED, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, SHARED, resource, sessionB ), 0 );

        // then
        verify( requestA ).lockAcquired();
        verify( requestB ).lockAcquired();
    }

    @Test
    public void shouldNotIssueExclusiveLockToWhenAnotherSessionHoldsSharedLock() throws Exception
    {
        // given
        LockSession sessionA = new LockSession();
        LockSession sessionB = new LockSession();
        StubCompletableLockRequests requests = new StubCompletableLockRequests();
        CompletableLockRequest requestA = requests.register( sessionA );
        CompletableLockRequest requestB = requests.register( sessionB );

        DistributedLockStateMachine stateMachine = new DistributedLockStateMachine( requests,
                NullLogProvider.getInstance() );

        // when
        stateMachine.onReplicated( new LockRequest( ACQUIRE, SHARED, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionB ), 0 );

        // then
        verify( requestA ).lockAcquired();
        verify( requestB, never() ).lockAcquired();
    }

    @Test
    public void shouldIssueExclusiveLockWhenSessionAlreadyHoldsSharedLock() throws Exception
    {
        // given
        LockSession sessionA = new LockSession();
        StubCompletableLockRequests requests = new StubCompletableLockRequests();
        CompletableLockRequest requestA = requests.register( sessionA );

        DistributedLockStateMachine stateMachine = new DistributedLockStateMachine( requests,
                NullLogProvider.getInstance() );

        // when
        stateMachine.onReplicated( new LockRequest( ACQUIRE, SHARED, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionA ), 0 );

        // then
        verify( requestA, times( 2 ) ).lockAcquired();
    }

    @Test
    public void shouldNotIssueExclusiveLockWhenNotAllExclusiveLockInvocationsHaveBeenReleased() throws Exception
    {
        // given
        LockSession sessionA = new LockSession();
        LockSession sessionB = new LockSession();
        StubCompletableLockRequests requests = new StubCompletableLockRequests();
        CompletableLockRequest requestB = requests.register( sessionB );

        DistributedLockStateMachine stateMachine = new DistributedLockStateMachine( requests,
                NullLogProvider.getInstance() );

        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( RELEASE, EXCLUSIVE, resource, sessionA ), 0 );

        // when
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionB ), 0 );

        // then
        verify( requestB, never() ).lockAcquired();
    }

    @Test
    public void shouldNotIssueExclusiveLockWhenNotAllSharedLockInvocationsHaveBeenReleased() throws Exception
    {
        // given
        LockSession sessionA = new LockSession();
        LockSession sessionB = new LockSession();
        StubCompletableLockRequests requests = new StubCompletableLockRequests();
        CompletableLockRequest requestB = requests.register( sessionB );

        DistributedLockStateMachine stateMachine = new DistributedLockStateMachine( requests,
                NullLogProvider.getInstance() );

        stateMachine.onReplicated( new LockRequest( ACQUIRE, SHARED, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, SHARED, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( RELEASE, SHARED, resource, sessionA ), 0 );

        // when
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionB ), 0 );

        // then
        verify( requestB, never() ).lockAcquired();
    }

    @Test
    public void shouldIssueQueuedLockRequestsInTheOrderTheyWereRequested() throws Exception
    {
        // given
        LockSession sessionA = new LockSession();
        LockSession sessionB = new LockSession();
        LockSession sessionC = new LockSession();
        LockSession sessionD = new LockSession();
        StubCompletableLockRequests requests = new StubCompletableLockRequests();
        CompletableLockRequest requestA = requests.register( sessionA );
        CompletableLockRequest requestB = requests.register( sessionB );
        CompletableLockRequest requestC = requests.register( sessionC );
        CompletableLockRequest requestD = requests.register( sessionD );

        DistributedLockStateMachine stateMachine = new DistributedLockStateMachine( requests,
                NullLogProvider.getInstance() );

        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionB ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionC ), 0 );
        stateMachine.onReplicated( new LockRequest( ACQUIRE, EXCLUSIVE, resource, sessionD ), 0 );

        // when
        stateMachine.onReplicated( new LockRequest( RELEASE, EXCLUSIVE, resource, sessionA ), 0 );
        stateMachine.onReplicated( new LockRequest( RELEASE, EXCLUSIVE, resource, sessionB ), 0 );
        stateMachine.onReplicated( new LockRequest( RELEASE, EXCLUSIVE, resource, sessionC ), 0 );

        // then
        InOrder order = inOrder( requestA, requestB, requestC, requestD );
        order.verify( requestA ).lockAcquired();
        order.verify( requestB ).lockAcquired();
        order.verify( requestC ).lockAcquired();
        order.verify( requestD ).lockAcquired();
    }

    private static class StubCompletableLockRequests implements CompletableLockRequests
    {
        private final Map<LockSession, CompletableLockRequest> map = new HashMap<>();

        CompletableLockRequest register( LockSession session )
        {
            CompletableLockRequest request = mock( CompletableLockRequest.class );
            map.put( session, request );
            return request;
        }

        @Override
        public Optional<CompletableLockRequest> retrieve( LockSession session )
        {
            return Optional.ofNullable( map.get( session ) );
        }
    }
}