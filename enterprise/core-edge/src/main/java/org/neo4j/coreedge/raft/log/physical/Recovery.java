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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.neo4j.coreedge.raft.log.PositionAwareRaftLogAppendRecord;
import org.neo4j.coreedge.raft.log.RaftLogAppendRecord;
import org.neo4j.cursor.IOCursor;

/**
 * Determines which versions contain which entries.
 */
public class Recovery
{
    private final VersionFiles files;

    public Recovery( VersionFiles files )
    {
        this.files = files;
    }

    /**
     * output: currentVersion, prevIndex, prevTerm, appendIndex
     * effects: current version file starts with a valid header and contains no extra bytes beyond the last entry at
     * appendIndex
     */
    public LogState recover() throws IOException, DamagedLogStorageException
    {
        long prevIndex = -1;
        long prevTerm = -1;
        long appendIndex = -1;
        long term = -1;
        VersionIndexRanges ranges = new VersionIndexRanges();

        Iterator<VersionFile> versionFiles = files.filesInVersionOrder().iterator();
        boolean encounteredMissingHeader = false;

        VersionFile file = null;
        VersionFile latestWellFormedFile = null;
        while ( versionFiles.hasNext() && !encounteredMissingHeader )
        {
            file = versionFiles.next();
            Header header = file.header();
            if ( header == null )
            {
                encounteredMissingHeader = true;
            }
            else
            {
                if ( noVersionsFoundYet( latestWellFormedFile ) )
                {
                    prevIndex = header.prevIndex;
                    prevTerm = header.prevTerm;
                }
                ranges.add( header.version, header.prevIndex );

                appendIndex = header.prevIndex;
                term = header.prevTerm;

                verifyVersion( latestWellFormedFile, file, header );
                latestWellFormedFile = file;
            }
        }

        verifyNoOrphanFiles( versionFiles, file );

        if ( noVersionsFoundYet( latestWellFormedFile ) )
        {
            file = files.createNewVersionFile( 0 );
        }
        else
        {
            PositionAwareRaftLogAppendRecord positionAwareRecord = null;

            try ( IOCursor<PositionAwareRaftLogAppendRecord> entryCursor = latestWellFormedFile.readEntries() )
            {
                while ( entryCursor.next() )
                {

                    positionAwareRecord = entryCursor.get();
                    RaftLogAppendRecord record = positionAwareRecord.record();
                    appendIndex = record.logIndex();
                    term = record.logEntry().term();
                }
            }
            long lastValidByte = lastValidByte( positionAwareRecord );
            if ( file.size() > lastValidByte )
            {
                file.truncate(lastValidByte);
            }
        }

        if ( noVersionsFoundYet( latestWellFormedFile ) || encounteredMissingHeader )
        {
            long version = latestWellFormedFile != null ? latestWellFormedFile.version() +1 : 0;
            Header header = new Header( version, appendIndex, term );
            ranges.add( header.version, header.prevIndex );
            file.writeHeader( header );
            latestWellFormedFile = file;
        }

        return new LogState( latestWellFormedFile.version(), prevIndex, prevTerm, appendIndex, term, ranges );
    }

    public long lastValidByte( PositionAwareRaftLogAppendRecord positionAwareRecord )
    {
        return positionAwareRecord == null ? HeaderReader.HEADER_LENGTH : positionAwareRecord.endPosition();
    }

    public void verifyNoOrphanFiles( Iterator<VersionFile> versionFiles, VersionFile file ) throws DamagedLogStorageException
    {
        if ( versionFiles.hasNext() )
        {
            throw new DamagedLogStorageException(
                    "Found empty file [%s] but there are files with higher version numbers: %s",
                    file.file(), collectOrphans( versionFiles ) );
        }
    }

    public void verifyVersion( VersionFile latestWellFormedFile, VersionFile file, Header header ) throws DamagedLogStorageException
    {

        if ( latestWellFormedFile != null && latestWellFormedFile.version() + 1 != header.version )
        {
            throw new DamagedLogStorageException( "Missing expected log file version %d amongst series of files %s", latestWellFormedFile.version() + 1, files.filesInVersionOrder() );
        }
    }

    public List<VersionFile> collectOrphans( Iterator<VersionFile> versionFiles )
    {
        List<VersionFile> orphans = new ArrayList<>();
        while ( versionFiles.hasNext() )
        {
            orphans.add( versionFiles.next() );
        }
        return orphans;
    }

    public boolean noVersionsFoundYet( VersionFile currentVersion )
    {
        return currentVersion == null;
    }

    static class LogState
    {
        public final long prevIndex;
        public final long prevTerm;
        public final long appendIndex;
        public final long term;
        public final long currentVersion;
        public final VersionIndexRanges ranges;

        public LogState( long currentVersion, long prevIndex, long prevTerm,
                         long appendIndex, long term, VersionIndexRanges ranges )
        {
            this.currentVersion = currentVersion;
            this.prevIndex = prevIndex;
            this.prevTerm = prevTerm;
            this.appendIndex = appendIndex;
            this.term = term;
            this.ranges = ranges;
        }
    }
}
