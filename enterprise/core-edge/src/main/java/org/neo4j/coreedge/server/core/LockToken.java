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

import java.util.Objects;

import org.neo4j.coreedge.raft.replication.ReplicatedContent;

import static java.lang.String.format;

public class LockToken<MEMBER> implements ReplicatedContent
{
    final MEMBER owner;
    final int id;

    public LockToken( MEMBER owner, int id )
    {
        this.owner = owner;
        this.id = id;
    }

    public MEMBER owner()
    {
        return owner;
    }

    public int requestedLockSessionId()
    {
        return id;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        { return true; }
        if ( o == null || getClass() != o.getClass() )
        { return false; }
        LockToken that = (LockToken) o;
        return id == that.id &&
               Objects.equals( owner, that.owner );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( owner, id );
    }

    @Override
    public String toString()
    {
        return format( "ReplicatedLockRequest{owner=%s, requestedLockSessionId=%d}", owner, id );
    }
}
