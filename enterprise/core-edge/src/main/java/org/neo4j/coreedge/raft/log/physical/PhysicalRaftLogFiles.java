/*
 * Copyright (c) 2002-2016 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.coreedge.raft.log.physical;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.impl.transaction.log.entry.LogHeader;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;

import static java.lang.Math.max;
import static java.lang.Math.min;

import static org.neo4j.kernel.impl.transaction.log.entry.LogHeader.LOG_HEADER_SIZE;
import static org.neo4j.kernel.impl.transaction.log.entry.LogHeaderReader.readLogHeader;

/**
 * Used to figure out what logical log file to open when the database
 * starts up.
 */
public class PhysicalRaftLogFiles extends LifecycleAdapter
{
    private final File logBaseName;
    private final Pattern logFilePattern;
    private final FileSystemAbstraction fileSystem;
    private long highestVersion;
    private long lowestVersion;

    public static final String BASE_FILE_NAME = "raft.log";
    public static final String REGEX_DEFAULT_VERSION_SUFFIX = "\\.";
    public static final String DEFAULT_VERSION_SUFFIX = ".";

    public PhysicalRaftLogFiles( File directory, FileSystemAbstraction fileSystem )
    {
        this.logBaseName = new File( directory, BASE_FILE_NAME );
        this.logFilePattern = Pattern.compile( BASE_FILE_NAME + REGEX_DEFAULT_VERSION_SUFFIX + "\\d+" );
        this.fileSystem = fileSystem;
        this.highestVersion = getHighestLogVersion();
    }

    @Override
    public void init()
    {
        long lowest = -1;
        long highest = -1;
        for ( File file : fileSystem.listFiles( logBaseName.getParentFile() ) )
        {
            if ( logFilePattern.matcher( file.getName() ).matches() )
            {
                // Get version based on the name
                long logVersion = getLogVersion( file.getName() );
                highest = max( highest, logVersion );
                lowest = lowest == -1 ? logVersion : min( lowest, logVersion );
            }
        }
        highestVersion = Math.max( highest, 0 );
        lowestVersion = Math.max( lowest, 0 );
    }

    public File getLogFileForVersion( long version )
    {
        return new File( logBaseName.getPath() + DEFAULT_VERSION_SUFFIX + version );
    }

    public boolean versionExists( long version )
    {
        return fileSystem.fileExists( getLogFileForVersion( version ) );
    }

    public LogHeader extractHeader( long version ) throws IOException
    {
        return readLogHeader( fileSystem, getLogFileForVersion( version ) );
    }

    public boolean hasAnyEntries( long version )
    {
        return fileSystem.getFileSize( getLogFileForVersion( version ) ) > LOG_HEADER_SIZE;
    }

    public long getHighestLogVersion()
    {
        return highestVersion;
    }

    public long getLowestLogVersion()
    {
        return lowestVersion;
    }

    public long registerNewVersion()
    {
        return ++highestVersion;
    }

    static long getLogVersion( String historyLogFilename )
    {
        int index = historyLogFilename.lastIndexOf( DEFAULT_VERSION_SUFFIX );
        if ( index == -1 )
        {
            throw new RuntimeException( "Invalid log file '" + historyLogFilename + "'" );
        }
        return Long.parseLong( historyLogFilename.substring( index + DEFAULT_VERSION_SUFFIX.length() ) );
    }
}
