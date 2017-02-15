package hu.mta.sztaki.lod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

/**
 * Node class identifying rdf graph resources.
 * 
 * @author Sac
 */
public class Node {
    /**
     * ehcache cache manager for local resource persistence. Unfortunately each
     * redeploy and server restart will earse the cached data, because the restartable
     * caching strategy is not part of the freeware ehcache distribution.
     */
    public static final CacheManager cacheManager = new CacheManager();
    /**
     * The identifier of the rdf node. Practically it is the URI.
     */
    public String resource_id;
    /**
     * Hashmap of the connections. The keys are connection types, the values are
     * ArrayLists with target uris. The direction of the connection is not stored.
     */
    public HashMap<String, ArrayList<String>> connections;
    /**
     * Strings associated with the node. These are the contents of the literals of
     * the nodes and used for the content related search functionality.
     */
    public ArrayList<String> strIndex;
    /**
     * true if the content has been loaded from the uri, false otherwise.
     */
    public boolean requested = false;
    /**
     * Parent nodes. Used to store the previous path segments in graph traversing
     * functions. It stores ArrayLists because in the same iteration a given node
     * might be reached from several sources.
     */
    public ArrayList<String>[] parents;
    /**
     * Steps from the original starting points in graph traversing.
     */
    public int[] distance;

    /**
     * Constructor of the Node.
     * @param resource_id The uri of the rdf resource.
     */
    public Node(String resource_id) {
        //Logger.getLogger(Node.class.getName()).log(Level.INFO, "Created: " + resource_id);
        this.resource_id = resource_id;
        this.connections = new HashMap<String, ArrayList<String>>();
        this.strIndex = new ArrayList<String>();
    }

    /**
     * Sets the parents of the rdf node. It is used in graph traversing.
     * @param parents 
     */
    public void setParents(ArrayList<String>[] parents) {
        this.parents = parents;
    }

    /**
     * Setter of the distance array.
     * @param distance 
     */
    public void setDistance(int[] distance) {
        this.distance = distance;
    }

