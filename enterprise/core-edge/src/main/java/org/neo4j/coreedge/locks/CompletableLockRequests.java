/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.coreedge.locks;

import java.util.Optional;

import org.neo4j.kernel.DeadlockDetectedException;

public interface CompletableLockRequests
{
    Optional<CompletableLockRequest> retrieve( LockSession session );

    interface CompletableLockRequest
    {

        void lockAcquired();

        void lockNotAvailable();

        void failWithDeadlock( DeadlockDetectedException e );
    }
}
