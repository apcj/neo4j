package org.neo4j.coreedge.discovery;

import org.neo4j.helpers.AdvertisedSocketAddress;
import org.neo4j.kernel.configuration.Config;

import static org.neo4j.graphdb.factory.GraphDatabaseSettings.boltConnectors;

class ConnectorAddresses
{
    static AdvertisedSocketAddress extractBoltAddress( Config config )
    {
        return boltConnectors( config ).stream().findFirst()
                .map( boltConnector -> config.get( boltConnector.advertised_address ) ).orElseThrow( () ->
                        new IllegalArgumentException( "A Bolt connector must be configured to run a cluster" ) );
    }
}
