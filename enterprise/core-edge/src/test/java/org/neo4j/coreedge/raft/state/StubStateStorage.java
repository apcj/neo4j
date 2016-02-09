package org.neo4j.coreedge.raft.state;

import java.io.IOException;

public class StubStateStorage<STATE> implements StateStorage<STATE>
{
    private final STATE state;

    public StubStateStorage( STATE state )
    {
        this.state = state;
    }

    @Override
    public STATE getInitialState()
    {
        return state;
    }

    @Override
    public void persistStoreData( STATE state ) throws IOException
    {

    }
}