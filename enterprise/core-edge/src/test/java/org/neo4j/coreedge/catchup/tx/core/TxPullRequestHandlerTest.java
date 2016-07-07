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
package org.neo4j.coreedge.catchup.tx.core;

import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.mockito.InOrder;

import org.neo4j.coreedge.catchup.CatchupServerProtocol;
import org.neo4j.coreedge.catchup.ResponseMessageType;
import org.neo4j.coreedge.catchup.tx.edge.TxPullRequest;
import org.neo4j.coreedge.catchup.tx.edge.TxPullResponse;
import org.neo4j.coreedge.server.StoreId;
import org.neo4j.cursor.IOCursor;
import org.neo4j.kernel.impl.transaction.CommittedTransactionRepresentation;
import org.neo4j.kernel.impl.transaction.log.LogicalTransactionStore;
import org.neo4j.kernel.impl.transaction.log.NoSuchTransactionException;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.kernel.impl.transaction.log.entry.OnePhaseCommit;
import org.neo4j.kernel.monitoring.Monitors;

import static java.lang.System.currentTimeMillis;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.neo4j.coreedge.catchup.CatchupServerProtocol.NextMessage.MESSAGE_TYPE;
import static org.neo4j.coreedge.catchup.CatchupServerProtocol.NextMessage.TX_PULL;
import static org.neo4j.function.Suppliers.singleton;
import static org.neo4j.kernel.impl.util.Cursors.cursor;
import static org.neo4j.kernel.impl.util.Cursors.io;

public class TxPullRequestHandlerTest
{
    @Test
    public void shouldReplyWithTransactions() throws Exception
    {
        // given
        long clientTxId = 12;
        StoreId storeId = new StoreId( 1, 2, 3, 4 );

        TransactionIdStore transactionIdStore = mock( TransactionIdStore.class );
        when( transactionIdStore.getLastCommittedTransactionId() ).thenReturn( clientTxId + 2 );

        LogicalTransactionStore logicalTransactionStore = mock( LogicalTransactionStore.class );
        IOCursor<CommittedTransactionRepresentation> cursor = io( cursor( tx( 13 ), tx( 14 ) ) );
        when( logicalTransactionStore.getTransactions( clientTxId + 1 ) ).thenReturn( cursor );

        CatchupServerProtocol protocol = new CatchupServerProtocol();
        protocol.expect( TX_PULL );
        TxPullRequestHandler handler = new TxPullRequestHandler( protocol, singleton( storeId ),
                singleton( transactionIdStore ), singleton( logicalTransactionStore ), new Monitors() );
        ChannelHandlerContext ctx = mock( ChannelHandlerContext.class );

        // when
        handler.channelRead0( ctx, new TxPullRequest( clientTxId ) );

        // then
        InOrder order = inOrder( ctx );
        order.verify( ctx ).write( eq( ResponseMessageType.TX ) );
        order.verify( ctx ).write( argThat( instanceOf( TxPullResponse.class ) ) );
        order.verify( ctx ).write( eq( ResponseMessageType.TX ) );
        order.verify( ctx ).write( argThat( instanceOf( TxPullResponse.class ) ) );
        order.verify( ctx ).flush();
        order.verify( ctx ).write( ResponseMessageType.TX_STREAM_FINISHED );
        order.verify( ctx ).write( new TxStreamFinishedResponse( clientTxId + 2 ) );
        order.verify( ctx ).flush();
        order.verifyNoMoreInteractions();

        assertTrue( protocol.isExpecting( MESSAGE_TYPE ) );
    }

    @Test
    public void shouldReplyIfNoSuchTransactionAvailable() throws Exception
    {
        // given
        long clientTxId = 12;
        StoreId storeId = new StoreId( 1, 2, 3, 4 );

        TransactionIdStore transactionIdStore = mock( TransactionIdStore.class );
        when( transactionIdStore.getLastCommittedTransactionId() ).thenReturn( clientTxId + 2 );

        LogicalTransactionStore logicalTransactionStore = mock( LogicalTransactionStore.class );
        when( logicalTransactionStore.getTransactions( clientTxId + 1 ) )
                .thenThrow( new NoSuchTransactionException( clientTxId ) );

        CatchupServerProtocol protocol = new CatchupServerProtocol();
        protocol.expect( TX_PULL );
        TxPullRequestHandler handler = new TxPullRequestHandler( protocol, singleton( storeId ),
                singleton( transactionIdStore ), singleton( logicalTransactionStore ), new Monitors() );
        ChannelHandlerContext ctx = mock( ChannelHandlerContext.class );

        // when
        handler.channelRead0( ctx, new TxPullRequest( clientTxId ) );

        // then
        InOrder order = inOrder( ctx );
        order.verify( ctx ).write( ResponseMessageType.NO_SUCH_TRANSACTION );
        order.verify( ctx ).write( new NoSuchTransactionResponse( clientTxId ) );
        order.verify( ctx ).flush();
        order.verifyNoMoreInteractions();

        assertTrue( protocol.isExpecting( MESSAGE_TYPE ) );
    }

    private CommittedTransactionRepresentation tx( long txId )
    {
        CommittedTransactionRepresentation tx = mock( CommittedTransactionRepresentation.class );
        when( tx.getCommitEntry() ).thenReturn( new OnePhaseCommit( txId, currentTimeMillis() ) );
        return tx;
    }
}
