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

import org.junit.Test;

import org.neo4j.io.fs.FileSystemAbstraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.neo4j.coreedge.raft.log.physical.PhysicalRaftLogFile.DEFAULT_VERSION_SUFFIX;
import static org.neo4j.coreedge.raft.log.physical.PhysicalRaftLogFiles.BASE_FILE_NAME;

public class PhysicalRaftLogFilesTest
{
    private final FileSystemAbstraction fs = mock( FileSystemAbstraction.class );
    private final File tmpDirectory = new File( "." );

    @Test
    public void shouldGetTheFileNameForAGivenVersion()
    {
        // given
        final PhysicalRaftLogFiles files = new PhysicalRaftLogFiles( tmpDirectory, fs );
        final int version = 12;

        // when
        final File versionFileName = files.getLogFileForVersion( version );

        // then
        final File expected = new File( tmpDirectory, BASE_FILE_NAME + DEFAULT_VERSION_SUFFIX + version );
        assertEquals( expected, versionFileName );
    }

    @Test
    public void shouldBeAbleToRetrieveTheHighestLogVersion()
    {
        // given
        PhysicalRaftLogFiles files = new PhysicalRaftLogFiles( tmpDirectory, fs );

        final File[] filesOnDisk = new File[]{
                new File( tmpDirectory, BASE_FILE_NAME + DEFAULT_VERSION_SUFFIX + "1" ),
                new File( tmpDirectory, "crap" + DEFAULT_VERSION_SUFFIX + "4" ),
                new File( tmpDirectory, BASE_FILE_NAME + DEFAULT_VERSION_SUFFIX + "3" ),
                new File( tmpDirectory, BASE_FILE_NAME )
        };

        when( fs.listFiles( tmpDirectory ) ).thenReturn( filesOnDisk );
        files.init();

        // when
        final long highestLogVersion = files.getHighestLogVersion();

        // then
        assertEquals( 3, highestLogVersion );
    }

    @Test
    public void shouldIncrementVersionOnRegisterNewVersion() throws Exception
    {
        // given
        PhysicalRaftLogFiles files = new PhysicalRaftLogFiles( tmpDirectory, fs );

        // when
        long initialVersion = files.getHighestLogVersion();

        // then
        assertEquals( 0, initialVersion );

        // when
        long newVersion = files.registerNewVersion();

        // then
        assertEquals( initialVersion + 1, newVersion );
        assertEquals( initialVersion + 1, files.getHighestLogVersion() );
    }

    @Test
    public void shouldFindTheVersionBasedOnTheFilename()
    {
        // given
        final File file =
                new File( "v" + DEFAULT_VERSION_SUFFIX + DEFAULT_VERSION_SUFFIX + DEFAULT_VERSION_SUFFIX + "2" );

        // when
        // Get version based on the name
        long logVersion = PhysicalRaftLogFiles.getLogVersion( file.getName() );

        // then
        assertEquals( 2, logVersion );
    }

    @Test
    public void shouldThrowIfThereIsNoVersionInTheFileName()
    {
        // given
        final File file = new File( "wrong" );

        // when
        try
        {
            // Get version based on the name
            PhysicalRaftLogFiles.getLogVersion( file.getName() );
            fail( "should have thrown" );
        }
        catch ( RuntimeException ex )
        {
            assertEquals( "Invalid log file '" + file.getName() + "'", ex.getMessage() );
        }
    }

    @Test(expected = NumberFormatException.class)
    public void shouldThrowIfVersionIsNotANumber()
    {
        // given
        final File file = new File( "aa" + DEFAULT_VERSION_SUFFIX + "A" );

        // when
        // Get version based on the name
        PhysicalRaftLogFiles.getLogVersion( file.getName() );
    }
}
