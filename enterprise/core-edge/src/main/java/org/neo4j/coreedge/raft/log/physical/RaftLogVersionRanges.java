package org.neo4j.coreedge.raft.log.physical;

import java.util.LinkedList;

import static java.lang.String.format;

public class RaftLogVersionRanges
{
    private final LinkedList<FileIndexRange> ranges = new LinkedList<>();

    public void add( LogHeader logHeader )
    {
        if ( !ranges.isEmpty() && ranges.peekLast().version >= logHeader.version )
        {
            throw new IllegalArgumentException( format( "Cannot accept range for version %d while having " +
                    "already accepted %d", logHeader.version, ranges.peek().version ) );
        }
        while ( !ranges.isEmpty() )
        {
            FileIndexRange range = ranges.peekLast();
            if ( range.prevIndex >= logHeader.prevIndex )
            {
                ranges.removeLast();
            }
            else
            {
                range.lastIndex = logHeader.prevIndex;
                break;
            }
        }
        ranges.add( new FileIndexRange( logHeader.version, logHeader.prevIndex ) );
    }

    public void pruneVersion( long version )
    {
        while( !ranges.isEmpty() && ranges.getFirst().version <= version )
        {
            ranges.removeFirst();
        }
    }

    public long versionForIndex( long index )
    {
        for ( int i = ranges.size() - 1; i >= 0; i-- )
        {
            FileIndexRange range = ranges.get( i );
            if ( range.includes( index ) )
            {
                return range.version;
            }
        }
        return -1;
    }

    @Override
    public String toString()
    {
        return format( "RaftLogVersionRanges{ranges=%s}", ranges );
    }

    private static class FileIndexRange
    {
        final long version;
        final long prevIndex;
        long lastIndex = Long.MAX_VALUE;

        FileIndexRange( long version, long prevIndex )
        {
            this.version = version;
            this.prevIndex = prevIndex;
        }

        boolean includes( long index )
        {
            return index <= lastIndex && index > prevIndex;
        }

        @Override
        public String toString()
        {
            return format( "%d: %d < index <= %d", version, prevIndex, lastIndex );
        }
    }
}
