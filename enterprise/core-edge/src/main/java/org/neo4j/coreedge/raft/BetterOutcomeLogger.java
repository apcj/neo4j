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
package org.neo4j.coreedge.raft;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.neo4j.coreedge.raft.outcome.Outcome;
import org.neo4j.coreedge.raft.state.RaftState;

import static java.lang.String.format;
import static java.lang.String.valueOf;

public class BetterOutcomeLogger implements OutcomeLogger
{
    private PrintWriter printWriter;
    private DateFormat dateFormat = new SimpleDateFormat( "HH:mm:ss.SSS" );

    public BetterOutcomeLogger( PrintWriter printWriter )
    {
        this.printWriter = printWriter;
    }

    @Override
    public void info( RaftState state, RaftMessages.Message message, Outcome outcome )
    {
        printWriter.println( format( "%s -->%s",
                dateFormat.format( new Date() ),
                state) );

        printWriter.println( format( "%s -->%s: %s",
                dateFormat.format( new Date() ),
                message.getClass().getSimpleName(),
                valueOf( message )) );

        printWriter.println( format( "%s -->%s",
                dateFormat.format( new Date() ),
                outcome.logString() ) );
        printWriter.println("-----");
        printWriter.flush();
    }
}
