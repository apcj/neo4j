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
package org.neo4j.coreedge.raft.log.physical;

import java.util.Objects;

public class Header
{
    public final long version;
    public final long prevIndex;
    public final long prevTerm;

    public Header( long version, long prevIndex, long prevTerm )
    {
        this.version = version;
        this.prevIndex = prevIndex;
        this.prevTerm = prevTerm;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }
        Header header = (Header) o;
        return Objects.equals( version, header.version ) &&
                Objects.equals( prevIndex, header.prevIndex ) &&
                Objects.equals( prevTerm, header.prevTerm );
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( version, prevIndex, prevTerm );
    }

    @Override public String toString()
    {
        return "Header{" +
                "version=" + version +
                ", prevIndex=" + prevIndex +
                ", prevTerm=" + prevTerm +
                '}';
    }
}
