package org.neo4j.server.rest.transactional;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatementResultTest
{
    @Test
    public void shouldCalculateExecutionTime() throws Exception
    {
        // given
        StatementResult statementResult = new StatementResult( 1234, null );

        // when
        statementResult.setEndTime( 5678 );

        // then
        assertEquals( 4444, statementResult.executionTime() );
    }
}