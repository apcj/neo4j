###!
Copyright (c) 2002-2014 "Neo Technology,"
Network Engine for Objects in Lund AB [http://neotechnology.com]

This file is part of Neo4j.

Neo4j is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
###

'use strict'

angular.module('neo4jApp.controllers')
  .controller 'SketchCtrl', ['$rootScope', '$scope', '$element', 'GraphStyle', 'Editor', ($rootScope, $scope, $element, GraphStyle, Editor) ->

    measureSize = ->
      width: $element.width()
      height: $element.height()

    $scope.sketchGraph = do ->
      graph = new neo.models.Graph()
      node = new neo.models.Node(0, [], {})
      node.x = 100
      node.y = 100
      graph.addNodes([node])
      graph

    emitCode = (graph) ->
      cypher = ''
      for node in graph.nodes()
        cypher += "CREATE (n#{node.id})\n"
      for rel in graph.relationships()
        cypher += "CREATE (n#{rel.source.id})-[:#{rel.type}]->(n#{rel.target.id})\n"
      Editor.setContent(cypher)

    @render = (graph) ->
      graphView = new neo.graphView($element[0], measureSize, graph, GraphStyle)
      graphView
      .on('nodeDblClicked', (clickedNode) ->
        newId = (collection) ->
          (d3.max(collection, (d) -> d.id) + 1) or 0
        newNode = new neo.models.Node(newId(graph.nodes()), [], {})
        newNode.x = 100
        newNode.y = 100
        graph.addNodes([newNode])
        graph.addRelationships([new neo.models.Relationship(newId(graph.relationships()), clickedNode, newNode, 'LINK', {})])
        graphView.update()
        emitCode(graph)
      )
      graphView.update()
      emitCode(graph)
]
