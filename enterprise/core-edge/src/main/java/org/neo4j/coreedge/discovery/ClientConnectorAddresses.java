package org.neo4j.coreedge.discovery;

import org.neo4j.helpers.AdvertisedSocketAddress;
import org.neo4j.kernel.configuration.Config;

import static org.neo4j.graphdb.factory.GraphDatabaseSettings.boltConnectors;

public class ClientConnectorAddresses
{
    public ClientConnectorAddresses( AdvertisedSocketAddress boltAddress )
    {
        this.boltAddress = boltAddress;
    }

    static ClientConnectorAddresses extractFromConfig( Config config )
    {
        AdvertisedSocketAddress boltAddress = boltConnectors( config ).stream().findFirst()
                .map( boltConnector -> config.get( boltConnector.advertised_address ) ).orElseThrow( () ->
                        new IllegalArgumentException( "A Bolt connector must be configured to run a cluster" ) );
        return new ClientConnectorAddresses( boltAddress );
    }

    private final AdvertisedSocketAddress boltAddress;

    public AdvertisedSocketAddress getBoltAddress()
    {
        return boltAddress;
    }

    @Override
    public String toString()
    {
        return boltAddress.toString();
    }
}
