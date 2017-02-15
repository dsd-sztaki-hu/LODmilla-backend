<%-- 
    Document   : lod_resource_proxy
    Created on : 23-Apr-2013, 10:43:35
    Author     : Sac
--%>
<%@page import="hu.mta.sztaki.lod.GraphUtils"%>

<%@ page contentType="application/json; charset=utf-8" pageEncoding="UTF-8"%>
<%
    request.setCharacterEncoding("utf-8");

    try {
        String reqUrl = request.getParameter("url");
        System.out.println("Requested: " + reqUrl);
        
        String retval = GraphUtils.getJsonVersionForUri(reqUrl);

        if (request.getParameter("callback") != null) {
            out.print(request.getParameter("callback") + "(" + retval + ");");
        } else {
            out.print(retval);
        }

    } catch (Exception e) {
        System.out.println("exception happened: " + e);
        response.setStatus(500);
    }
%>
