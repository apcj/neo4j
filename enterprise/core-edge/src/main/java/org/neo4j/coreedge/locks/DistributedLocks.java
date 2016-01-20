package org.neo4j.coreedge.locks;

import org.neo4j.coreedge.raft.replication.Replicator;
import org.neo4j.kernel.impl.locking.Locks;

public class DistributedLocks implements Locks
{
    private final PendingLockRequests pendingLockRequests = new PendingLockRequests();
    private final DistributedLockStateMachine stateMachine;

    private final Replicator replicator;

    public DistributedLocks( Replicator replicator )
    {
        this.replicator = replicator;
        stateMachine = new DistributedLockStateMachine( pendingLockRequests );
        replicator.subscribe( stateMachine );
    }

    @Override
    public Client newClient()
    {
        return new DistributedLockClient( pendingLockRequests, replicator );
    }

    @Override
    public void accept( Visitor visitor )
    {

    }

    @Override
    public void close()
    {

    }
}
