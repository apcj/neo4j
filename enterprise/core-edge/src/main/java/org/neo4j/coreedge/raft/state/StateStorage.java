/*
 * Copyright (C) 2012 Neo Technology
 * All rights reserved
 */
package org.neo4j.coreedge.raft.state;

import java.io.IOException;

public interface StateStorage<STATE>
{
    STATE getInitialState();

    void persistStoreData( STATE state ) throws IOException;
}
