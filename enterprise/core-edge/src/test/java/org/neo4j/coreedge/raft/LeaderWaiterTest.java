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

import org.junit.Test;

import org.neo4j.coreedge.server.RaftTestMember;
import org.neo4j.kernel.monitoring.Monitors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LeaderWaiterTest
{
    @Test
    public void shouldThrowExceptionIfNoLeaderAppearsWithinTimeout() throws Exception
    {
        // Given
        int leaderWaitTimeout = 10;
        LeaderWaiter<RaftTestMember> leaderWaiter = new LeaderWaiter<>( leaderWaitTimeout, new Monitors() );

        try
        {
            // When
            // There is no leader
            leaderWaiter.getLeader();
            fail( "Should have thrown exception" );
        }
        // Then
        catch ( NoLeaderTimeoutException e )
        {
            // expected
        }
    }

    @Test
    public void shouldMonitorLeaderNotFound() throws Exception
    {
        // Given
        int leaderWaitTimeout = 10;

        Monitors monitors = new Monitors();
        LeaderNotFoundMonitor leaderNotFoundMonitor = new StubLeaderNotFoundMonitor();
        monitors.addMonitorListener( leaderNotFoundMonitor );

        LeaderWaiter<RaftTestMember> leaderWaiter = new LeaderWaiter<>( leaderWaitTimeout, monitors );

        try
        {
            // When
            // There is no leader
            leaderWaiter.getLeader();
            fail( "Should have thrown exception" );
        }
        // Then
        catch ( NoLeaderTimeoutException e )
        {
            // expected
            assertEquals( 1, leaderNotFoundMonitor.leaderNotFoundExceptions() );
        }
    }

    private class StubLeaderNotFoundMonitor implements LeaderNotFoundMonitor
    {
        long count = 0;

        @Override
        public long leaderNotFoundExceptions()
        {
            return count;
        }

        @Override
        public void increment()
        {
            count++;
        }
    }

}