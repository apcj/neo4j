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
package org.neo4j.cluster.protocol.commit;

import org.neo4j.cluster.com.message.Message;
import org.neo4j.cluster.com.message.MessageHolder;
import org.neo4j.cluster.statemachine.State;

/**
 * State machine for the Cluster API
 *
 * @see org.neo4j.cluster.protocol.cluster.Cluster
 * @see org.neo4j.cluster.protocol.cluster.ClusterMessage
 */
public enum CommitState
        implements State<CommitContext, CommitMessage>
{
    start
            {
                @Override
                public State<?, ?> handle( CommitContext context, Message<CommitMessage> message,
                                           MessageHolder outgoing ) throws Throwable
                {
                    switch ( message.getMessageType() )
                    {
                        case append:
                        {
                            System.out.println( "commit payload = " + message.getPayload() );
                            context.committed( message.<Commands>getPayload() );

                            break;
                        }

                    }
                    return this;
                }
            },
    propose
            {
                @Override
                public State<?, ?> handle( CommitContext commitContext, Message<CommitMessage> message,
                                           MessageHolder outgoing ) throws Throwable
                {
                    return start;
                }
            }

}
