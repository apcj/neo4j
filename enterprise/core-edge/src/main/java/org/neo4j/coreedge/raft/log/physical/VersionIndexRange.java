package org.neo4j.coreedge.raft.log.physical;

import static java.lang.String.format;

public class VersionIndexRange
{
    public final long version;
    public final long prevIndex;
    private long lastIndex = Long.MAX_VALUE;

    public VersionIndexRange( long version, long prevIndex )
    {
        this.version = version;
        this.prevIndex = prevIndex;
    }

    public boolean includes( long index )
    {
        return index <= lastIndex && index > prevIndex;
    }

    void endAt( long lastIndex )
    {
        this.lastIndex = lastIndex;
    }

    @Override
    public String toString()
    {
        return format( "%d: %d < index <= %d", version, prevIndex, lastIndex );
    }

    public static final VersionIndexRange OUT_OF_RANGE = new VersionIndexRange( -1, -1 ) {

        @Override
        public boolean includes( long index )
        {
            return false;
        }

        @Override
        void endAt( long lastIndex )
        {
            throw new UnsupportedOperationException( "Immutable" );
        }

        @Override
        public String toString()
        {
            return "OUT_OF_RANGE";
        }
    };
}
