package org.neo4j.coreedge.raft.log;

import java.io.IOException;

import org.neo4j.coreedge.raft.log.physical.VersionIndexRange;
import org.neo4j.coreedge.raft.log.physical.VersionIndexRanges;
import org.neo4j.cursor.CursorValue;
import org.neo4j.cursor.IOCursor;

public class VersionBridgingRaftEntryStore implements RaftEntryStore
{
    private final VersionIndexRanges ranges;
    private final EntryReader reader;

    public VersionBridgingRaftEntryStore( VersionIndexRanges ranges, EntryReader reader )
    {
        this.ranges = ranges;
        this.reader = reader;
    }

    @Override
    public IOCursor<RaftLogAppendRecord> getEntriesFrom( long logIndex ) throws IOException, RaftLogCompactedException
    {
        return new IOCursor<RaftLogAppendRecord>()
        {
            private CursorValue<RaftLogAppendRecord> cursorValue = new CursorValue<>();
            private long nextIndex = logIndex;
            private VersionIndexRange range = VersionIndexRange.OUT_OF_RANGE;
            private IOCursor<RaftLogAppendRecord> versionCursor;

            @Override
            public boolean next() throws IOException
            {
                if ( !range.includes( nextIndex ) )
                {
                    range = ranges.versionForIndex( nextIndex );
                    if ( !range.includes( nextIndex ) )
                    {
                        return false;
                    }
                    close();
                    versionCursor = reader.readEntriesInVersion( range.version );
                }
                while ( versionCursor.next() )
                {
                    RaftLogAppendRecord record = versionCursor.get();
                    if ( record.logIndex() == nextIndex )
                    {
                        cursorValue.set( record );
                        nextIndex++;
                        return true;
                    }
                }
                cursorValue.invalidate();
                return false;
            }

            @Override
            public void close() throws IOException
            {
                if ( versionCursor != null )
                {
                    versionCursor.close();
                }
            }

            @Override
            public RaftLogAppendRecord get()
            {
                return cursorValue.get();
            }
        };
    }
}