    /**
     * Returns the connections to or from the given rdf resource. The node might be
     * in two states: downloaded and not downloaded. In downloaded state its parameters
     * and content is set from the cache or from the original resource.  
     * @return HashMap<String, ArrayList<String>> of the connections to/from the node. The String key is the uri of the connection type.
     */
    public HashMap<String, ArrayList<String>> getConnections() {
        if (!this.requested) {
            try {
                // Get the node description from the cache
                Element elem = cacheManager.getEhcache("loduris").get(this.resource_id);
                if (elem == null) {
                    // It was not found in the cache. Download it from the original source.
                    String jsonStrNode = GraphUtils.getJsonVersionForUri(this.resource_id);

                    ObjectMapper mapper = new ObjectMapper();
                    if (jsonStrNode == null) {
                        Logger.getLogger(Node.class.getName()).log(Level.SEVERE, "Check this URL (resulted null): " + this.resource_id);
                        return null;
                    }
                    // Parse the json version of the resource
                    JsonNode rootNode = mapper.readValue(jsonStrNode, JsonNode.class);

                    Iterator fieldNames = rootNode.fieldNames();
                    while (fieldNames.hasNext()) {
                        String keyUri = GraphUtils.fixEncoding((String) fieldNames.next());
                        JsonNode selfNode = rootNode.get(keyUri);

                        Logger.getLogger(Node.class.getName()).log(Level.FINER, keyUri);
                        Iterator selfFieldNames = selfNode.fieldNames();
                        while (selfFieldNames.hasNext()) {
                            // selfFieldNames: connection URIs
                            String connectionURI = (String) selfFieldNames.next();
                            if (Constants.trivialconnectiontypesList.contains(connectionURI)) {
                                // It is a trivial connection type
                                continue;
                            }
                            boolean trivial = false;
                            for (int i = 0; i < Constants.trivialconnectiontypeprefs.length; i++) {
                                if (connectionURI.startsWith(Constants.trivialconnectiontypeprefs[i])) {
                                    // It is a trivial connection type
                                    trivial = true;
                                    break;
                                }
                            }
                            if (trivial) {
                                continue;
                            }

                            JsonNode subNode = selfNode.get(connectionURI);
                            if (subNode.isArray()) {
                                for (int subNodeArrayCounter = 0; subNodeArrayCounter < subNode.size(); subNodeArrayCounter++) {
                                    JsonNode snitem = subNode.get(subNodeArrayCounter);
                                    if (snitem.has("type") && snitem.get("type").textValue().equals("uri") && snitem.has("value")) {
                                        String snitemText = GraphUtils.fixEncoding(snitem.get("value").textValue());
                                        trivial = false;
                                        for (int i = 0; i < Constants.trivialconnections.length; i++) {
                                            if (snitemText.startsWith(Constants.trivialconnections[i])) {
                                                trivial = true;
                                                break;
                                            }
                                        }
                                        if (!trivial) {
                                            if (!keyUri.equals(snitemText)) {
                                                ArrayList<String> typedConnections;
                                                if (snitemText.equals(this.resource_id)) {
                                                    if (!this.connections.containsKey(connectionURI)) {
                                                        typedConnections = new ArrayList<String>();
                                                        this.connections.put(connectionURI, typedConnections);
                                                    } else {
                                                        typedConnections = this.connections.get(connectionURI);
                                                    }
                                                    if (!typedConnections.contains(keyUri)) {
                                                        typedConnections.add(keyUri);
                                                    }
                                                } else if (keyUri.equals(this.resource_id) || selfNode.has("http://www.w3.org/2000/01/rdf-schema#label")) {
                                                    // The json node contains the out connections of the resource.
                                                    if (!this.connections.containsKey(connectionURI)) {
                                                        typedConnections = new ArrayList<String>();
                                                        this.connections.put(connectionURI, typedConnections);
                                                    } else {
                                                        typedConnections = this.connections.get(connectionURI);
                                                    }
                                                    if (!typedConnections.contains(snitemText)) {
                                                        typedConnections.add(snitemText);
                                                    }
                                                    //this.connections.add(snitemText);
                                                }
                                                //Logger.getLogger(Node.class.getName()).log(Level.INFO, "Added: " + snitem.get("value").textValue());
                                            }
                                        }
                                    } else if (snitem.has("type") && snitem.get("type").textValue().equals("literal") && snitem.has("value")) {
                                        // The value of the literals of the resource will be 
                                        // stored to support the content search functionality
                                        String snitemText = snitem.get("value").textValue();
                                        if (keyUri.equals(this.resource_id) || selfNode.has("http://www.w3.org/2000/01/rdf-schema#label")) {
                                            this.strIndex.add(snitemText);
                                        }
                                    }
                                }
                            } else {
                                Logger.getLogger(Node.class.getName()).log(Level.INFO, "NOT ARRAY!!!");
                            }
                        }
                    }

                    Object content[];
                    content = new Object[2];
                    content[0] = this.connections;
                    content[1] = this.strIndex;
                    
                    // The connections and the strIndex will be stored in the cache
                    cacheManager.getEhcache("loduris").put(elem = new Element(this.resource_id, content));
                }
                Object content[] = (Object[]) elem.getObjectValue();
                this.connections = (HashMap<String, ArrayList<String>>) content[0];
                this.strIndex = (ArrayList<String>) content[1];
            } catch (MalformedURLException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, "Exception for: " + this.resource_id, ex);
            } catch (IOException ex) {
                Logger.getLogger(Node.class.getName()).log(Level.SEVERE, "Exception for: " + this.resource_id, ex);
            }
        }

        this.requested = true;
        Logger.getLogger(Node.class.getName()).log(Level.INFO, "Collected: " + this.resource_id);
        return this.connections;
    }

    /**
     * Checks for a given search in the collected literal values.
     * @param search String to look up in the literal values.
     * @return true if the string is found somewhere in the content, false otherwise.
     */
    boolean matchContentString(String search) {
        boolean retval = false;
        if (!this.requested) {
            getConnections();
        }

        for (int i = 0; i < this.strIndex.size(); i++) {
            if (this.strIndex.get(i).toUpperCase().contains(search.toUpperCase())) {
                return true;
            }
        }
        return retval;
    }
}
