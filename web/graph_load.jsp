<%-- 
    Document   : graph_load
    Created on : 16-Apr-2013, 15:45:29
    Author     : Sac
--%>
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
<%@ page contentType="application/json" pageEncoding="UTF-8"%>
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
    String graph_name = null;
    String graph_username = null;
    PreparedStatement selectGraph = null;
    String retval = null;
    try {
        conn = DriverManager.getConnection(properties.getProperty("mysql_host"), properties.getProperty("mysql_user"), properties.getProperty("mysql_pw"));
        int size = 1;
        stmt = conn.createStatement();

        if (request.getParameter("graph_id") != "") {
            selectGraph = conn.prepareStatement("SELECT graph, user_name, graph_name, graph_id FROM graphs WHERE graph_id=?");
            selectGraph.setString(1, request.getParameter("graph_id"));
            selectGraph.execute();
            rs = selectGraph.getResultSet();
        } else if (request.getParameter("user_name") != "" && request.getParameter("graph_name") != "") {
            selectGraph = conn.prepareStatement("SELECT graph, user_name, graph_name, graph_id FROM graphs WHERE user_name=? AND graph_name=?");
            selectGraph.setString(1, request.getParameter("user_name"));
            selectGraph.setString(2, request.getParameter("graph_name"));
            selectGraph.execute();
            rs = selectGraph.getResultSet();
        } else {
            rs = null;
        }


        System.out.println("incoming user_name: " + request.getParameter("user_name") + " graph_name: " + request.getParameter("graph_name") + " graph_id: " + request.getParameter("graph_id"));
        if (rs.next()) {
            retval = rs.getString("graph");
            graph_id = rs.getString("graph_id");
            graph_name = rs.getString("graph_name");
            graph_username = rs.getString("user_name");
            System.out.println("retval: \n" + retval);
        }
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
    if (request.getParameter("callback") != null) {
        out.print(request.getParameter("callback") + "({"
                + "\"graph\":" + retval + ","
                + "\"graph_id\":\"" + graph_id + "\","
                + "\"graph_name\":\"" + graph_name + "\","
                + "\"graph_username\":\"" + graph_username + "\""
                + "});");
    } else {
        out.print("{"
                + "\"graph\":" + retval + ","
                + "\"graph_id\":\"" + graph_id + "\","
                + "\"graph_name\":\"" + graph_name + "\","
                + "\"graph_username\":\"" + graph_username + "\""
                + "}");
    }
%>