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
package org.neo4j.coreedge.raft.roles;

import org.neo4j.coreedge.raft.RaftMessages;
import org.neo4j.coreedge.raft.log.RaftStorageException;
import org.neo4j.coreedge.raft.outcome.Outcome;
import org.neo4j.coreedge.raft.state.ReadableRaftState;

import static org.neo4j.coreedge.raft.Ballot.shouldVoteFor;

public class Voting
{
    public static <MEMBER> void handleVoteRequest( ReadableRaftState<MEMBER> state, Outcome<MEMBER> outcome,
                                                   RaftMessages.Vote.Request<MEMBER> voteRequest ) throws RaftStorageException
    {
        if ( voteRequest.term() > state.term() )
        {
            outcome.setNextTerm( voteRequest.term() );
            outcome.setVotedFor( null );
        }

        boolean willVoteForCandidate = shouldVoteFor( voteRequest.candidate(), outcome.getTerm(), voteRequest.term(),
                state.entryLog().readEntryTerm( state.entryLog().appendIndex() ), voteRequest.lastLogTerm(),
                state.entryLog().appendIndex(), voteRequest.lastLogIndex(),
                outcome.getVotedFor() );

        if ( willVoteForCandidate )
        {
            outcome.setVotedFor( voteRequest.from() );
            outcome.renewElectionTimeout();
        }

        outcome.addOutgoingMessage( new RaftMessages.Directed<>( voteRequest.from(), new RaftMessages.Vote.Response<>(
                state.myself(), outcome.getTerm(),
                willVoteForCandidate ) ) );
    }
}
