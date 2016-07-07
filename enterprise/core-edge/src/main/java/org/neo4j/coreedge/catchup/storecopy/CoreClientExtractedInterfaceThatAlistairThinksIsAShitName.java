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
package org.neo4j.coreedge.catchup.storecopy;

import org.neo4j.coreedge.catchup.storecopy.edge.StoreFileStreamingCompleteListener;
import org.neo4j.coreedge.catchup.tx.edge.NoSuchTransactionListener;
import org.neo4j.coreedge.catchup.tx.edge.TxPullResponseListener;
import org.neo4j.coreedge.catchup.tx.edge.TxStreamCompleteListener;
import org.neo4j.coreedge.server.CoreMember;

public interface CoreClientExtractedInterfaceThatAlistairThinksIsAShitName
{
    void pollForTransactions( CoreMember serverAddress, long lastTransactionId );

    void addTxPullResponseListener( TxPullResponseListener listener );

    void removeTxPullResponseListener( TxPullResponseListener listener );

    void addStoreFileStreamingCompleteListener( StoreFileStreamingCompleteListener listener );

    void removeStoreFileStreamingCompleteListener( StoreFileStreamingCompleteListener listener );

    void addTxStreamCompleteListener( TxStreamCompleteListener listener );

    void removeTxStreamCompleteListener( TxStreamCompleteListener listener );

    void addNoSuchTransactionListener( NoSuchTransactionListener listener );

    void removeNoSuchTransactionListener( NoSuchTransactionListener listener );
}
