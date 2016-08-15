package org.neo4j.bolt.v1.runtime.bookmarking;

import org.neo4j.kernel.api.exceptions.KernelException;
import org.neo4j.kernel.api.exceptions.Status;

import static java.lang.String.format;

public class Bookmark
{
    private static final String BOOKMARK_TX_PREFIX = "neo4j:bookmark:v1:tx";

    private final long txId;

    public Bookmark( long txId )
    {
        this.txId = txId;
    }

    @Override
    public String toString()
    {
        return format( BOOKMARK_TX_PREFIX + "%d", txId );
    }

    public static Bookmark fromString( String bookmarkString) throws BookmarkFormatException
    {
        if ( bookmarkString != null && bookmarkString.startsWith( BOOKMARK_TX_PREFIX ) )
        {
            try
            {
                return new Bookmark( Long.parseLong( bookmarkString.substring( BOOKMARK_TX_PREFIX.length() ) ) );
            }
            catch ( NumberFormatException e )
            {
                throw new BookmarkFormatException( bookmarkString );
            }
        }
        throw new BookmarkFormatException( bookmarkString );
    }

    public long txId()
    {
        return txId;
    }

    static class BookmarkFormatException extends KernelException
    {
        BookmarkFormatException( String bookmarkString )
        {
            super( Status.Transaction.InvalidBookmark, "Supplied bookmark [%s] does not conform to pattern %s",
                    bookmarkString, BOOKMARK_TX_PREFIX );
        }
    }
}
