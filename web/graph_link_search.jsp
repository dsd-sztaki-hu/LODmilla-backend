<%-- 
    Document   : graph_link_search
    Created on : 23-May-2013, 14:44:51
    Author     : Sac
--%>
<%@page import="hu.mta.sztaki.lod.LinkFinder"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Iterator"%>
<%@page import="hu.mta.sztaki.lod.GraphUtils"%>
<%@page import="java.util.HashMap"%>
<%@page import="com.fasterxml.jackson.core.JsonParser"%>
<%@page import="com.fasterxml.jackson.databind.JsonNode"%>
<%@page import="com.fasterxml.jackson.databind.ObjectMapper"%>
<%@page import="hu.mta.sztaki.lod.Node"%>
<%@page import="java.util.ArrayList"%>
<%@page contentType="application/json; charset=utf-8" pageEncoding="UTF-8"%>
<%
    request.setCharacterEncoding("utf-8");

    String reqUrlsJsonStr = request.getParameter("urls");
    int pathMax = Integer.parseInt(request.getParameter("path_max"));
    int nodes_max = Integer.parseInt(request.getParameter("nodes_max"));
    String search = request.getParameter("search");

    System.out.println("Requested: " + reqUrlsJsonStr + " (" + pathMax + "," + nodes_max
            + ") - " + search);
    
    search = search.toUpperCase();
    if (pathMax > 6) {
        pathMax = 6;
    }

    ObjectMapper mapper = new ObjectMapper();
    JsonNode rootNode = mapper.readValue(reqUrlsJsonStr, JsonNode.class);
    JsonNode urlsJsonNode = rootNode.get("urls");

    HashMap<String, Node> finalGraph = new HashMap();
    HashMap<String, Node> actualIterationGraph;
    String[] rootNodes = null;


    int startNodesCount = 0;

    if (urlsJsonNode.isArray()) {
        startNodesCount = urlsJsonNode.size();

        rootNodes = new String[startNodesCount];
        actualIterationGraph = new HashMap<String, Node>();

        for (int i = 0; i < startNodesCount; i++) {
            String actRootNode = urlsJsonNode.get(i).textValue();
            rootNodes[i] = actRootNode;

            if (actualIterationGraph.get(rootNodes[i]) != null) {
                throw new Exception("Duplicate startNode url");
            }

            Node newNode = new Node(rootNodes[i]);
            
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
        LinkFinder lf = new LinkFinder(rootNodes, finalGraph, actualIterationGraph, pathMax, nodes_max, search);
        lf.traverseLinks();
        
        if (request.getParameter("callback") != null) {
            out.print(request.getParameter("callback") + "(" + GraphUtils.jsonStrFromGraph(finalGraph, null, rootNodes) + ");");
        } else {
            out.print(GraphUtils.jsonStrFromGraph(finalGraph, null, rootNodes));
        }

        System.out.println("FINISHED");
    } else {
        // TODO
    }








%>
