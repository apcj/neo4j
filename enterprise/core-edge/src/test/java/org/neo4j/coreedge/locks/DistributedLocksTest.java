package org.neo4j.coreedge.locks;

import org.junit.Test;

import org.neo4j.coreedge.raft.replication.StubReplicator;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.kernel.impl.locking.ResourceTypes;
import org.neo4j.logging.NullLogProvider;

public class DistributedLocksTest
{
    @Test
    public void shouldAcquireExclusiveLock() throws Exception
    {
        // given
        DistributedLocks locks = new DistributedLocks( new StubReplicator(), NullLogProvider.getInstance() );
        Locks.Client client = locks.newClient();

        // when
        client.acquireExclusive( ResourceTypes.NODE, 0 );
    }

    @Test
    public void shouldRejectLockIfHeldBySomeoneElse() throws Exception
    {
        // given
        DistributedLocks locks = new DistributedLocks( new StubReplicator(), NullLogProvider.getInstance() );
        Locks.Client client1 = locks.newClient();
        Locks.Client client2 = locks.newClient();

        // when
        client1.acquireExclusive( ResourceTypes.NODE, 0 );
        client2.tryExclusiveLock( ResourceTypes.NODE, 1 );
    }


}