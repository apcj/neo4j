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
package org.neo4j.coreedge.raft.state;

import java.io.File;
import java.io.IOException;

import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.impl.transaction.log.ReadAheadChannel;
import org.neo4j.storageengine.api.ReadableChannel;

public class StateRecoveryManager<STATE>
{
    public static class RecoveryStatus
    {
        private File previouslyInactive;
        private File previouslyActive;

        public File previouslyActive()
        {
            return previouslyActive;
        }

        public File previouslyInactive()
        {
            return previouslyInactive;
        }

        public void setFileStatus( File active, File inactive )
        {
            this.previouslyActive = active;
            this.previouslyInactive = inactive;
        }
    }

    protected final FileSystemAbstraction fileSystem;
    private final ChannelMarshal<STATE> marshal;
    private final StateStuff<STATE> stateStuff;

    public StateRecoveryManager( FileSystemAbstraction fileSystem, ChannelMarshal<STATE> marshal, StateStuff<STATE> stateStuff )
    {
        this.fileSystem = fileSystem;
        this.marshal = marshal;
        this.stateStuff = stateStuff;
    }

    /**
     * @return RecoveryStatus containing the previously active and previously inactive files. The previously active
     * file contains the latest readable log index (though it may also contain some garbage) and the inactive file is
     * safe to become the new state holder.
     * @throws IOException if any IO goes wrong.
     */
    public RecoveryStatus recover( File fileA, File fileB ) throws IOException
    {
        assert fileA != null && fileB != null;

        RecoveryStatus recoveryStatus = new RecoveryStatus();

        ensureExists( fileA );
        ensureExists( fileB );

        long a = getOrdinalOfLastRecord( fileA );
        long b = getOrdinalOfLastRecord( fileB );

        if ( a > b )
        {
            recoveryStatus.setFileStatus( fileA, fileB );
        }
        else
        {
            recoveryStatus.setFileStatus( fileB, fileA );
        }

        return recoveryStatus;
    }

    private void ensureExists( File file ) throws IOException
    {
        if ( !fileSystem.fileExists( file ) )
        {
            fileSystem.mkdirs( file.getParentFile() );
            fileSystem.create( file ).close();
        }
    }

    protected long getOrdinalOfLastRecord( File file ) throws IOException
    {
        return stateStuff.ordinal( readLastEntryFrom( file ) );
    }

    public STATE readLastEntryFrom( File file )
            throws IOException
    {
        final ReadableChannel channel = new ReadAheadChannel<>( fileSystem.open( file, "r" ) );

        STATE result = stateStuff.startState();
        STATE lastRead;

        while ( (lastRead = marshal.unmarshal( channel)) != null )
        {
            result = lastRead;
        }

        return result;
    }
}
