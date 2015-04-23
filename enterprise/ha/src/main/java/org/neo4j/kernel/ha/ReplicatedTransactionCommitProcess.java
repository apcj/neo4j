/**
 * Copyright (c) 2002-2015 "Neo Technology,"
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
package org.neo4j.kernel.ha;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;

import org.neo4j.cluster.protocol.atomicbroadcast.AtomicBroadcast;
import org.neo4j.cluster.protocol.atomicbroadcast.AtomicBroadcastListener;
import org.neo4j.cluster.protocol.atomicbroadcast.AtomicBroadcastSerializer;
import org.neo4j.cluster.protocol.atomicbroadcast.ObjectStreamFactory;
import org.neo4j.cluster.protocol.atomicbroadcast.Payload;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.impl.api.TransactionCommitProcess;
import org.neo4j.kernel.impl.locking.LockGroup;
import org.neo4j.kernel.impl.transaction.TransactionRepresentation;
import org.neo4j.kernel.impl.transaction.tracing.CommitEvent;
import org.neo4j.kernel.lifecycle.Lifecycle;

public class ReplicatedTransactionCommitProcess implements TransactionCommitProcess, Lifecycle
{
    private final AtomicBroadcast atomicBroadcast;
    private final AtomicBroadcastSerializer serializer =
            new AtomicBroadcastSerializer( new ObjectStreamFactory(), new ObjectStreamFactory() );
    private final AtomicBroadcastListener broadcastListener;
    private final ConcurrentHashMap<UUID, Exchanger<Long>> pendingTransactions = new ConcurrentHashMap<>();

    public ReplicatedTransactionCommitProcess( AtomicBroadcast atomicBroadcast, final TransactionCommitProcess inner )
    {
        this.atomicBroadcast = atomicBroadcast;
        this.broadcastListener = new AtomicBroadcastListener()
        {
            @Override
            public void receive( Payload payload )
            {
                final Object value;
                try
                {
                    value = serializer.receive( payload );
                }
                catch ( IOException | ClassNotFoundException e )
                {
                    // TODO: inform the client somehow
                    throw new RuntimeException( e );
                }

                if ( value instanceof CommitMessage )
                {
                    CommitMessage message = (CommitMessage) value;
                    System.out.printf( "received %s%n", message.txCorrelationId );
                    Exchanger<Long> exchanger = pendingTransactions.get( message.txCorrelationId );
                    if ( exchanger != null )
                    {
                        try ( LockGroup locks = new LockGroup() )
                        {
                            long txId = inner.commit( message.tx, locks, CommitEvent.NULL );
                            exchanger.exchange( txId );
                        }
                        catch ( InterruptedException e )
                        {
                            // TODO: inform the client somehow
                            throw new RuntimeException( e );
                        }
                        catch ( TransactionFailureException e )
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
    }

    @Override
    public long commit( TransactionRepresentation representation, LockGroup locks, CommitEvent commitEvent ) throws TransactionFailureException
    {
        final UUID txCorrelationId = UUID.randomUUID();
        System.out.println( "broadcast = " + txCorrelationId );
        try
        {
            Payload payload = serializer.broadcast( new CommitMessage( txCorrelationId, representation ) );
            atomicBroadcast.broadcast( payload );
        }
        catch ( IOException e )
        {
            // TODO: inform the client somehow
            throw new RuntimeException( e );
        }

        final Exchanger<Long> exchanger = new Exchanger<>();
        pendingTransactions.put( txCorrelationId, exchanger );

        try
        {
            return exchanger.exchange( null );
        }
        catch ( InterruptedException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public void init() throws Throwable
    {
    }

    @Override
    public void start() throws Throwable
    {
        atomicBroadcast.addAtomicBroadcastListener( broadcastListener );
    }

    @Override
    public void stop() throws Throwable
    {
        atomicBroadcast.removeAtomicBroadcastListener( broadcastListener );
    }

    @Override
    public void shutdown() throws Throwable
    {
    }

    static class CommitMessage implements Serializable
    {
        private final UUID txCorrelationId;
        private final TransactionRepresentation tx;
        //byte[] data = new byte[994*1024+421]; // doesn't work over network... (netty limit)
        //byte[] data = new byte[994*1024+420]; // works! // TODO: Avoid 1MiB limit.

        static final long serialVersionUID = 0xBC72837206738BCBL; // TODO

        CommitMessage( UUID txCorrelationId, TransactionRepresentation tx )
        {
            this.txCorrelationId = txCorrelationId;
            this.tx = tx;
        }
    }
}
