package hu.mta.sztaki.lod;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.jena.riot.web.HttpNames;

/**
 *
 * @author Sac
 */
public class GraphUtils {

    private static final Logger logger = Logger.getLogger("GraphUtils");
    private static DefaultHttpClient httpClient = getHttpClient();

    private static String getAbsoluteUri(URL url, String bestURL) {
        if (bestURL.startsWith("/")) {
            int port = url.getPort();
            String portStr = "";
            if (port > 0) {
                portStr = ":" + port;
            }
            bestURL = url.getProtocol() + "://" + url.getHost() + portStr + bestURL;
        } else if (bestURL.startsWith("http:") || bestURL.startsWith("https:")) {
        } else {
            if (url.toString().endsWith("/")) {
                bestURL = url.toString() + bestURL;
            } else {
                bestURL = url.toString() + "/" + bestURL;
            }
        }
        bestURL = bestURL.replaceAll(" ", "%20");
        return bestURL;
    }

    public static String getJsonVersionForUri(String reqUrl) throws MalformedURLException, IOException {
        ArrayList<String> jsonUsable = new ArrayList();
        ArrayList<String> xmlUsable = new ArrayList();

        reqUrl = reqUrl.replaceAll(" ", "%20");

        try {

            HttpGet httpGet = new HttpGet(reqUrl);
            httpGet.addHeader(HttpNames.hAccept, "text/turtle,application/rdf+xml;q=0.9,*/*;q=0.5");
            // Originally in Jena: httpGet.addHeader(HttpNames.hAccept, "text/turtle,application/rdf+xml;q=0.9,application/xml;q=0.8,*/*;q=0.5");
            // But with that we have found problematic urls (http://viaf.org/viaf/14182199 -> http://viaf.org/viaf/14182199/viaf.xml)
            httpGet.addHeader(HttpNames.hAcceptCharset, "utf-8");
            String whole = "";
            try {
                HttpResponse rsp = httpClient.execute(httpGet);
                HttpEntity entity = rsp.getEntity();
                ContentType contentType = ContentType.getOrDefault(entity);
                String mimeType = contentType.getMimeType();
                InputStream instream = entity.getContent();
                //long length = entity.getContentLength();
                
                /*StringWriter writer = new StringWriter();
                IOUtils.copy(instream, writer, "utf-8");
                String stringContent = writer.toString();
                
                instream = entity.getContent();*/
                
                ByteArrayOutputStream baos0 = new ByteArrayOutputStream();

                BasicStatusLine bsl = (BasicStatusLine) rsp.getStatusLine();
                if (bsl.getStatusCode() == 200) {
                    if (!mimeType.equals("text/html")) {

                        Model model = ModelFactory.createDefaultModel();

                        /*String baseurl = url.getProtocol();
                         baseurl += (url.getPort() > -1) ? ":" + url.getPort() : ":";
                         baseurl += "//" + url.getHost();*/

                        //model.read(instream, reqUrl);
                        if (mimeType.equals("text/turtle")) {
                            model.read(instream, reqUrl, "TTL");
                        } else {
                            model.read(instream, reqUrl);
                        }
                        //model.read(reqUrl.toString());
                        String syntax = "RDF/JSON"; // also try "N-TRIPLE" and "TURTLE"
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();

                        //StringWriter outwriter = new StringWriter();
                        model.write(baos, syntax);


                        httpGet.releaseConnection();
                        return (baos.toString("utf-8"));
                    } else {
                        logger.log(Level.INFO, reqUrl + " text/html");
                        URL url = new URL(reqUrl);
                        //ResponseHandler<String> responseHandler = new BasicResponseHandler();
                        //whole = httpclient.execute(httpGet, responseHandler);
                        whole = IOUtils.toString(instream, "UTF-8");

                        Pattern patt = Pattern.compile("(?i)<head(.*?)>(.*?)</head>");
                        Matcher matcher = patt.matcher(whole);
                        String head = "";
                        if (matcher.find()) {
                            head = matcher.group(2);
                        }
                        patt = Pattern.compile("<link (.*?)>");
                        Pattern typePatt = Pattern.compile("(?i)type=\"(.*?)\"");
                        Pattern hrefPatt = Pattern.compile("(?i)href=\"(.*?)\"");
                        matcher = patt.matcher(whole);
                        while (matcher.find()) {
                            Matcher typeMatch = typePatt.matcher(matcher.group(1));
                            Matcher hrefMatch = hrefPatt.matcher(matcher.group(1));
                            if (typeMatch.find()) {
                                hrefMatch.find();
                                if (Constants.jsonformatsList.contains(typeMatch.group(1))) {
                                    // Best ever
                                    jsonUsable.add(getAbsoluteUri(url, hrefMatch.group(1)));
                                    //break;
                                } else if (Constants.xmlformatsList.contains(typeMatch.group(1))) {
                                    xmlUsable.add(getAbsoluteUri(url, hrefMatch.group(1)));
                                }
                            }
                        }
                        httpGet.releaseConnection();
                        for (int i = 0; i < jsonUsable.size(); i++) {
                            try {
                                Model model = ModelFactory.createDefaultModel();
                                model.read(jsonUsable.get(i));
                                String syntax = "RDF/JSON"; // also try "N-TRIPLE" and "TURTLE"
                                StringWriter outwriter = new StringWriter();
                                model.write(outwriter, syntax);
                                return (outwriter.toString());
                            } catch (Exception e) {
                                //logger.info(e.getMessage());
                            }
                        }
                        for (int i = 0; i < xmlUsable.size(); i++) {
                            try {
                                Model model = ModelFactory.createDefaultModel();
                                model.read(xmlUsable.get(i));
                                String syntax = "RDF/JSON"; // also try "N-TRIPLE" and "TURTLE"
                                StringWriter outwriter = new StringWriter();
                                model.write(outwriter, syntax);
                                return (outwriter.toString());
                            } catch (Exception e) {
                                //logger.info(e.getMessage());
                            }
                        }
                    }
                } else {
                }
            } catch (IOException ex) {

                // In case of an IOException the connection will be released
                // back to the connection manager automatically
                throw ex;

            } catch (RuntimeException ex) {

                // In case of an unexpected exception you may want to abort
                // the HTTP request in order to shut down the underlying
                // connection and release it back to the connection manager.
                ex.printStackTrace();
                httpGet.abort();
                throw ex;

            } finally {
                httpGet.releaseConnection();
            }

            //System.out.println(outwriter.toString());
        } catch (Exception e) {
            System.out.println("exception happened: " + e);
        }

        /*URL url = new URL(reqUrl);

         GraphUtils.getUsableFormatsForUri(url, jsonUsable, xmlUsable);

         return (GraphUtils.getBestJsonFromUris(jsonUsable, xmlUsable));*/
        return null;
    }

