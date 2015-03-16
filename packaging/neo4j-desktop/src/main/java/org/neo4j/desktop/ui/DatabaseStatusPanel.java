/**
 * Copyright (c) 2002-2015 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.desktop.ui;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static org.neo4j.desktop.ui.Components.createPanel;
import static org.neo4j.desktop.ui.Components.ellipsis;
import static org.neo4j.desktop.ui.Components.withBackground;
import static org.neo4j.desktop.ui.Components.withLayout;
import static org.neo4j.desktop.ui.Components.withTitledBorder;

public class DatabaseStatusPanel
{
    public static final Color STOPPED_COLOR = new Color( 1.0f, 0.5f, 0.5f );
    public static final Color CHANGING_COLOR = new Color( 1.0f, 1.0f, 0.5f );
    public static final Color STARTED_COLOR = new Color( 0.5f, 1.0f, 0.5f );

    public static JPanel createStatusPanel( DesktopModel model, final SystemOutDebugWindow debugWindow )
    {
        final CardLayout layout = new CardLayout();
        final JPanel panel = withLayout( layout, withTitledBorder( "Status", createPanel() ) );
        for ( DatabaseStatus status : DatabaseStatus.values() )
        {
            panel.add( status.name(), display( status, model ) );
        }

        panel.addMouseListener( new MouseAdapter()
        {
            @Override
            public void mouseClicked( MouseEvent e )
            {
                if ( MouseEvent.BUTTON1 == e.getButton() && e.isAltDown() )
                {
                    debugWindow.show();
                }
            }
        } );
        model.register( new DesktopModelListener()
        {
            @Override
            public void desktopModelChanged( DesktopModel model )
            {
                layout.show( panel, model.getDatabaseStatus().name() );
            }
        } );
        return panel;
    }

    public static Component display( DatabaseStatus status, DesktopModel model )
    {
        switch ( status )
        {
            case STOPPED:
                return createStatusDisplay( STOPPED_COLOR,
                        new JLabel( "Choose a graph database directory, then start the server" ) );
            case STARTING:
                return createStatusDisplay( CHANGING_COLOR,
                        new JLabel( ellipsis( "In just a few seconds, Neo4j will be ready" ) ) );
            case STARTED:
                final JLabel link = new JLabel( "http://localhost:7474/" );

                model.register( new DesktopModelListener()
                {
                    @Override
                    public void desktopModelChanged( DesktopModel changedModel )
                    {
                        link.setText( "http://localhost:" + changedModel.getServerPort() + "/" );
                    }
                } );

                link.setFont( Components.underlined( link.getFont() ) );
                link.addMouseListener( new OpenBrowserMouseListener( link, model ) );

                return createStatusDisplay( STARTED_COLOR,
                        new JLabel( "Neo4j is ready. Browse to " ), link );
            case STOPPING:
                return createStatusDisplay( CHANGING_COLOR,
                        new JLabel( ellipsis( "Neo4j is shutting down" ) ) );
            default:
                throw new IllegalStateException();
        }
    }

    private static JPanel createStatusDisplay( Color color, Component... components )
    {
        return withBackground( color, withLayout( new FlowLayout(), createPanel( components ) ) );
    }
}
