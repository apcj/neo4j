package org.neo4j.kernel.ha.factory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import c5db.SimpleModuleInformationProvider;
import c5db.discovery.BeaconService;
import c5db.interfaces.DiscoveryModule;
import c5db.interfaces.LogModule;
import c5db.interfaces.ReplicationModule;
import c5db.interfaces.replication.GeneralizedReplicator;
import c5db.log.LogService;
import c5db.replication.C5GeneralizedReplicationService;
import c5db.replication.NioQuorumFileReaderWriter;
import c5db.replication.ReplicatorService;
import c5db.util.ExceptionHandlingBatchExecutor;
import c5db.util.FiberSupplier;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.Service;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.jetlang.fibers.Fiber;
import org.jetlang.fibers.PoolFiberFactory;

import org.neo4j.cluster.InstanceId;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.impl.api.TransactionCommitProcess;
import org.neo4j.kernel.impl.locking.LockGroup;
import org.neo4j.kernel.impl.transaction.TransactionRepresentation;
import org.neo4j.kernel.impl.transaction.tracing.CommitEvent;

import static com.google.common.util.concurrent.Futures.allAsList;

public class RAFTTransactionCommitProcess implements TransactionCommitProcess
{
    private static final int NUMBER_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();

    private final TransactionCommitProcess inner;

    public RAFTTransactionCommitProcess( TransactionCommitProcess commitProcess, InstanceId instanceId, int discoveryPort, int consensusPort )
    {
        this.inner = commitProcess;
        SimpleModuleInformationProvider moduleInfo = new SimpleModuleInformationProvider(new PoolFiberFactory( Executors.newSingleThreadExecutor() ).create(), stupidErrorHandler() );
        EventLoopGroup bossGroup = new NioEventLoopGroup(NUMBER_OF_PROCESSORS / 3);
        EventLoopGroup workerGroup = new NioEventLoopGroup(NUMBER_OF_PROCESSORS / 3);

        ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_OF_PROCESSORS);
        final PoolFiberFactory fiberFactory = new PoolFiberFactory(executorService);

        ReplicationModule replicationModule = null;
        FiberSupplier fiberSupplier = new FiberSupplier()
        {
            @Override
            public Fiber getNewFiber( Consumer<Throwable> throwableHandler )
            {
                return fiberFactory.create(new ExceptionHandlingBatchExecutor(stupidErrorHandler()));
            }
        };
        try
        {
            Path baseTestPath = Files.createTempDirectory( "quorum" );
            replicationModule = new ReplicatorService(bossGroup, workerGroup, instanceId.toIntegerIndex(), consensusPort, moduleInfo, fiberSupplier,
                    new NioQuorumFileReaderWriter( baseTestPath ));
            LogModule logModule = new LogService(baseTestPath, fiberSupplier);

            DiscoveryModule nodeInfoModule = new BeaconService(instanceId.toIntegerIndex(), discoveryPort, workerGroup, moduleInfo, fiberSupplier);

            startAll(moduleInfo, logModule, nodeInfoModule, replicationModule);

            C5GeneralizedReplicationService service = new C5GeneralizedReplicationService(replicationModule, logModule, fiberSupplier);
            GeneralizedReplicator replicator = service.createReplicator( "MY_QUORUM", Arrays.asList( 1L, 2L, 3L ) ).get();
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
//

    }

    private void startAll( SimpleModuleInformationProvider moduleInfo, LogModule logModule,
                           DiscoveryModule nodeInfoModule, ReplicationModule replicationModule ) throws Exception {
        List<ListenableFuture<Service.State>> startFutures = new ArrayList<>();

        startFutures.add(moduleInfo.startModule(logModule));
        startFutures.add(moduleInfo.startModule(nodeInfoModule));
        startFutures.add(moduleInfo.startModule(replicationModule));

        ListenableFuture<List<Service.State>> allFutures = allAsList(startFutures);

        // Block waiting for everything to start.
        allFutures.get();
    }

    private Consumer<Throwable> stupidErrorHandler()
    {
        return new Consumer<Throwable>() {

            @Override
            public void accept( Throwable value )
            {
                value.printStackTrace();
            }
        };
    }

    @Override
    public long commit( TransactionRepresentation representation, LockGroup locks, CommitEvent commitEvent ) throws TransactionFailureException
    {
        return inner.commit( representation, locks, commitEvent );
    }
}
