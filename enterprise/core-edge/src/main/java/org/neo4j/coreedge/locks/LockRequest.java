package org.neo4j.coreedge.locks;

import org.neo4j.coreedge.raft.replication.ReplicatedContent;
import org.neo4j.kernel.impl.locking.community.LockResource;

public class LockRequest implements ReplicatedContent
{
    private final LockOperation lockOperation;
    private final LockType lockType;

    private final LockResource resource;
    private final LockSession session;

    public LockRequest( LockOperation lockOperation, LockType lockType, LockResource resource, LockSession session )
    {
        this.lockOperation = lockOperation;
        this.lockType = lockType;
        this.resource = resource;
        this.session = session;
    }

    public LockOperation lockOperation()
    {
        return lockOperation;
    }

    public LockType lockType()
    {
        return lockType;
    }

    public LockResource resource()
    {
        return resource;
    }

    public LockSession session()
    {
        return session;
    }

}
