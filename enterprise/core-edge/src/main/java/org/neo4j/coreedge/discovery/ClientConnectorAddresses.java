package org.neo4j.coreedge.discovery;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.helpers.AdvertisedSocketAddress;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.server.configuration.ClientConnectorSettings.HttpConnector.Encryption;

import static org.neo4j.graphdb.factory.GraphDatabaseSettings.boltConnectors;
import static org.neo4j.server.configuration.ClientConnectorSettings.httpConnector;

public class ClientConnectorAddresses
{
    public ClientConnectorAddresses( AdvertisedSocketAddress boltAddress, AdvertisedSocketAddress httpAddress,
                                     Optional<AdvertisedSocketAddress> httpsAddress )
    {
        this.boltAddress = boltAddress;
        this.httpAddress = httpAddress;
        this.httpsAddress = httpsAddress;
    }

    static ClientConnectorAddresses extractFromConfig( Config config )
    {
        AdvertisedSocketAddress boltAddress = boltConnectors( config ).stream().findFirst()
                .map( boltConnector -> config.get( boltConnector.advertised_address ) ).orElseThrow( () ->
                        new IllegalArgumentException( "A Bolt connector must be configured to run a cluster" ) );

        AdvertisedSocketAddress httpAddress = config.get( httpConnector( config, Encryption.NONE ).orElseThrow( () ->
                new IllegalArgumentException( "An HTTP connector must be configured to run the server" ) )
                .advertised_address );

        Optional<AdvertisedSocketAddress> httpsAddress = httpConnector( config, Encryption.NONE )
                .map( (connector) -> config.get( connector.advertised_address ) );

        return new ClientConnectorAddresses( boltAddress, httpAddress, httpsAddress );
    }

    private final AdvertisedSocketAddress boltAddress;
    private final AdvertisedSocketAddress httpAddress;
    private final Optional<AdvertisedSocketAddress> httpsAddress;

    public AdvertisedSocketAddress getBoltAddress()
    {
        return boltAddress;
    }

    @Override
    public String toString()
    {
        String collect = Stream.of( Optional.of( boltAddress ), Optional.of( httpAddress ), httpsAddress )
                .filter( Optional::isPresent )
                .map (x -> x.get().toString())
                .collect( Collectors.joining( "," );

        return collect;
    }
}
