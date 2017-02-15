<%-- 
    Document   : graph_conn_search
    Created on : 03-May-2013, 14:44:51
    Author     : Sac

Backend interface. It makes possible to find paths between rdf nodes.

Example request: 

graph_conn_search.jsp?urls={%22urls%22:[%22http://lod.sztaki.hu/sztaki/auth/008002215%22,%22http://lod.sztaki.hu/sztaki/auth/008003209%22]}&nodes_max=100&path_max=5

Request parameters: 

- urls
The urls to find shortest path between them. It must be in json format and must contain an array identified by
the urls key. An example json object is:

{
    "urls": [
        "http: //lod.sztaki.hu/sztaki/auth/008002215",
        "http: //lod.sztaki.hu/sztaki/auth/008003209"
    ]
}

The number of the urls in the request is arbitrary, but the algorithm will stop at the iteration where it finds
a node with paths to any two of the referred nodes.

- nodes_max
Integer value. The maximum number of nodes given back by the backend. These any node from the resulting graph 
will be connected to at least two input nodes with continous not overlapping paths.

- path_max
Longer paths than the given one will not be requested. The maximum path-length is 6. Because of the not transitive
links between nodes typically found in interconnected lod services, it is not quarantied that all of the paths
with the given links will be checked by the algorithm.


Path finding algorithm and limitations
The algorithm grows several sub-graphs from the given starting points. Each iteration means the process of collecting and downloading all resources found on the connnections of the actual leaf nodes of the sub-graphs.
At the end of each iterations it is checked if a node is found which is connected to more than one sub-graphs.
The algorithm stops if:
- such node(s) is/are found
- the maximum number of iterations is reached (it is the half of the path_max parameter)
The algorithm stops immedaitely if more than 10000 nodes were requested.

--%>
<%@page import="com.fasterxml.jackson.core.JsonParseException"%>
<%@page import="hu.mta.sztaki.lod.GraphUtils"%>
<%@page import="hu.mta.sztaki.lod.PathFinder"%>
<%@page import="java.util.ArrayList"%>
<%@page import="hu.mta.sztaki.lod.Node"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.fasterxml.jackson.databind.JsonNode"%>
<%@page import="com.fasterxml.jackson.databind.ObjectMapper"%>
<%@page import="java.util.logging.Level"%>
<%@page import="java.util.logging.Logger"%>
<%@page contentType="application/json; charset=utf-8" pageEncoding="UTF-8"%>
<%
    request.setCharacterEncoding("utf-8");
    String reqUrlsJsonStr = request.getParameter("urls");
    int pathMax = 0;
    int nodes_max = 0;

    try {
        pathMax = Integer.parseInt(request.getParameter("path_max"));
        nodes_max = Integer.parseInt(request.getParameter("nodes_max"));
    } catch (NumberFormatException e) {
        response.setStatus(400);
        return;
    }

    Logger.getLogger("graph_conn_search.jsp").log(Level.INFO, "Requested: " + reqUrlsJsonStr + " (" + pathMax + "," + nodes_max + ")");
    // The allowed maximum path is 6 (3 iteration from all endpoints).
    if (pathMax > 6) {
        pathMax = 6;
    }

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode;
    try {
        rootNode = mapper.readValue(reqUrlsJsonStr, JsonNode.class);
    } catch (NullPointerException e) {
        Logger.getLogger("graph_conn_search.jsp").log(Level.SEVERE, "NullPointerException - 400");
        response.setStatus(400);
        return;
    } catch (JsonParseException e) {
        Logger.getLogger("graph_conn_search.jsp").log(Level.SEVERE, "JsonParseException - 400");
        response.setStatus(400);
        return;
    }
    JsonNode urlsJsonNode = rootNode.get("urls");


    HashMap<String, Node> actualIterationGraph; // Set of nodes issued for content download

    String[] rootNodes = null;
    int startNodesCount = 0;

    if (urlsJsonNode.isArray()) {
        startNodesCount = urlsJsonNode.size();

        rootNodes = new String[startNodesCount];
        actualIterationGraph = new HashMap<String, Node>();

        for (int i = 0; i < startNodesCount; i++) {
            // Add the urls given by the user to the actual iteration graph
            // (the parents of these nodes will become themselves)
            String actRootNode = urlsJsonNode.get(i).textValue();
            rootNodes[i] = actRootNode;

            if (actualIterationGraph.get(actRootNode) != null) {
                // The starting URLs must difere
                Logger.getLogger("graph_conn_search.jsp").log(Level.SEVERE, "Duplicate startNode url - 400");
                response.setStatus(400);
                return;
            }

            Node newNode = new Node(actRootNode);
            ArrayList<String>[] nodeParents = new ArrayList[startNodesCount];
            nodeParents[i] = new ArrayList<String>();
            nodeParents[i].add(actRootNode);
            newNode.setParents(nodeParents);
            newNode.distance = new int[startNodesCount];
            for (int j = 0; j < startNodesCount; j++) {
                newNode.distance[j] = -1;
            }
            newNode.distance[i] = 0;
            actualIterationGraph.put(newNode.resource_id, newNode);
        }
        PathFinder fp = new PathFinder(actualIterationGraph, pathMax, nodes_max);
        fp.findShortestPath();
        HashMap<String, Node> retval;
        retval = fp.cleanupResultGraph();


        if (request.getParameter("callback") != null) {
            out.print(request.getParameter("callback") + "(" + GraphUtils.jsonStrFromGraph(retval, null, rootNodes) + ");");
        } else {
            out.print(GraphUtils.jsonStrFromGraph(retval, null, rootNodes));
        }

        Logger.getLogger("graph_conn_search.jsp").log(Level.INFO, "FINISHED " + reqUrlsJsonStr + " (" + pathMax + "," + nodes_max + ")");
    } else {
        // TODO: error handling
    }
%>
