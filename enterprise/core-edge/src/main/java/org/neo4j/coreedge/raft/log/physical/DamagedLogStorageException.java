package org.neo4j.coreedge.raft.log.physical;

import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.exceptions.Status;

public class DamagedLogStorageException extends KernelException
{
    protected DamagedLogStorageException( String message, Object... parameters )
    {
        super( Status.General.StorageDamageDetected, message, parameters );
    }
}
