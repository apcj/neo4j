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
package org.neo4j.metrics.source.causalclustering;

import java.util.concurrent.atomic.LongAdder;

import org.neo4j.causalclustering.catchup.tx.TxApplicationMonitor;

class TxApplicationMetric implements TxApplicationMonitor
{
    private LongAdder transactions = new LongAdder(  );
    private LongAdder batches = new LongAdder(  );

    @Override
    public void onTransactionQueuedForApplication()
    {
        transactions.increment();
    }

    @Override
    public void onBatchEmptied()
    {
        batches.increment();
    }

    @Override
    public long transactionsQueuedForApplication()
    {
        return transactions.longValue();
    }

    @Override
    public long transactionApplicationBatchesEmptied()
    {
        return batches.longValue();
    }
}
