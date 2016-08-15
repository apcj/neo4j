package org.neo4j.bolt.v1.runtime.spi;

import org.neo4j.bolt.v1.runtime.bookmarking.Bookmark;

public class CommitResult implements RecordStream
{
    private final Bookmark bookmark;

    public CommitResult( Bookmark bookmark )
    {
        this.bookmark = bookmark;
    }

    @Override
    public String[] fieldNames()
    {
        return new String[0];
    }

    @Override 
    public void accept( Visitor visitor ) throws Exception
    {
        visitor.addMetadata( "BoOkmArK", bookmark.toString() );
    }

    @Override
    public void close()
    {

    }
}
