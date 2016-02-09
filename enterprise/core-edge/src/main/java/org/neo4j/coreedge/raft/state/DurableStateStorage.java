package org.neo4j.coreedge.raft.state;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.internal.DatabaseHealth;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;

public class DurableStateStorage<STATE> extends LifecycleAdapter implements StateStorage<STATE>
{
    private final StatePersister<STATE> statePersister;
    private STATE initialState;

    public DurableStateStorage( FileSystemAbstraction fileSystemAbstraction, File stateDir, String name,
                                StateMarshal<STATE> marshal, int numberOfEntriesBeforeRotation,
                                Supplier<DatabaseHealth> databaseHealthSupplier, LogProvider logProvider )
            throws IOException
    {
        File fileA = new File( stateDir, name + ".a" );
        File fileB = new File( stateDir, name + ".b" );

        StateRecoveryManager<STATE> recoveryManager =
                new StateRecoveryManager<>( fileSystemAbstraction, marshal );

        final StateRecoveryManager.RecoveryStatus recoveryStatus = recoveryManager.recover( fileA, fileB );

        this.initialState =
                recoveryManager.readLastEntryFrom( recoveryStatus.previouslyActive() );

        this.statePersister = new StatePersister<>( fileA, fileB, fileSystemAbstraction, numberOfEntriesBeforeRotation,
                marshal, recoveryStatus.previouslyInactive(), databaseHealthSupplier );

        Log log = logProvider.getLog( getClass() );
        log.info( "%s state restored, up to level %d.", name, marshal.ordinal( initialState ) );
    }

    @Override
    public STATE getInitialState()
    {
        return initialState;
    }

    @Override
    public void persistStoreData( STATE state ) throws IOException
    {
        statePersister.persistStoreData( state );
    }

    @Override
    public void shutdown() throws Throwable
    {
        statePersister.close();
    }
}
