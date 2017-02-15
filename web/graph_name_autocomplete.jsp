<%-- 
    Document   : graph_name_autocomplete
    Created on : 22-Apr-2013, 15:16:47
    Author     : Sac
--%>

<%@page import="org.apache.commons.lang3.StringEscapeUtils"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="java.sql.PreparedStatement"%>
<%@page import="java.security.SecureRandom"%>
<%@page import="java.math.BigInteger"%>
<%@page import="java.sql.ResultSet"%>
<%@page import="java.sql.Statement"%>
<%@page import="java.sql.SQLException"%>
<%@page import="java.sql.DriverManager"%>
<%@page import="java.sql.Connection"%>
<%@page import="java.io.IOException"%>
<%@page import="java.io.InputStream"%>
<%@page import="java.util.Properties"%>
<%

    Properties prop = new Properties();
    Properties properties = new Properties();
    properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("lodmillabackend.properties"));


    try {
        // The newInstance() call is a work around for some
        // broken Java implementations

        Class.forName("com.mysql.jdbc.Driver").newInstance();
    } catch (Exception ex) {
        // handle the error
    }

    Connection conn = null;
    Statement stmt = null;
    ResultSet rs = null;
    String graph_id = null;
    PreparedStatement selectGraph = null;
    //List<String> retval = new ArrayList<String>();
    String searchGraphName = "";
    if (request.getParameter("graph_name") != null && request.getParameter("graph_name") != "") {
        searchGraphName += request.getParameter("graph_name");
        System.out.println("searchGraphName " + searchGraphName);
    }
    String retvalStr = "";
    System.out.println("incoming user_name: " + request.getParameter("user_name") + " graph_name: " + request.getParameter("graph_name"));
    try {
        conn = DriverManager.getConnection(properties.getProperty("mysql_host"), properties.getProperty("mysql_user"), properties.getProperty("mysql_pw"));
        int size = 1;
        stmt = conn.createStatement();

        if (request.getParameter("user_name") != "") {
            selectGraph = conn.prepareStatement("SELECT graph_name FROM graphs WHERE user_name=? AND graph_name LIKE ?");
            selectGraph.setString(1, request.getParameter("user_name"));
            selectGraph.setString(2, searchGraphName + '%');
            selectGraph.execute();
            rs = selectGraph.getResultSet();
        } else {
            rs = null;
        }

        while (rs.next()) {
            String valueToAdd = rs.getString("graph_name");
            if (retvalStr != "") {
                retvalStr += ",";
            }
            retvalStr += "\"" + StringEscapeUtils.escapeEcmaScript(valueToAdd) + "\"";
            System.out.println("retval added: " + valueToAdd);
        }
        retvalStr = "{\"graph_names\":[" + retvalStr + "]}";

    } catch (SQLException ex) {
        // handle any errors
%>SQLException:<%= ex.getMessage()%><%
        System.out.println("SQLException: " + ex.getMessage());
        System.out.println("SQLState: " + ex.getSQLState());
        System.out.println("VendorError: " + ex.getErrorCode());
    } finally {
        // it is a good idea to release
        // resources in a finally{} block
        // in reverse-order of their creation
        // if they are no-longer needed

        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException sqlEx) {
            } // ignore

            rs = null;
        }

        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException sqlEx) {
            } // ignore

            stmt = null;
        }
    }
%>
<%@ page contentType="application/json" pageEncoding="UTF-8"%>
<%= request.getParameter("callback")%>(<%= retvalStr%>);