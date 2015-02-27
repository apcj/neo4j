package org.neo4j.server.rest.transactional;

import org.neo4j.graphdb.Result;

public class StatementResult
{
    private Result result;
    private long startTime;
    private long endTime;

    public StatementResult( long startTime, Result result )
    {
        this.result = result;
        this.startTime = startTime;
    }

    public long executionTime()
    {
        return endTime - startTime;
    }

    public void setEndTime( long endTime )
    {
        this.endTime = endTime;
    }

    public Result result()
    {
        return result;
    }
}
