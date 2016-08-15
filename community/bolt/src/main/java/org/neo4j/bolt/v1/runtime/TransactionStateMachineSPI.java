package org.neo4j.bolt.v1.runtime;

import java.util.Map;

import org.neo4j.bolt.v1.runtime.spi.StatementRunner;
import org.neo4j.graphdb.Result;
import org.neo4j.kernel.api.KernelTransaction;
import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.security.AuthSubject;
import org.neo4j.kernel.impl.core.ThreadToStatementContextBridge;
import org.neo4j.kernel.impl.query.QueryExecutionEngine;
import org.neo4j.kernel.impl.query.QueryExecutionKernelException;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

class TransactionStateMachineSPI implements TransactionStateMachine.SPI
{
    private final GraphDatabaseAPI db;
    private final ThreadToStatementContextBridge txBridge;
    private final QueryExecutionEngine queryExecutionEngine;
    private final StatementRunner statementRunner;

    TransactionStateMachineSPI( GraphDatabaseAPI db,
                                ThreadToStatementContextBridge txBridge,
                                QueryExecutionEngine queryExecutionEngine,
                                StatementRunner statementRunner )
    {
        this.db = db;
        this.txBridge = txBridge;
        this.queryExecutionEngine = queryExecutionEngine;
        this.statementRunner = statementRunner;
    }

    @Override
    public KernelTransaction beginTransaction( AuthSubject authSubject )
    {
        db.beginTransaction( KernelTransaction.Type.explicit, authSubject );
        return txBridge.getKernelTransactionBoundToThisThread( false );
    }

    @Override
    public void bindTransactionToCurrentThread( KernelTransaction tx )
    {
        txBridge.bindTransactionToCurrentThread( tx );
    }

    @Override
    public void unbindTransactionFromCurrentThread()
    {
        txBridge.unbindTransactionFromCurrentThread();
    }

    @Override
    public boolean isPeriodicCommit( String query )
    {
        return queryExecutionEngine.isPeriodicCommit( query );
    }

    @Override
    public Result executeQuery( AuthSubject authSubject,
                                String statement,
                                Map<String, Object> params ) throws QueryExecutionKernelException
    {
        try
        {
            return statementRunner.run( authSubject, statement, params );
        }
        catch ( KernelException e )
        {
            throw new QueryExecutionKernelException( e );
        }
    }

}
