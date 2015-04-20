package org.neo4j.ha.commit;

import java.io.IOException;
import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.neo4j.cluster.protocol.atomicbroadcast.AtomicBroadcast;
import org.neo4j.cluster.protocol.atomicbroadcast.AtomicBroadcastListener;
import org.neo4j.cluster.protocol.atomicbroadcast.AtomicBroadcastSerializer;
import org.neo4j.cluster.protocol.atomicbroadcast.ObjectStreamFactory;
import org.neo4j.cluster.protocol.atomicbroadcast.Payload;
import org.neo4j.cluster.protocol.commit.ReplicatedTransactionLog;
import org.neo4j.kernel.impl.transaction.TransactionRepresentation;
import org.neo4j.kernel.impl.transaction.log.LogicalTransactionStore;
import org.neo4j.kernel.impl.transaction.tracing.LogAppendEvent;

public class AtomicBroadcastReplicatedTransactionLog implements ReplicatedTransactionLog
{
    private final AtomicBroadcast atomicBroadcast;
    private final AtomicBroadcastSerializer serializer =
            new AtomicBroadcastSerializer( new ObjectStreamFactory(), new ObjectStreamFactory() );

    private ConcurrentHashMap<UUID, Exchanger<Long>> pendingTransactions = new ConcurrentHashMap<>();

    public AtomicBroadcastReplicatedTransactionLog( AtomicBroadcast atomicBroadcast,
                                                    final LogicalTransactionStore logicalTransactionStore )
    {
        this.atomicBroadcast = atomicBroadcast;
        this.atomicBroadcast.addAtomicBroadcastListener( new AtomicBroadcastListener()
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
                    Exchanger<Long> exchanger = pendingTransactions.get( message.txCorrelationId );
                    if ( exchanger != null )
                    {
                        try
                        {
                            long txId = logicalTransactionStore.getAppender().append( message.tx, LogAppendEvent.NULL );
                            exchanger.exchange( txId );
                        }
                        catch ( IOException | InterruptedException e )
                        {
                            // TODO: inform the client somehow
                            throw new RuntimeException( e );
                        }
                    }
                }
            }
        } );
    }

    @Override
    public Future<Long> tx( TransactionRepresentation tx )
    {
        final UUID txCorrelationId = UUID.randomUUID();
        try
        {
            atomicBroadcast.broadcast( serializer.broadcast( new CommitMessage( txCorrelationId, tx ) ) );
        }
        catch ( IOException e )
        {
            // TODO: inform the client somehow
            throw new RuntimeException( e );
        }

        final Exchanger<Long> exchanger = new Exchanger<Long>();
        pendingTransactions.put( txCorrelationId, exchanger );

        return new Future<Long>()
        {
            @Override public boolean cancel( boolean mayInterruptIfRunning )
            {
                return false;
            }

            @Override public boolean isCancelled()
            {
                return false;
            }

            @Override public boolean isDone()
            {
                return false;
            }

            @Override public Long get() throws InterruptedException, ExecutionException
            {
                return exchanger.exchange( null );
            }

            @Override
            public Long get( long timeout, TimeUnit unit ) throws InterruptedException, ExecutionException,
                    TimeoutException
            {
                return exchanger.exchange( null, timeout, unit );
            }
        };
    }

    static class CommitMessage implements Serializable
    {
        private final UUID txCorrelationId;
        private final TransactionRepresentation tx;

        CommitMessage( UUID txCorrelationId, TransactionRepresentation tx )
        {
            this.txCorrelationId = txCorrelationId;
            this.tx = tx;
        }

//        Payload serialise()
//        {
//            byte[] uuidBytes = txCorrelationId.toString().getBytes();
//            return new Payload( uuidBytes, uuidBytes.length );
//        }
//
//        static CommitMessage deserialise( Payload payload )
//        {
//            PhysicalTransactionRepresentation dummyTx = new PhysicalTransactionRepresentation(
//                    Arrays.<Command>asList( new Command.NodeCommand() ) );
//            return new CommitMessage( UUID.fromString(new String( payload.getBuf() )), dummyTx );
//        }
    }

}
