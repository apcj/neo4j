package org.neo4j.coreedge.raft.log.physical;

import java.io.File;
import java.io.IOException;

import org.neo4j.coreedge.raft.log.PositionAwareRaftLogAppendRecord;
import org.neo4j.cursor.IOCursor;

import static java.lang.String.format;

public class VersionFile
{
    private final long version;
    private final File file;
    private final long size;
    private final HeaderReader headerReader;

    public VersionFile( long version, File file, long size, HeaderReader headerReader )
    {
        this.version = version;
        this.file = file;
        this.size = size;
        this.headerReader = headerReader;
    }

    public IOCursor<PositionAwareRaftLogAppendRecord> readEntries() throws IOException
    {
        return null;
    }

    public long version()
    {
        return version;
    }

    public File file()
    {
        return file;
    }

    public long size()
    {
        return size;
    }

    @Override
    public String toString()
    {
        return format( "%d: %s", version, file );
    }

    public Header header() throws DamagedLogStorageException
    {
        Header header = headerReader.readHeader( file );
        if ( version != header.version )
        {
            throw new DamagedLogStorageException( "Expected file [%s] to contain log version %d, but " +
                    "contained log version %d", file, version, header.version );
        }

        return header;
    }

    public void truncate( long lastValidByte )
    {

    }

    public void writeHeader( Header header )
    {

    }
}
