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
package org.neo4j.coreedge.raft.net;

import io.netty.buffer.ByteBuf;

import org.neo4j.coreedge.server.CoreMember;
import org.neo4j.coreedge.server.core.LockToken;

public class ReplicatedLockRequestSerializer
{
    public static void serialize( LockToken<CoreMember> content, ByteBuf buffer )
    {
        buffer.writeInt( content.requestedLockSessionId() );
        new CoreMember.CoreMemberMarshal().marshal( content.owner(), buffer );
    }

    public static LockToken<CoreMember> deserialize( ByteBuf buffer )
    {
        int requestedLockSessionId = buffer.readInt();
        CoreMember owner = new CoreMember.CoreMemberMarshal().unmarshal( buffer );

        return new LockToken<>( owner, requestedLockSessionId );
    }
}
