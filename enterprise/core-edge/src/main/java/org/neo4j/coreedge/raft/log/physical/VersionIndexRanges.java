package org.neo4j.coreedge.raft.log.physical;

import java.util.LinkedList;

import static java.lang.String.format;

public class VersionIndexRanges
{
    private final LinkedList<VersionIndexRange> ranges = new LinkedList<>();

    public void add( long version, long prevIndex )
    {
        if ( !ranges.isEmpty() && ranges.peekLast().version >= version )
        {
            throw new IllegalArgumentException( format( "Cannot accept range for version %d while having " +
                    "already accepted %d", version, ranges.peek().version ) );
        }
        while ( !ranges.isEmpty() )
        {
            VersionIndexRange range = ranges.peekLast();
            if ( range.prevIndex >= prevIndex )
            {
                ranges.removeLast();
            }
            else
            {
                range.endAt( prevIndex );
                break;
            }
        }
        ranges.add( new VersionIndexRange( version, prevIndex ) );
    }

    public void pruneVersion( long version )
    {
        while( !ranges.isEmpty() && ranges.getFirst().version <= version )
        {
            ranges.removeFirst();
        }
    }

    public VersionIndexRange versionForIndex( long index )
    {
        for ( int i = ranges.size() - 1; i >= 0; i-- )
        {
            VersionIndexRange range = ranges.get( i );
            if ( range.includes( index ) )
            {
                return range;
            }
        }
        return VersionIndexRange.OUT_OF_RANGE;
    }

    @Override
    public String toString()
    {
        return format( "RaftLogVersionRanges{ranges=%s}", ranges );
    }

    public long lowestVersion()
    {
        return ranges.peekFirst().version;
    }

    public long highestVersion()
    {
        return ranges.peekLast().version;
    }
}
