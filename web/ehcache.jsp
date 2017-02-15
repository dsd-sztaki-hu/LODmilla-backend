<%-- 
    Document   : ehcache
    Created on : 23-May-2013, 16:24:48
    Author     : Sac
--%>
<%@page import="net.sf.ehcache.config.CacheConfiguration"%>
<%@page import="org.apache.commons.lang3.StringEscapeUtils"%>
<%@page import="net.sf.ehcache.Ehcache"%>
<%@page import="hu.mta.sztaki.lod.Node"%>
<%@page import="net.sf.ehcache.CacheManager"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>EHCACHE status</title>
    </head>
    <body>
        <h1>EHCACHE</h1>
        <%
            String cacheName = request.getParameter("cache");
            CacheManager cacheManager = Node.cacheManager;
            //out.println("<h2>Configuration</h2>");
            if (cacheName == null) {
                out.println("<pre>");
                out.println(StringEscapeUtils.escapeXml(cacheManager.getActiveConfigurationText()));
                out.println("</pre>");
                out.println("<h2>Caches</h2>");
                out.println("<ul>");
                String[] cachenames = cacheManager.getCacheNames();
                for (int i = 0; i < cachenames.length; i++) {
                    out.println("<li><a href=\"ehcache.jsp?cache=" + cachenames[i] + "\">" + cachenames[i] + "</li>");
                }
                out.println("</ul>");
            } else {

                out.println("<h2>Cache - " + cacheName + "</h3>");
                Ehcache ehcache = cacheManager.getEhcache(cacheName);
                if (request.getParameter("clearStatistics") != null) {
                    ehcache.clearStatistics();
                    out.println("<p>Operation executed: clearStatistics");
                    out.println("<p><a href=\"ehcache.jsp?cache=" + cacheName + "\">Back</a></p>");
                } //else if (request.getParameter("dispose") != null) {
                // Don't call it, it will shut down the cache!
                //ehcache.dispose();
                //out.println("<p>Operation executed: dispose");
                //out.println("<p><a href=\"ehcache.jsp?cache=" + cacheName + "\">Back</a></p>");
                //}
                else if (request.getParameter("evictExpiredElements") != null) {
                    ehcache.evictExpiredElements();
                    out.println("<p>Operation executed: evictExpiredElements</p>");
                    out.println("<p>Causes all elements stored in the Cache to be synchronously checked for expiry, and if expired, evicted.</p>");
                    out.println("<p><a href=\"ehcache.jsp?cache=" + cacheName + "\">Back</a></p>");
                } else if (request.getParameter("flush") != null) {
                    ehcache.flush();
                    out.println("<p>Operation executed: flush</p>");
                    out.println("<p>Flushes all cache items from memory to the disk store, and from the DiskStore to disk. </p>");
                    out.println("<p><a href=\"ehcache.jsp?cache=" + cacheName + "\">Back</a></p>");
                } else if (request.getParameter("removeAll") != null) {
                    ehcache.removeAll();
                    out.println("<p>Operation executed: removeAll</p>");
                    out.println("<p>Removes all cached items.</p>");
                    out.println("<p><a href=\"ehcache.jsp?cache=" + cacheName + "\">Back</a></p>");
                } else if (request.getParameter("setDisabled") != null) {
                    ehcache.setDisabled(true);
                    out.println("<p>Operation executed: setDisabled(true)</p>");
                    out.println("<p>Disables or enables this cache. This call overrides the previous value of disabled.</p>");
                    out.println("<p><a href=\"ehcache.jsp?cache=" + cacheName + "\">Back</a></p>");
                } else if (request.getParameter("setEnabled") != null) {
                    ehcache.setDisabled(false);
                    out.println("<p>Operation executed: setEnabled(false)</p>");
                    out.println("<p>Disables or enables this cache. This call overrides the previous value of disabled.</p>");
                    out.println("<p><a href=\"ehcache.jsp?cache=" + cacheName + "\">Back</a></p>");
                } else if (request.getParameter("setStatisticsEnabled") != null) {
                    ehcache.setStatisticsEnabled(true);
                    out.println("<p>Operation executed: setStatisticsEnabled(true)</p>");
                    out.println("<p>Enable/disable statistics collection. Enabling statistics does not have any effect on sampled statistics. To enable sampled statistics, use setSampledStatisticsEnabled(boolean) with parameter true. Disabling statistics also disables the sampled statistics collection if it is enabled.</p>");
                    out.println("<p><a href=\"ehcache.jsp?cache=" + cacheName + "\">Back</a></p>");
                } else if (request.getParameter("setStatisticsDisabled") != null) {
                    ehcache.setStatisticsEnabled(false);
                    out.println("<p>Operation executed: setStatisticsEnabled(false)</p>");
                    out.println("<p>Enable/disable statistics collection. Enabling statistics does not have any effect on sampled statistics. To enable sampled statistics, use setSampledStatisticsEnabled(boolean) with parameter true. Disabling statistics also disables the sampled statistics collection if it is enabled.</p>");
                    out.println("<p><a href=\"ehcache.jsp?cache=" + cacheName + "\">Back</a></p>");
                } else {
                    out.println("<p><a href=\"ehcache.jsp\">Back</a></p>");
                    out.println("<h4>Configuration</h4>");
                    CacheConfiguration cc = ehcache.getCacheConfiguration();
                    if (ehcache.getStatus().equals(net.sf.ehcache.Status.STATUS_ALIVE)) {
                        out.println("<p>ALIVE</p>");
                        out.println("<table border=\"1\">");
                        out.println("<tr><td><b>Disabled</b></td><td>" + ehcache.isDisabled() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalDiskAsString</b></td><td>" + cc.getMaxBytesLocalDiskAsString() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalHeapAsString</b></td><td>" + cc.getMaxBytesLocalHeapAsString() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalOffHeapAsString</b></td><td>" + cc.getMaxBytesLocalOffHeapAsString() + "</td></tr>");
                        out.println("<tr><td><b>DiskAccessStripes</b></td><td>" + cc.getDiskAccessStripes() + "</td></tr>");
                        out.println("<tr><td><b>DiskExpiryThreadIntervalSeconds</b></td><td>" + cc.getDiskExpiryThreadIntervalSeconds() + "</td></tr>");
                        out.println("<tr><td><b>Logging</b></td><td>" + cc.getLogging() + "</td></tr>");
                        out.println("<tr><td><b>DiskSpoolBufferSizeMB</b></td><td>" + cc.getDiskSpoolBufferSizeMB() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalDisk</b></td><td>" + cc.getMaxBytesLocalDisk() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalDiskPercentage</b></td><td>" + cc.getMaxBytesLocalDiskPercentage() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalHeap</b></td><td>" + cc.getMaxBytesLocalHeap() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalHeapPercentage</b></td><td>" + cc.getMaxBytesLocalHeapPercentage() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalOffHeap</b></td><td>" + cc.getMaxBytesLocalOffHeap() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalOffHeapPercentage</b></td><td>" + cc.getMaxBytesLocalOffHeapPercentage() + "</td></tr>");
                        out.println("<tr><td><b>MaxElementsOnDisk</b></td><td>" + cc.getMaxElementsOnDisk() + "</td></tr>");
                        out.println("<tr><td><b>MaxEntriesLocalDisk</b></td><td>" + cc.getMaxEntriesLocalDisk() + "</td></tr>");
                        out.println("<tr><td><b>MaxEntriesLocalHeap</b></td><td>" + cc.getMaxEntriesLocalHeap() + "</td></tr>");
                        out.println("<tr><td><b>Statistics</b></td><td>" + cc.getStatistics() + "</td></tr>");
                        out.println("<tr><td><b>TimeToIdleSeconds</b></td><td>" + cc.getTimeToIdleSeconds() + "</td></tr>");
                        out.println("<tr><td><b>TimeToLiveSeconds</b></td><td>" + cc.getTimeToLiveSeconds() + "</td></tr>");
                        out.println("<tr><td><b>ClearOnFlush</b></td><td>" + cc.isClearOnFlush() + "</td></tr>");

                        out.println("<tr><td><b>CopyOnRead</b></td><td>" + cc.isCopyOnRead() + "</td></tr>");
                        out.println("<tr><td><b>CopyOnWrite</b></td><td>" + cc.isCopyOnWrite() + "</td></tr>");
                        out.println("<tr><td><b>CountBasedTuned</b></td><td>" + cc.isCountBasedTuned() + "</td></tr>");
                        out.println("<tr><td><b>Eternal</b></td><td>" + cc.isEternal() + "</td></tr>");
                        out.println("<tr><td><b>Frozen</b></td><td>" + cc.isFrozen() + "</td></tr>");
                        out.println("<tr><td><b>LocalTransactional</b></td><td>" + cc.isLocalTransactional() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalDiskPercentageSet</b></td><td>" + cc.isMaxBytesLocalDiskPercentageSet() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalHeapPercentageSet</b></td><td>" + cc.isMaxBytesLocalHeapPercentageSet() + "</td></tr>");
                        out.println("<tr><td><b>MaxBytesLocalOffHeapPercentageSet</b></td><td>" + cc.isMaxBytesLocalOffHeapPercentageSet() + "</td></tr>");
                        out.println("<tr><td><b>OverflowToOffHeap</b></td><td>" + cc.isOverflowToOffHeap() + "</td></tr>");
                        out.println("<tr><td><b>OverflowToOffHeapSet</b></td><td>" + cc.isOverflowToOffHeapSet() + "</td></tr>");
                        out.println("<tr><td><b>Searchable</b></td><td>" + cc.isSearchable() + "</td></tr>");
                        out.println("<tr><td><b>TerracottaClustered</b></td><td>" + cc.isTerracottaClustered() + "</td></tr>");
                        out.println("<tr><td><b>XaStrictTransactional</b></td><td>" + cc.isXaStrictTransactional() + "</td></tr>");

                        out.println("</table>");

                        out.println("<h4>Variables</h4>");
                        out.println("<table border=\"1\">");
                        out.println("<tr><td><b>InMemorySize</b><br />Warning: This method can be very expensive to run.</td><td>" + ehcache.calculateInMemorySize() + "</td></tr>");
                        out.println("<tr><td><b>MemoryStoreSize (Nr)</b></td><td>" + ehcache.getMemoryStoreSize() + "</td></tr>");
                        out.println("<tr><td><b>OffHeapSize</b></td><td>" + ehcache.calculateOffHeapSize() + "</td></tr>");
                        out.println("<tr><td><b>OnDiskSize</b></td><td>" + ehcache.calculateOnDiskSize() + "</td></tr>");
                        out.println("<tr><td><b>DiskStoreSize (Nr)</b></td><td>" + ehcache.getDiskStoreSize() + "</td></tr>");
                        out.println("<tr><td><b>Size (Nr)</b></td><td>" + ehcache.getSize() + "</td></tr>");
                        out.println("<tr><td><b>AverageGetTime</b></td><td>" + ehcache.getAverageGetTime() + "</td></tr>");
                        out.println("<tr><td><b>AverageSearchTime</b></td><td>" + ehcache.getAverageSearchTime() + "</td></tr>");
                        out.println("<tr><td><b>SearchesPerSecond</b></td><td>" + ehcache.getSearchesPerSecond() + "</td></tr>");
                        out.println("</table>");
                        out.println("<p><form id=\"cacheOperations\" action=\"ehcache.jsp\" method=\"get\">");
                        out.println("<input type=\"hidden\" name=\"cache\" value=\"" + cacheName + "\"/>");
                        out.println("<input type=\"submit\" name=\"clearStatistics\" value=\"clearStatistics\"/>");
                        out.println("<input type=\"submit\" name=\"evictExpiredElements\" value=\"evictExpiredElements\"/>");
                        out.println("<input type=\"submit\" name=\"flush\" value=\"flush\"/>");
                        out.println("<input type=\"submit\" name=\"removeAll\" value=\"removeAll\"/>");
                        out.println("<input type=\"submit\" name=\"setDisabled\" value=\"setDisabled\"/>");
                        out.println("<input type=\"submit\" name=\"setEnabled\" value=\"setEnabled\"/>");
                        out.println("<input type=\"submit\" name=\"setStatisticsEnabled\" value=\"setStatisticsEnabled\"/>");
                        out.println("<input type=\"submit\" name=\"setStatisticsDisabled\" value=\"setStatisticsDisabled\"/>");
                        //out.println("<input type=\"submit\" name=\"dispose\" value=\"dispose\"/>");
                        out.println("</form></p>");
                    } else if (ehcache.getStatus().equals(net.sf.ehcache.Status.STATUS_SHUTDOWN)) {
                        out.println("<p>SHUTDOWN</p>");
                    } else if (ehcache.getStatus().equals(net.sf.ehcache.Status.STATUS_UNINITIALISED)) {
                        out.println("<p>UNINITIALISED</p>");
                    }

                    out.println("<p><a href=\"ehcache.jsp\">Back</a></p>");
                }
            }

        %>
    </body>
</html>
