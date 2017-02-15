<%-- 
    Document   : graph_save
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
    PreparedStatement insertGraph = null;
    PreparedStatement oldGraph = null;
    PreparedStatement updateGraph = null;
    boolean justUpdate = false;
    try {
        conn = DriverManager.getConnection(properties.getProperty("mysql_host"), properties.getProperty("mysql_user"), properties.getProperty("mysql_pw"));
        int size = 1;
        stmt = conn.createStatement();

        oldGraph = conn.prepareStatement("SELECT graph_id FROM graphs WHERE user_name=? AND graph_name=?");
        oldGraph.setString(1, request.getParameter("user_name"));
        oldGraph.setString(2, request.getParameter("graph_name"));
        rs = oldGraph.executeQuery();
        if (rs.next()) {
            graph_id = rs.getString("graph_id");
            System.out.println("Old id found: " + graph_id);
            updateGraph = conn.prepareStatement("UPDATE graphs SET graph=? WHERE graph_id=?");
            updateGraph.setString(1, request.getParameter("graph"));
            updateGraph.setString(2, graph_id);
            updateGraph.execute();
        } else {

            do {
                graph_id = new BigInteger(130, new SecureRandom()).toString(32);
                rs = stmt.executeQuery("SELECT * FROM graphs WHERE graph_id='" + graph_id + "'");
            } while (rs.next());


            insertGraph = conn.prepareStatement("INSERT INTO graphs (graph_id, user_name, graph_name, graph) VALUES (?,?,?,?)");
            insertGraph.setString(1, graph_id);
            insertGraph.setString(2, request.getParameter("user_name"));
            insertGraph.setString(3, request.getParameter("graph_name"));
            insertGraph.setString(4, request.getParameter("graph"));
            insertGraph.execute();
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
    String retval = "{"
            + "\"graph_id\":\"" + graph_id + "\","
            + "\"graph_name\":\"" + request.getParameter("graph_name") + "\","
            + "\"graph_username\":\"" + request.getParameter("user_name") + "\""
            + "}";
    if (request.getParameter("callback") != null) {
        out.print(request.getParameter("callback") + "(" + retval + ");");
    } else {
        out.print(retval);
    }
%>
