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
package org.neo4j.desktop.config.osx;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.neo4j.desktop.config.unix.UnixEnvironment;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;

class DarwinEnvironment extends UnixEnvironment
{
    @Override
    public void openCommandPrompt( File binDirectory, File jreBinDirectory, File workingDirectory ) throws IOException
    {
        File script = File.createTempFile( "neo4j", "sh" );
        try ( PrintWriter writer = new PrintWriter( script ) )
        {
            writer.println( "#!/bin/bash" );
            writer.println( format( "export PATH=\"%s:%s:${PATH}\"", jreBinDirectory, binDirectory ) );
            writer.println( String.format( "export REPO=\"%s\"", binDirectory ) );
            writer.println( "echo" );
            writer.println( "echo Neo4j Command Prompt" );
            writer.println( "echo" );
            writer.println( "echo \"This window is configured with Neo4j on the path.\"" );
            writer.println( "echo" );
            writer.println( "echo \"Available commands:\"" );
            writer.println( "echo \"* neo4j-shell\"" );
            writer.println( "echo \"* neo4j-import\"" );
            writer.println( "bash" );
        }
        System.out.println( "script = " + script );
        String[] cmdArray1 = {
                "chmod",
                "+x",
                script.getAbsolutePath()
        };
        getRuntime().exec( cmdArray1 );
        String[] cmdArray2 = {
                "open",
                "-a",
                "Terminal",
                script.getAbsolutePath()
        };
        getRuntime().exec( cmdArray2 );
    }
}
