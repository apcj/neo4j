package org.neo4j.coreedge.raft.log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import org.neo4j.coreedge.raft.log.physical.VersionIndexRanges;
import org.neo4j.cursor.IOCursor;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.io.fs.StoreChannel;
import org.neo4j.kernel.impl.transaction.log.LogVersionedStoreChannel;
import org.neo4j.kernel.impl.transaction.log.PhysicalLogVersionedStoreChannel;
import org.neo4j.kernel.impl.transaction.log.ReadAheadChannel;
import org.neo4j.kernel.impl.transaction.log.entry.LogHeader;

import static org.neo4j.kernel.impl.transaction.log.entry.LogHeader.LOG_HEADER_SIZE;
import static org.neo4j.kernel.impl.transaction.log.entry.LogHeaderReader.readLogHeader;

public class VersionBridgingRaftEntryStoreIT
{
    @Test
    public void foo() throws Exception
    {
        // given


        EphemeralFileSystemAbstraction fileSystem = new EphemeralFileSystemAbstraction();
        new VersionBridgingRaftEntryStore( new VersionIndexRanges(), new Reader( new VersionedLogFiles(), fileSystem ) );

        // when

        // then
    }

    private static class Reader implements VersionBridgingRaftEntryStore.SingleVersionReader
    {
        private final VersionedLogFiles files;
        private final FileSystemAbstraction fileSystem;

        private Reader( VersionedLogFiles files, FileSystemAbstraction fileSystem )
        {
            this.files = files;
            this.fileSystem = fileSystem;
        }

        @Override
        public IOCursor<RaftLogAppendRecord> readEntriesInVersion( long version ) throws IOException
        {
            File file = files.fileForVersion( version );

            StoreChannel rawChannel = fileSystem.open( file, "rw" );
            ByteBuffer buffer = ByteBuffer.allocate( LOG_HEADER_SIZE );
            LogHeader header = readLogHeader( buffer, rawChannel, true );
            assert header != null && header.logVersion == version;

            PhysicalLogVersionedStoreChannel physicalLogVersionedStoreChannel =
                    new PhysicalLogVersionedStoreChannel( rawChannel, version, header.logFormatVersion );
            ReadAheadChannel<LogVersionedStoreChannel> readAheadChannel = new ReadAheadChannel<>(
                    physicalLogVersionedStoreChannel );

            return new RaftAppendRecordCursor( readAheadChannel, new DummyRaftableContentSerializer() );
        }
    }

    private static class VersionedLogFiles
    {
        public File fileForVersion( long version )
        {
            return null;
        }
    }
}