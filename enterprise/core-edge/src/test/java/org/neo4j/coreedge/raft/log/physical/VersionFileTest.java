package org.neo4j.coreedge.raft.log.physical;

import java.io.File;

import org.junit.Test;

import static junit.framework.TestCase.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VersionFileTest
{
    @Test
    public void shouldThrowAnExceptionIfHeaderContentsDoNotMatchFile() throws Exception
    {
        // given
        HeaderReader headerReader = mock( HeaderReader.class );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( HeaderBuilder.version( 3 ).prevIndex( -1 )
                .prevTerm( -1 ) );

        VersionFile file = new VersionFile( 2, new File( "v2" ), HeaderReader.HEADER_LENGTH, headerReader );

        // when
        try
        {
            file.header();
            fail( "Should have thrown exception" );
        }
        catch ( DamagedLogStorageException e )
        {
            // then expected
        }

    }

    @Test
    public void shouldReturnHeader() throws Exception
    {
        // given
        HeaderReader headerReader = mock( HeaderReader.class );
        Header expectedHeader = HeaderBuilder.version( 2 ).prevIndex( -1 ).prevTerm( -1 );
        when( headerReader.readHeader( new File( "v2" ) ) ).thenReturn( expectedHeader );

        VersionFile file = new VersionFile( 2, new File( "v2" ), HeaderReader.HEADER_LENGTH, headerReader );

        // when
        Header header = file.header();

        // then
        assertSame( expectedHeader, header );
    }
}