/*
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.locking;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * State control class for Locks.Clients.
 * Client state represent current Locks.Client state: OPEN/CLOSED
 * and number of active clients
 */
public final class LockClientStateHolder
{
    private static final int FLAG_BITS = 1;
    private static final int CLIENT_BITS = Integer.SIZE - FLAG_BITS;
    private static final int STATE_BIT_MASK = 1 << CLIENT_BITS;
    private static final int CLOSED = 1 << CLIENT_BITS;
    private static final int INITIAL_STATE = 0;
    private AtomicInteger clientState = new AtomicInteger( INITIAL_STATE );

    /**
     * Check if we still have any active client
     * @return true if have any open client, false otherwise.
     */
    public boolean hasActiveClients()
    {
        return getActiveClients( clientState.get() ) > 0;
    }

    /**
     * Closing current client
     */
    public void closeClient()
    {
        int currentValue;
        do
        {
            currentValue = clientState.get();
        }
        while ( !clientState.compareAndSet( currentValue, stateWithNewStatus( currentValue, CLOSED ) ) );
    }

    /**
     * Increment active number of clients that use current state instance.
     * @return false if already closed and not possible to increment active clients counter, true in case if counter
     * was successfully incremented.
     */
    public boolean incrementActiveClients()
    {
        int currentState;
        do
        {
            currentState = clientState.get();
            if ( isClosed( currentState ) )
            {
                return false;
            }
        }
        while ( !clientState.compareAndSet( currentState, statusWithUpdatedClients( currentState, 1 ) ) );
        return true;
    }

    /**
     * Decrement number of active clients that use current client state object.
     */
    public void decrementActiveClients()
    {
        int currentState;
        do
        {
            currentState = clientState.get();
        }
        while ( !clientState.compareAndSet( currentState, statusWithUpdatedClients( currentState, -1 ) ) );
    }

    /**
     * Check if closed
     * @return true if client is closed, false otherwise
     */
    public boolean isClosed()
    {
        return isClosed( clientState.get() );
    }

    /**
     * Reset state to initial state disregard any current state or number of active clients
     */
    public void reset()
    {
        clientState.set( INITIAL_STATE );
    }

    private boolean isClosed( int clientState )
    {
        return getStatus( clientState ) == CLOSED;
    }

    private int getStatus( int clientState )
    {
        return clientState & STATE_BIT_MASK;
    }

    private int getActiveClients( int clientState )
    {
        return clientState & ~STATE_BIT_MASK;
    }

    private int stateWithNewStatus( int clientState, int newStatus )
    {
        return newStatus | getActiveClients( clientState );
    }

    private int statusWithUpdatedClients( int clientState, int delta )
    {
        return getStatus( clientState ) | (getActiveClients( clientState ) + delta);
    }
}