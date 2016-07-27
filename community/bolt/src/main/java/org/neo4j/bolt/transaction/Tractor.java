package org.neo4j.bolt.transaction;

import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.exceptions.Status;
import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.api.security.AccessMode;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

public class Tractor
{
    private final GraphDatabaseAPI db;
    private final ThreadToStatementContextBridge txBridge;

    private KernelTransaction currentTransaction;

    public Tractor( GraphDatabaseAPI db, ThreadToStatementContextBridge txBridge )
    {
        this.db = db;
        this.txBridge = txBridge;
    }

    public void begin( KernelTransaction.Type type, AccessMode subject ) throws TransactionFailureException
    {
        db.beginTransaction( type, subject );
        currentTransaction = txBridge.getKernelTransactionBoundToThisThread( false );
    }

    public void beginIfNoCurrentTransaction( KernelTransaction.Type type, AccessMode subject )
            throws TransactionFailureException
    {
        if ( !hasTransaction() )
        {
            begin( type, subject );
        }
    }

    public void commit() throws TransactionFailureException
    {
        if ( hasTransaction() )
        {
            try
            {
                currentTransaction.success();
                currentTransaction.close();
            }
            finally
            {
                currentTransaction = null;
            }
        }
    }

    public void rollback() throws TransactionFailureException
    {
        if ( hasTransaction() )
        {
            try
            {
                currentTransaction.failure();
                currentTransaction.close();
            }
            finally
            {
                currentTransaction = null;
            }
        }
    }

    public void markForTermination( Status status )
    {
        if ( hasTransaction() )
        {
            currentTransaction.markForTermination( status );
        }
    }

    public void bindCurrentTransactionToCurrentThread()
    {
        if ( hasTransaction() )
        {
            txBridge.bindTransactionToCurrentThread( currentTransaction );
        }
    }

    public void unbindCurrentTransactionFromCurrentThread()
    {
        if ( hasTransaction() )
        {
            txBridge.unbindTransactionFromCurrentThread();
        }
    }

    public boolean hasTransaction()
    {
        return currentTransaction != null;
    }

    public boolean hasImplicitTransaction()
    {
        return hasTransaction() && currentTransaction.transactionType() == KernelTransaction.Type.implicit;
    }
}
