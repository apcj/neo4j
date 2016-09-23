package org.neo4j.server.configuration;

import java.util.Optional;

import org.neo4j.graphdb.config.Setting;
import org.neo4j.graphdb.factory.Description;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.helpers.AdvertisedSocketAddress;
import org.neo4j.helpers.ListenSocketAddress;
import org.neo4j.kernel.configuration.Config;

import static org.neo4j.graphdb.factory.GraphDatabaseSettings.Connector.ConnectorType.HTTP;
import static org.neo4j.kernel.configuration.GroupSettingSupport.enumerate;
import static org.neo4j.kernel.configuration.Settings.advertisedAddress;
import static org.neo4j.kernel.configuration.Settings.legacyFallback;
import static org.neo4j.kernel.configuration.Settings.listenAddress;
import static org.neo4j.kernel.configuration.Settings.options;
import static org.neo4j.kernel.configuration.Settings.setting;

public class ClientConnectorSettings
{
    public static HttpConnector httpConnector( String key )
    {
        return new HttpConnector( key );
    }

    public static Optional<HttpConnector> httpConnector( Config config, HttpConnector.Encryption encryption )
    {
        return config
                .view( enumerate( GraphDatabaseSettings.Connector.class ) )
                .map( HttpConnector::new )
                .filter( ( connConfig ) ->
                        config.get( connConfig.type ) == HTTP &&
                                config.get( connConfig.enabled ) &&
                                config.get( connConfig.encryption ) == encryption )
                .findFirst();
    }

    @Description("Configuration options for HTTP connectors. " +
            "\"(http-connector-key)\" is a placeholder for a unique name for the connector, for instance " +
            "\"http-public\" or some other name that describes what the connector is for.")
    public static class HttpConnector extends GraphDatabaseSettings.Connector
    {
        @Description("Enable TLS for this connector")
        public final Setting<Encryption> encryption;

        @Description( "Address the connector should bind to. " +
                "This setting is deprecated and will be replaced by `+listen_address+`" )
        public final Setting<ListenSocketAddress> address;

        @Description( "Address the connector should bind to" )
        public final Setting<ListenSocketAddress> listen_address;

        @Description( "Advertised address for this connector" )
        public final Setting<AdvertisedSocketAddress> advertised_address;

        public HttpConnector()
        {
            this( "(http-connector-key)" );
        }

        public HttpConnector( String key )
        {
            super( key, ConnectorType.HTTP.name() );
            encryption = group.scope( setting( "encryption", options( HttpConnector.Encryption.class ), HttpConnector.Encryption.NONE.name() ) );
            Setting<ListenSocketAddress> legacyAddressSetting = listenAddress( "address", 7474 );
            Setting<ListenSocketAddress> listenAddressSetting = legacyFallback( legacyAddressSetting,
                    listenAddress( "listen_address", 7474 ) );

            this.address = group.scope( legacyAddressSetting );
            this.listen_address = group.scope( listenAddressSetting );
            this.advertised_address = group.scope( advertisedAddress( "advertised_address", listenAddressSetting ) );
        }

        public enum Encryption
        {
            NONE, TLS
        }
    }
}
