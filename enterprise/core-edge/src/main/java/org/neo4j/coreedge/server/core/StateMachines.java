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
package org.neo4j.coreedge.server.core;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.coreedge.raft.log.RaftLog;
import org.neo4j.coreedge.raft.replication.ReplicatedContent;
import org.neo4j.coreedge.raft.replication.Replicator;

public class StateMachines implements RaftLog.Listener
{
    List<Replicator.ReplicatedContentListener> machines = new ArrayList<>();

    public void add( Replicator.ReplicatedContentListener stateMachine )
    {
        machines.add( stateMachine );
    }

    @Override
    public void onAppended( ReplicatedContent content, long logIndex )
    {
    }

    @Override
    public void onCommitted( ReplicatedContent content, long logIndex )
    {
        for ( Replicator.ReplicatedContentListener machine : machines )
        {
            machine.onReplicated( content, logIndex );
        }
    }

    @Override
    public void onTruncated( long fromLogIndex )
    {
    }
}
