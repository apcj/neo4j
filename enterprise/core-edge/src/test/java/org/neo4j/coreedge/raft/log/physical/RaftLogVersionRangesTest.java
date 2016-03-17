package org.neo4j.coreedge.raft.log.physical;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RaftLogVersionRangesTest
{
    @Test
    public void shouldBuildListOfFiles() throws Exception
    {
        RaftLogVersionRanges ranges = new RaftLogVersionRanges();
        assertEquals( -1, ranges.versionForIndex( 0 ) );
        assertEquals( -1, ranges.versionForIndex( 1 ) );

        ranges.add( new LogHeader( 0, -1 ) );
        assertEquals( 0, ranges.versionForIndex( 0 ) );
        assertEquals( 0, ranges.versionForIndex( Long.MAX_VALUE ) );

        ranges.add( new LogHeader( 1, 24 ) );
        assertEquals( 0, ranges.versionForIndex( 0 ) );
        assertEquals( 0, ranges.versionForIndex( 24 ) );
        assertEquals( 1, ranges.versionForIndex( 25 ) );
        assertEquals( 1, ranges.versionForIndex( Long.MAX_VALUE ) );

        ranges.add( new LogHeader( 2, 68 ) );
        assertEquals( 0, ranges.versionForIndex( 0 ) );
        assertEquals( 0, ranges.versionForIndex( 24 ) );
        assertEquals( 1, ranges.versionForIndex( 25 ) );
        assertEquals( 1, ranges.versionForIndex( 68 ) );
        assertEquals( 2, ranges.versionForIndex( 69 ) );
        assertEquals( 2, ranges.versionForIndex( Long.MAX_VALUE ) );

        ranges.add( new LogHeader( 3, 48 ) );
        assertEquals( 0, ranges.versionForIndex( 0 ) );
        assertEquals( 0, ranges.versionForIndex( 24 ) );
        assertEquals( 1, ranges.versionForIndex( 25 ) );
        assertEquals( 1, ranges.versionForIndex( 48 ) );
        assertEquals( 3, ranges.versionForIndex( 49 ) );
        assertEquals( 3, ranges.versionForIndex( 69 ) );
        assertEquals( 3, ranges.versionForIndex( Long.MAX_VALUE ) );

        ranges.add( new LogHeader( 4, 62 ) );
        assertEquals( 0, ranges.versionForIndex( 0 ) );
        assertEquals( 0, ranges.versionForIndex( 24 ) );
        assertEquals( 1, ranges.versionForIndex( 25 ) );
        assertEquals( 1, ranges.versionForIndex( 48 ) );
        assertEquals( 3, ranges.versionForIndex( 49 ) );
        assertEquals( 3, ranges.versionForIndex( 62 ) );
        assertEquals( 4, ranges.versionForIndex( 63 ) );
        assertEquals( 4, ranges.versionForIndex( 69 ) );
        assertEquals( 4, ranges.versionForIndex( Long.MAX_VALUE ) );

        ranges.add( new LogHeader( 5, 2 ) );
        assertEquals( 0, ranges.versionForIndex( 0 ) );
        assertEquals( 5, ranges.versionForIndex( 24 ) );
        assertEquals( 5, ranges.versionForIndex( 25 ) );
        assertEquals( 5, ranges.versionForIndex( 48 ) );
        assertEquals( 5, ranges.versionForIndex( 49 ) );
        assertEquals( 5, ranges.versionForIndex( 62 ) );
        assertEquals( 5, ranges.versionForIndex( 63 ) );
        assertEquals( 5, ranges.versionForIndex( 69 ) );
        assertEquals( 5, ranges.versionForIndex( Long.MAX_VALUE ) );
    }

    @Test
    public void shouldPruneEarlyRanges() throws Exception
    {
        RaftLogVersionRanges ranges = new RaftLogVersionRanges();

        ranges.add( new LogHeader( 0, 2 ) );
        ranges.add( new LogHeader( 1, 24 ) );
        ranges.add( new LogHeader( 2, 68 ) );

        ranges.pruneVersion( 1 );

        assertEquals( -1, ranges.versionForIndex( 0 ) );
        assertEquals( -1, ranges.versionForIndex( 68 ) );
        assertEquals( 2, ranges.versionForIndex( 69 ) );
    }

    @Test
    public void shouldPruneAllRanges() throws Exception
    {
        // given
        RaftLogVersionRanges ranges = new RaftLogVersionRanges();
        ranges.add( new LogHeader( 0, 2 ) );

        // when
        ranges.pruneVersion( 0 );

        // then
        assertEquals( -1, ranges.versionForIndex( 0 ) );
    }

    @Test
    public void shouldRejectAlreadyContainedVersion() throws Exception
    {
        RaftLogVersionRanges ranges = new RaftLogVersionRanges();

        ranges.add( new LogHeader( 0, 2 ) );
        ranges.add( new LogHeader( 1, 24 ) );
        ranges.add( new LogHeader( 2, 68 ) );

        assertThat( ranges, rejectsHeader( new LogHeader( 1, 2 ) ) );
        assertThat( ranges, rejectsHeader( new LogHeader( 2, 2 ) ) );
    }

    private Matcher<? super RaftLogVersionRanges> rejectsHeader( LogHeader logHeader )
    {
        return new TypeSafeMatcher<RaftLogVersionRanges>()
        {
            @Override
            protected boolean matchesSafely( RaftLogVersionRanges ranges )
            {
                try
                {
                    ranges.add( logHeader );
                    return false;
                }
                catch ( Exception e )
                {
                    return true;
                }
            }

            @Override
            public void describeTo( Description description )
            {
                description.appendText( "Ranges that rejects header: " ).appendValue( logHeader );
            }
        };
    }
}
