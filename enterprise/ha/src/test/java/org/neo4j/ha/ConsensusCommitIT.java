/**
 * Copyright (c) 2002-2015 "Neo Technology,"
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
package org.neo4j.ha;

import org.junit.Rule;
import org.junit.Test;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.schema.ConstraintDefinition;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.kernel.ha.HaSettings;
import org.neo4j.kernel.ha.HighlyAvailableGraphDatabase;
import org.neo4j.test.ha.ClusterManager;
import org.neo4j.test.ha.ClusterRule;

import static org.junit.Assert.assertEquals;

public class ConsensusCommitIT
{
    @Rule
    public ClusterRule clusterRule = new ClusterRule( getClass() );

    @Test
    public void shouldCommitSimpleTransactionWithConsensus() throws Exception
    {
        // given
        clusterRule.config( HaSettings.consensus_commit, "true" );

        ClusterManager.ManagedCluster cluster = clusterRule.startCluster();
        HighlyAvailableGraphDatabase slave = cluster.getAnySlave();

        long id;
        try ( Transaction tx = slave.beginTx() )
        {
            id = slave.createNode().getId();
            tx.success();
        }

        // TODO: Do we need a delay here or should we allow a time for the transaction of propagating to all members?

        Iterable<HighlyAvailableGraphDatabase> members = cluster.getAllMembers();
        for ( HighlyAvailableGraphDatabase member : members )
        {
            try ( Transaction tx = member.beginTx() )
            {
                member.getNodeById( id );
                tx.success();
            }
        }
    }

    @Test
    public void shouldCommitLargeTransactionWithConsensus() throws Exception
    {
        final int NBR_NODES = 8000; // TODO: Much larger transactions can't be handled because of Netty limit. Need another mechanism for sending transactions?

        // given
        clusterRule.config( HaSettings.consensus_commit, "true" );

        ClusterManager.ManagedCluster cluster = clusterRule.startCluster();
        HighlyAvailableGraphDatabase slave = cluster.getAnySlave();

        long[] ids = new long[NBR_NODES];
        try ( Transaction tx = slave.beginTx() )
        {
            for ( int i = 0; i < NBR_NODES; i++ )
            {
                ids[i] = slave.createNode().getId();
            }
            tx.success();
        }

        // TODO: Do we need a delay here or perhaps allow a time for transaction propagate to all members? If not, why?

        Iterable<HighlyAvailableGraphDatabase> members = cluster.getAllMembers();
        for ( HighlyAvailableGraphDatabase member : members )
        {
            if ( member.equals( slave ) )
            {
                // We handle this one last, because this test wants to stress the other nodes as quickly as possibly.
                continue;
            }

            try ( Transaction tx = member.beginTx() )
            {
                for ( int i = 0; i < NBR_NODES; i++ )
                {
                    member.getNodeById( ids[i] );
                }
                tx.success();
            }
        }

        try ( Transaction tx = slave.beginTx() )
        {
            for ( int i = 0; i < NBR_NODES; i++ )
            {
                slave.getNodeById( ids[i] );
            }
            tx.success();
        }
    }

    @Test
    public void shouldCommitConstraintWithConsensus() throws Exception
    {
        // given
        clusterRule.config( HaSettings.consensus_commit, "true" );

        ClusterManager.ManagedCluster cluster = clusterRule.startCluster();
        HighlyAvailableGraphDatabase master = cluster.getMaster();

        ConstraintDefinition originalConstraint;
        try ( Transaction tx = master.beginTx() )
        {
            originalConstraint = master.schema().constraintFor( DynamicLabel.label( "label" ) ).
                    assertPropertyIsUnique( "key" ).create();

            tx.success();
        }

        Iterable<HighlyAvailableGraphDatabase> members = cluster.getAllMembers();
        for ( HighlyAvailableGraphDatabase member : members )
        {
            try ( Transaction tx = member.beginTx() )
            {
                ConstraintDefinition constraint = IteratorUtil.single( member.schema().getConstraints().iterator() );
                assertEquals( constraint, originalConstraint );

                tx.success();
            }
        }
    }
}
