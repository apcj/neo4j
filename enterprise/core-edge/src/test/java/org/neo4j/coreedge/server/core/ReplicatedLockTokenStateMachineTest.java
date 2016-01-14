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

import org.junit.Test;

import org.neo4j.coreedge.raft.replication.StubReplicator;
import org.neo4j.coreedge.server.RaftTestMember;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static org.neo4j.coreedge.server.RaftTestMember.member;

public class ReplicatedLockTokenStateMachineTest
{
    @Test
    public void shouldKeepTrackOfGlobalLockSession() throws Exception
    {
        // given
        StubReplicator replicator = new StubReplicator();
        CurrentLockToken stateMachine = new LockTokenTokenMachine<>( replicator );

        // when
        replicator.replicate( new LockToken<>( member( 1 ), 0 ) );
        replicator.replicate( new LockToken<>( member( 2 ), 1 ) );

        // then
        assertEquals( 1, stateMachine.currentLockSession().id() );

        // when
        replicator.replicate( new LockToken<>( member( 3 ), 0 ) );

        // then
        assertEquals( 1, stateMachine.currentLockSession().id() );

        // when
        replicator.replicate( new LockToken<>( member( 4 ), 2 ) );

        // then
        assertEquals( 2, stateMachine.currentLockSession().id() );
    }

    @Test
    public void shouldKeepTrackOfLocalLockSession() throws Exception
    {
        // given
        StubReplicator replicator = new StubReplicator();
        RaftTestMember me = member( 0 );
        CurrentLockToken stateMachine = new LockTokenTokenMachine<>( replicator );

        // when
        replicator.replicate( new LockToken<>( member( 1 ), 0 ) );
        replicator.replicate( new LockToken<>( member( 2 ), 1 ) );

        // then
        assertFalse( stateMachine.currentLockSession().isMine() );

        // when
        replicator.replicate( new LockToken<>( me, 0 ) );

        // then
        assertFalse( stateMachine.currentLockSession().isMine() );

        // when
        replicator.replicate( new LockToken<>( me, 2 ) );

        // then
        assertTrue( stateMachine.currentLockSession().isMine() );
        assertEquals( 2, stateMachine.currentLockSession().id() );

        // when
        replicator.replicate( new LockToken<>( member( 1 ), 2 ) );

        // then
        assertTrue( stateMachine.currentLockSession().isMine() );
        assertEquals( 2, stateMachine.currentLockSession().id() );

        // when
        replicator.replicate( new LockToken<>( member( 1 ), 3 ) );

        // then
        assertFalse( stateMachine.currentLockSession().isMine() );
        assertEquals( 3, stateMachine.currentLockSession().id() );
    }

    @Test
    public void shouldIssueNextLockSessionId() throws Exception
    {
        // given
        StubReplicator replicator = new StubReplicator();
        LockTokenTokenMachine stateMachine = new LockTokenTokenMachine<>( replicator );

        // then
        assertEquals(1, stateMachine.nextId());

        // when
        replicator.replicate( new LockToken<>( member( 1 ), 3 ) );

        // then
        assertEquals( 4, stateMachine.nextId() );
    }

}