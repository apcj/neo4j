package org.neo4j.coreedge.raft.log.physical;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import static org.neo4j.coreedge.raft.log.physical.VersionIndexRange.OUT_OF_RANGE;

public class VersionIndexRangesTest
{
    @Test
    public void shouldBuildListOfFiles() throws Exception
    {
        VersionIndexRanges ranges = new VersionIndexRanges();
        assertEquals( OUT_OF_RANGE, ranges.versionForIndex( 0 ) );
        assertEquals( OUT_OF_RANGE, ranges.versionForIndex( 1 ) );

        ranges.add( 0, -1 );
        assertEquals( 0, ranges.versionForIndex( 0 ).version );
        assertEquals( 0, ranges.versionForIndex( Long.MAX_VALUE ).version );

        ranges.add( 1, 24 );
        assertEquals( 0, ranges.versionForIndex( 0 ).version );
        assertEquals( 0, ranges.versionForIndex( 24 ).version );
        assertEquals( 1, ranges.versionForIndex( 25 ).version );
        assertEquals( 1, ranges.versionForIndex( Long.MAX_VALUE ).version );

        ranges.add( 2, 68 );
        assertEquals( 0, ranges.versionForIndex( 0 ).version );
        assertEquals( 0, ranges.versionForIndex( 24 ).version );
        assertEquals( 1, ranges.versionForIndex( 25 ).version );
        assertEquals( 1, ranges.versionForIndex( 68 ).version );
        assertEquals( 2, ranges.versionForIndex( 69 ).version );
        assertEquals( 2, ranges.versionForIndex( Long.MAX_VALUE ).version );

        ranges.add( 3, 48 );
        assertEquals( 0, ranges.versionForIndex( 0 ).version );
        assertEquals( 0, ranges.versionForIndex( 24 ).version );
        assertEquals( 1, ranges.versionForIndex( 25 ).version );
        assertEquals( 1, ranges.versionForIndex( 48 ).version );
        assertEquals( 3, ranges.versionForIndex( 49 ).version );
        assertEquals( 3, ranges.versionForIndex( 69 ).version );
        assertEquals( 3, ranges.versionForIndex( Long.MAX_VALUE ).version );

        ranges.add( 4, 62 );
        assertEquals( 0, ranges.versionForIndex( 0 ).version );
        assertEquals( 0, ranges.versionForIndex( 24 ).version );
        assertEquals( 1, ranges.versionForIndex( 25 ).version );
        assertEquals( 1, ranges.versionForIndex( 48 ).version );
        assertEquals( 3, ranges.versionForIndex( 49 ).version );
        assertEquals( 3, ranges.versionForIndex( 62 ).version );
        assertEquals( 4, ranges.versionForIndex( 63 ).version );
        assertEquals( 4, ranges.versionForIndex( 69 ).version );
        assertEquals( 4, ranges.versionForIndex( Long.MAX_VALUE ).version );

        ranges.add( 5, 2 );
        assertEquals( 0, ranges.versionForIndex( 0 ).version );
        assertEquals( 5, ranges.versionForIndex( 24 ).version );
        assertEquals( 5, ranges.versionForIndex( 25 ).version );
        assertEquals( 5, ranges.versionForIndex( 48 ).version );
        assertEquals( 5, ranges.versionForIndex( 49 ).version );
        assertEquals( 5, ranges.versionForIndex( 62 ).version );
        assertEquals( 5, ranges.versionForIndex( 63 ).version );
        assertEquals( 5, ranges.versionForIndex( 69 ).version );
        assertEquals( 5, ranges.versionForIndex( Long.MAX_VALUE ).version );
    }

    @Test
    public void shouldPruneEarlyRanges() throws Exception
    {
        VersionIndexRanges ranges = new VersionIndexRanges();

        ranges.add( 0, 2 );
        ranges.add( 1, 24 );
        ranges.add( 2, 68 );

        ranges.pruneVersion( 1 );

        assertEquals( OUT_OF_RANGE, ranges.versionForIndex( 0 ) );
        assertEquals( OUT_OF_RANGE, ranges.versionForIndex( 68 ) );
        assertEquals( 2, ranges.versionForIndex( 69 ).version );
    }

    @Test
    public void shouldPruneAllRanges() throws Exception
    {
        // given
        VersionIndexRanges ranges = new VersionIndexRanges();
        ranges.add( 0, 2 );

        // when
        ranges.pruneVersion( 0 );

        // then
        assertEquals( OUT_OF_RANGE, ranges.versionForIndex( 0 ) );
    }

    @Test
    public void shouldRejectAlreadyContainedVersion() throws Exception
    {
        VersionIndexRanges ranges = new VersionIndexRanges();

        ranges.add( 0, 2 );
        ranges.add( 1, 24 );
        ranges.add( 2, 68 );

        assertThat( ranges, rejectsVersion( 1, 2 ) );
        assertThat( ranges, rejectsVersion( 2, 2 ) );
    }

    private Matcher<? super VersionIndexRanges> rejectsVersion( long version, long prevIndex )
    {
        return new TypeSafeMatcher<VersionIndexRanges>()
        {
            @Override
            protected boolean matchesSafely( VersionIndexRanges ranges )
            {
                try
                {
                    ranges.add( version, prevIndex );
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
                description.appendText( "Ranges that rejects version: " ).appendValue( version );
            }
        };
    }
}