    /**
     *
     * @param graph
     * @param selectedList
     * @return
     */
    public static String jsonStrFromGraph(HashMap<String, Node> graph, List selectedList, String[] rootNodes) {
        String retval = "{\"graph\":{\"nodes\":{";
        Iterator graphIter = graph.keySet().iterator();
        int nodecounter = 0;
        while (graphIter.hasNext()) {
            String nodeId = (String) graphIter.next();
            if (nodecounter > 0) {
                retval += ", ";
            }
            nodecounter++;
            retval += "\"" + nodeId + "\":{\"resource_id\":\"" + nodeId + "\"";
            if (selectedList != null && selectedList.contains(nodeId)) {
                retval += ",\"selected\":\"1\"";
            }
            retval += ",\"dist\":[";
            Node node = graph.get(nodeId);
            for (int i = 0; i < node.distance.length; i++) {
                if (i != 0) {
                    retval += ",";
                }
                retval += node.distance[i];
            }
            retval += "]";
            retval += "}";
        }
        retval += "},\"input\":[";
        for (int i = 0; i < rootNodes.length; i++) {
            if (i != 0) {
                retval += ",";
            }

            retval += "\"" + rootNodes[i] + "\"";
        }
        retval += "]}}";
        return retval;
    }

    /**
     * Hack needed for badly encoded urls
     *
     * @param text
     * @return
     */
    public static String fixEncoding(String text) {
        if (text.startsWith("http://lod.sztaki.hu")) {
            text = text.replaceAll("Ã³", "ó");
            text = text.replaceAll("Ã¡", "á");
            text = text.replaceAll("Ã¶", "ö");
        }
        return text;
    }

    private static DefaultHttpClient getHttpClient() {
        HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams, 15000);
        HttpConnectionParams.setSoTimeout(httpParams, 15000);
        DefaultHttpClient defaultHttpClient = new DefaultHttpClient(new PoolingClientConnectionManager(), httpParams);
        return defaultHttpClient;
    }
}
