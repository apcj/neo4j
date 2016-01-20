package org.neo4j.coreedge.locks;

import org.neo4j.coreedge.raft.replication.Replicator;
import org.neo4j.kernel.impl.locking.Locks;
import org.neo4j.logging.LogProvider;

public class DistributedLocks implements Locks
{
    private final PendingLockRequests pendingLockRequests = new PendingLockRequests();
    private final DistributedLockStateMachine stateMachine;

    private final Replicator replicator;

    public DistributedLocks( Replicator replicator, LogProvider logProvider )
    {
        this.replicator = replicator;
        stateMachine = new DistributedLockStateMachine( pendingLockRequests, logProvider );
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
