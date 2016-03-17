package org.neo4j.coreedge.raft.log.physical;

import static java.lang.String.format;

public class LogHeader
{
    final long version;
    final long prevIndex;

    public LogHeader( long version, long prevIndex )
    {
        this.version = version;
        this.prevIndex = prevIndex;
    }

    @Override
    public String toString()
    {
        return format( "LogHeader{version=%d, prevIndex=%d}", version, prevIndex );
    }
}
