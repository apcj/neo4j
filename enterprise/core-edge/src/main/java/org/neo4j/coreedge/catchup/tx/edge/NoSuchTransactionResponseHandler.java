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
package org.neo4j.coreedge.catchup.tx.edge;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import org.neo4j.coreedge.catchup.CatchupClientProtocol;
import org.neo4j.coreedge.catchup.tx.core.NoSuchTransactionResponse;

import static org.neo4j.coreedge.catchup.CatchupClientProtocol.NextMessage;

public class NoSuchTransactionResponseHandler extends SimpleChannelInboundHandler<NoSuchTransactionResponse>
{
    private final CatchupClientProtocol protocol;
    private final NoSuchTransactionListener listener;

    public NoSuchTransactionResponseHandler( CatchupClientProtocol protocol, NoSuchTransactionListener
            noSuchTransactionListener )
    {
        this.protocol = protocol;
        this.listener = noSuchTransactionListener;
    }

    @Override
    protected void channelRead0( ChannelHandlerContext ctx, NoSuchTransactionResponse msg ) throws Exception
    {
        listener.onNoSuchTransaction( msg.txId() );
        protocol.expect( NextMessage.MESSAGE_TYPE );
    }
}
