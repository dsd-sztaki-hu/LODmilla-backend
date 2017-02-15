<%-- 
    Document   : graph_content_search
    Created on : 03-May-2013, 14:44:51
    Author     : Sac
--%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.Set"%>
<%@page import="java.util.Iterator"%>
<%@page import="hu.mta.sztaki.lod.ContentFinder"%>
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
            Node newNode;
            rootNodes[i] = urlsJsonNode.get(i).textValue();

            if (actualIterationGraph.get(rootNodes[i]) != null) {
                throw new Exception("Duplicate startNode url");
            }

            newNode = new Node(rootNodes[i]);
            ArrayList<String>[] nodeParents = new ArrayList[startNodesCount];
            /*for (int j = 0; j < startNodesCount; j++) {
             nodeParents[j] = new ArrayList<String>();
             }*/
            nodeParents[i] = new ArrayList<String>();
            nodeParents[i].add(rootNodes[i]);
            newNode.setParents(nodeParents);
            newNode.distance = new int[startNodesCount];
            for (int j = 0; j < startNodesCount; j++) {
                newNode.distance[j] = -1;
            }
            newNode.distance[i] = 0;

            actualIterationGraph.put(newNode.resource_id, newNode);
        }
        ContentFinder cf = new ContentFinder(rootNodes, finalGraph, actualIterationGraph, pathMax, search);
        HashMap<String, Node> retval = cf.findContent();
        
        Object[] retvalArray = retval.keySet().toArray();
        
        if (nodes_max > 0) {
            retval = cf.cleanupResultGraph(nodes_max);
        }

        if (request.getParameter("callback") != null) {
            out.print(request.getParameter("callback") + "(" + GraphUtils.jsonStrFromGraph(retval, Arrays.asList(retvalArray), rootNodes) + ");");
        } else {
            out.print(GraphUtils.jsonStrFromGraph(retval, Arrays.asList(retvalArray), rootNodes));
        }

        System.out.println("FINISHED");
    } else {
        // TODO
    }








%>
