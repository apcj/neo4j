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

import org.neo4j.coreedge.raft.log.EntryReader;
import org.neo4j.coreedge.raft.log.RaftLogAppendRecord;
import org.neo4j.cursor.IOCursor;

/**
 * Determines which versions contain which entries.
 */
public class Recovery
{
    private final VersionFiles files;
    private final HeaderReader headerReader;
    private final EntryReader reader;
    private final HeaderWriter headerWriter;

    public Recovery( VersionFiles files, HeaderReader headerReader, EntryReader reader, HeaderWriter headerWriter )
    {
        this.files = files;
        this.headerReader = headerReader;
        this.reader = reader;
        this.headerWriter = headerWriter;
    }

    /**
     * output: currentVersion, prevIndex, prevTerm, appendIndex
     * effects: current version file starts with a valid header and contains no extra bytes beyond the last entry at
     * appendIndex
     */
    public LogState recover() throws IOException, DamagedLogStorageException
    {
        long currentVersion = -1;
        long prevIndex = -1;
        long prevTerm = -1;
        long appendIndex = -1;
        long term = -1;
        VersionIndexRanges ranges = new VersionIndexRanges();

        Iterator<VersionFiles.VersionFile> versionFiles = files.filesInVersionOrder().iterator();
        boolean encounteredMissingHeader = false;

        VersionFiles.VersionFile file = null;
        while ( versionFiles.hasNext() && !encounteredMissingHeader )
        {
            file = versionFiles.next();

            Header header = headerReader.readHeader( file.file );
            if ( header == null )
            {
                encounteredMissingHeader = true;
            }
            else
            {
                if ( noVersionsFoundYet( currentVersion ) )
                {
                    prevIndex = header.prevIndex;
                    prevTerm = header.prevTerm;
                }
                ranges.add( header.version, header.prevIndex );

                appendIndex = header.prevIndex;
                term = header.prevTerm;

                verifyVersion( currentVersion, file, header );
                currentVersion = header.version;
            }
        }

        verifyNoOrphanFiles( versionFiles, file );

        if ( noVersionsFoundYet( currentVersion ) )
        {
            file = files.createNewVersionFile( 0 );
        }
        else
        {
            try ( IOCursor<RaftLogAppendRecord> entryCursor = reader.readEntriesInVersion( currentVersion ) )
            {
                while ( entryCursor.next() )
                {
                    RaftLogAppendRecord record = entryCursor.get();
                    appendIndex = record.logIndex();
                    term = record.logEntry().term();
                }
            }
        }

        if ( noVersionsFoundYet( currentVersion ) || encounteredMissingHeader )
        {
            currentVersion++;

            Header header = new Header( currentVersion, appendIndex, term );
            ranges.add( header.version, header.prevIndex );
            headerWriter.write( file.file, header );
        }

        return new LogState( currentVersion, prevIndex, prevTerm, appendIndex, term, ranges );
    }

    public void verifyNoOrphanFiles( Iterator<VersionFiles.VersionFile> versionFiles, VersionFiles.VersionFile file ) throws DamagedLogStorageException
    {
        if ( versionFiles.hasNext() )
        {
            throw new DamagedLogStorageException(
                    "Found empty file [%s] but there are files with higher version numbers: %s",
                    file.file, collectOrphans( versionFiles ) );
        }
    }

    public void verifyVersion( long currentVersion, VersionFiles.VersionFile file, Header header ) throws DamagedLogStorageException
    {
        if ( header.version != file.version )
        {
            throw new DamagedLogStorageException( "Expected file [%s] to contain log version %d, but " +
                    "contained log version %d", file.file, file.version, header.version );
        }
        if ( currentVersion >= 0 && currentVersion + 1 != header.version )
        {
            throw new DamagedLogStorageException( "Missing expected log file version %d amongst series of files %s", currentVersion + 1, files.filesInVersionOrder() );
        }
    }

    public List<VersionFiles.VersionFile> collectOrphans( Iterator<VersionFiles.VersionFile> versionFiles )
    {
        List<VersionFiles.VersionFile> orphans = new ArrayList<>();
        while ( versionFiles.hasNext() )
        {
            orphans.add( versionFiles.next() );
        }
        return orphans;
    }

    public boolean noVersionsFoundYet( long currentVersion )
    {
        return currentVersion < 0;
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
