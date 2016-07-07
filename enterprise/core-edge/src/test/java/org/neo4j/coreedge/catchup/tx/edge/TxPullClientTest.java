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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.junit.After;
import org.junit.Test;

import org.neo4j.coreedge.catchup.storecopy.CoreClientExtractedInterfaceThatAlistairThinksIsAShitName;
import org.neo4j.coreedge.catchup.storecopy.edge.StoreFileStreamingCompleteListener;
import org.neo4j.coreedge.server.CoreMember;
import org.neo4j.kernel.impl.transaction.log.NoSuchTransactionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import static org.neo4j.coreedge.server.RaftTestMember.member;

public class TxPullClientTest
{
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @After
    public void after()
    {
        executor.shutdown();
    }

    @Test
    public void shouldPullTransactionsSuccessfully() throws Exception
    {
        // given
        StubClient stubClient = new StubClient();
        TxPullClient client = new TxPullClient( stubClient, 10_000 );
        long startingTxId = 42;
        long endTxId = 45;

        // when
        Future<Long> txId = executor.submit( () ->
                client.pullTransactions( member( 0 ), startingTxId, mock( TxPullResponseListener.class ) ) );
        stubClient.txStreamCompleteListener.get().onTxStreamingComplete( endTxId );

        // then
        assertEquals( (Long) endTxId, txId.get() );
    }

    @Test
    public void shouldThrowExceptionIfNoSuchTransactionFoundOnServer()
    {
        // given
        StubClient stubClient = new StubClient();
        TxPullClient client = new TxPullClient( stubClient, 10_000 );
        long startingTxId = 42;

        // when
        try
        {
            Future<Long> txId = executor.submit( () ->
                    client.pullTransactions( member( 0 ), startingTxId, mock( TxPullResponseListener.class ) ) );
            stubClient.noSuchTransactionListener.get().onNoSuchTransaction( startingTxId );
            txId.get();
            fail( "Was expecting to not be able to pull transactions." );
        }
        catch ( ExecutionException | InterruptedException e )
        {
            //Then
            //Should throw an exception.
            assertEquals( NoSuchTransactionException.class, e.getCause().getCause().getCause().getClass() );
        }
    }

    @Test
    public void shouldThrowExceptionIfCatchupTimeoutExceeded()
    {
        // given
        StubClient stubClient = new StubClient();
        TxPullClient client = new TxPullClient( stubClient, 1 );
        long startingTxId = 42;

        // when
        try
        {
//            Future<Long> txId = executor.submit( () ->
                    client.pullTransactions( member( 0 ), startingTxId, mock( TxPullResponseListener.class ) ); //);
//            stubClient.txStreamCompleteListener.completeExceptionally( new TimeoutException() );
//            txId.get();
            fail( "Was expecting to not be able to pull transactions." );
        }
        catch ( Exception e )
        {
            //Then
            //Should throw an exception.
            assertEquals( TimeoutException.class, e.getCause().getClass() );
        }
    }

    class StubClient implements CoreClientExtractedInterfaceThatAlistairThinksIsAShitName
    {
        private TxPullResponseListener txPullResponseListener;
        private CompletableFuture<StoreFileStreamingCompleteListener> storeFileStreamingCompleteListener = new
                CompletableFuture<>();
        private CompletableFuture<TxStreamCompleteListener> txStreamCompleteListener = new CompletableFuture<>();
        private CompletableFuture<NoSuchTransactionListener> noSuchTransactionListener = new CompletableFuture<>();

        @Override
        public void pollForTransactions( CoreMember serverAddress, long lastTransactionId )
        {
            // do nothing
        }

        @Override
        public void addTxPullResponseListener( TxPullResponseListener listener )
        {
            txPullResponseListener = listener;
        }

        @Override
        public void removeTxPullResponseListener( TxPullResponseListener listener )
        {
            txPullResponseListener = null;
        }

        @Override
        public void addStoreFileStreamingCompleteListener( StoreFileStreamingCompleteListener listener )
        {
            storeFileStreamingCompleteListener.complete( listener );
        }

        @Override
        public void removeStoreFileStreamingCompleteListener( StoreFileStreamingCompleteListener listener )
        {
            storeFileStreamingCompleteListener = null;
        }

        @Override
        public void addTxStreamCompleteListener( TxStreamCompleteListener listener )
        {
            txStreamCompleteListener.complete( listener );
        }

        @Override
        public void removeTxStreamCompleteListener( TxStreamCompleteListener listener )
        {
            txStreamCompleteListener = null;
        }

        @Override
        public void addNoSuchTransactionListener( NoSuchTransactionListener listener )
        {
            noSuchTransactionListener.complete( listener );
        }

        @Override
        public void removeNoSuchTransactionListener( NoSuchTransactionListener listener )
        {
            noSuchTransactionListener = null;
        }
    }
}
