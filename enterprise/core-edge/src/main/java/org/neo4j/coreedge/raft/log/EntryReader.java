package org.neo4j.coreedge.raft.log;

import java.io.IOException;

import org.neo4j.cursor.IOCursor;

public interface EntryReader
{
    IOCursor<RaftLogAppendRecord> readEntriesInVersion( long version ) throws IOException;
}
