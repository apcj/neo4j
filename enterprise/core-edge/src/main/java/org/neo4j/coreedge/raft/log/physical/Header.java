package org.neo4j.coreedge.raft.log.physical;

public class Header
{
    public final long version;
    public final long prevIndex;
    public final long prevTerm;

    public Header( long version, long prevIndex, long prevTerm )
    {
        this.version = version;
        this.prevIndex = prevIndex;
        this.prevTerm = prevTerm;
    }
}
