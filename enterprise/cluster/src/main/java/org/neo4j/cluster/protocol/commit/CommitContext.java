package org.neo4j.cluster.protocol.commit;

public class CommitContext
{
    void committed( Commands transaction ) {

    }

    public CommitContext snapshot()
    {
        return this;
    }
}
