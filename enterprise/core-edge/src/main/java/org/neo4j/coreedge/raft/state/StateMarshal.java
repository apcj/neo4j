/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.coreedge.raft.state;

public interface StateMarshal<STATE> extends ChannelMarshal<STATE>
{
    STATE startState();

    long ordinal( STATE state );
}