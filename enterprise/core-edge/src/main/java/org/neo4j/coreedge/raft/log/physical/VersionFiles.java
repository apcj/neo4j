/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.raft.log.physical;

import java.io.File;
import java.io.IOException;

import org.neo4j.coreedge.raft.log.PositionAwareRaftLogAppendRecord;
import org.neo4j.cursor.IOCursor;

import static java.lang.String.format;

public class VersionFiles
{
    public Iterable<VersionFile> filesInVersionOrder()
    {
        return null;
    }

    public VersionFile createNewVersionFile( long version )
    {
        return null;
    }

    public static class VersionFile
    {
        private final long version;
        private final File file;
        private final long size;

        public VersionFile( long version, File file, long size )
        {
            this.version = version;
            this.file = file;
            this.size = size;
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

        public Header header()
        {
            return new Header( -1,-1,-1 );
        }

        public void truncate( long lastValidByte )
        {

        }

        public void writeHeader( Header header )
        {

        }
    }
}
