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
package org.neo4j.coreedge.raft.log;

import java.io.IOException;

import org.neo4j.coreedge.raft.replication.ReplicatedContent;
import org.neo4j.coreedge.raft.state.ChannelMarshal;
import org.neo4j.kernel.impl.transaction.log.ReadAheadChannel;
import org.neo4j.storageengine.api.WritableChannel;

import static org.neo4j.coreedge.raft.log.PhysicalRaftLog.RecordType.APPEND;

public class PositionAwareRaftLogAppendRecord extends RaftLogRecord
{
    private final long startPosition;
    private final long endPosition;
    private final RaftLogAppendRecord record;

    public PositionAwareRaftLogAppendRecord(long startPosition, long endPosition, RaftLogAppendRecord record)
    {
        super( APPEND );
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.record = record;
    }

    public static PositionAwareRaftLogAppendRecord read( ReadAheadChannel channel, ChannelMarshal<ReplicatedContent> marshal ) throws IOException
    {
        long startPosition = channel.position();
        RaftLogAppendRecord record = RaftLogAppendRecord.read( channel, marshal );
        long endPosition = channel.position();

        return new PositionAwareRaftLogAppendRecord( startPosition, endPosition, record);
    }


    public static void write( WritableChannel channel, ChannelMarshal<ReplicatedContent> marshal, long appendIndex,
                              long term, ReplicatedContent content ) throws IOException
    {
        RaftLogAppendRecord.write( channel, marshal, appendIndex, term, content );
    }

    public long endPosition()
    {
        return endPosition;
    }

    public long startPosition()
    {
        return startPosition;
    }

    public RaftLogAppendRecord record()
    {
        return record;
    }
}
