package org.neo4j.coreedge.raft.log.physical;

public class HeaderBuilder
{
    public long version;
    public long prevIndex;
    public long prevTerm;

    public static HeaderBuilder version(long version)
    {
        return new HeaderBuilder( version );
    }

    public HeaderBuilder( long version )
    {
        this.version = version;
    }

    public HeaderBuilder prevIndex( long prevIndex )
    {
        this.prevIndex = prevIndex;
        return this;
    }

    public Header prevTerm( long prevTerm )
    {
        this.prevTerm = prevTerm;
        return new Header( version, prevIndex, prevTerm );
    }
}
