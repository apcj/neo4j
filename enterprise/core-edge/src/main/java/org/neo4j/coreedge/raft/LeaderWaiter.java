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
package org.neo4j.coreedge.raft;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.neo4j.kernel.monitoring.Monitors;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class LeaderWaiter<MEMBER> implements LeaderLocator<MEMBER>
{
    private final long leaderWaitTimeout;
    private final LeaderNotFoundMonitor leaderNotFoundMonitor;
    private volatile CompletableFuture<MEMBER> futureLeader = // of the conservative party
            new CompletableFuture<>();

    public LeaderWaiter( long leaderWaitTimeout, Monitors monitors )
    {
        this.leaderWaitTimeout = leaderWaitTimeout;
        leaderNotFoundMonitor = monitors.newMonitor( LeaderNotFoundMonitor.class );
    }

    @Override
    public MEMBER getLeader() throws NoLeaderTimeoutException
    {
        try
        {
            return futureLeader.get( leaderWaitTimeout, MILLISECONDS );
        }
        catch ( InterruptedException | ExecutionException | TimeoutException e )
        {
            leaderNotFoundMonitor.increment();
            throw new NoLeaderTimeoutException();
        }
    }

    public void setLeader( MEMBER leader )
    {
        if ( leader != null && !futureLeader.isDone() )
        {
            futureLeader.complete( leader );
        }
        else if ( futureLeader.isDone() && !futureLeader.getNow( null ).equals( leader ) )
        {
            futureLeader = new CompletableFuture<>();
            if( leader != null )
            {
                futureLeader.complete( leader );
            }
        }
    }
}
