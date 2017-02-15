package hu.mta.sztaki.lod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sac
 */
public class LinkFinder {

    private static final Logger logger = Logger.getLogger("LinkFinder");
    private String[] rootNodes;
    private HashMap<String, Node> finalGraph;
    private HashMap<String, Node> actualIterationNodes;
    private HashMap<String, Node> nextIterationNodes;
    HashMap<String, Node> workingOnNodes;
    HashMap<String, Node> retval;
    private int pathMax;
    private int nodesMax;
    private Object semafore = new Object();
    private String[] search;

    public LinkFinder(String[] rootNodes, HashMap<String, Node> finalGraph, HashMap<String, Node> actualIterationNodes, int pathMax, int nodesMax, String search) {
        this.rootNodes = rootNodes;
        this.finalGraph = finalGraph;
        this.actualIterationNodes = actualIterationNodes;
        this.pathMax = pathMax;
        this.nodesMax = nodesMax;
        nextIterationNodes = new HashMap<String, Node>();
        workingOnNodes = new HashMap<String, Node>();
        retval = new HashMap<String, Node>();
        this.search = search.split(",");
    }

    /**
     *
     * @param finalGraph The content had been required from the servers
     * @param actualIterationNodes
     * @return
     */
    public void traverseLinks() {
        int iterationCount = 0;

        logger.log(Level.INFO, "Starting iteration: " + iterationCount + Thread.currentThread().getId());
        while (true) {
            Thread[] threads = new Thread[5];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(new GetNodeContentThread(), "httpharvester");
                threads[i].start();
                //logger.log(Level.INFO, "Starting thread( " + Thread.currentThread().getId() + "): " + threads[i].getId());
            }

            boolean threadsrunning = true;
            while (threadsrunning) {
                threadsrunning = false;
                for (int i = 0; i < threads.length; i++) {
                    if (threads[i].isAlive()) {
                        threadsrunning = true;
                        try {
                            threads[i].join();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }

            if (finalGraph.size() > 10000) {
                logger.log(Level.SEVERE, "More than 10000 nodes were requested");
                return;
            }

            if (actualIterationNodes.isEmpty()) {
                // The fifos are empty
                logger.log(Level.INFO, "The fifos are empty");
                if (!retval.isEmpty()) {
                    logger.log(Level.INFO, "Final value found");
                    return;
                }
                iterationCount++;
                if (iterationCount > pathMax) {
                    logger.log(Level.INFO, "Max path reached");
                    return;
                }
                if (nextIterationNodes.isEmpty()) {
                    logger.log(Level.INFO, "Search finished - no more traversable paths");
                    return;
                } else {
                    actualIterationNodes = nextIterationNodes;
                    nextIterationNodes = new HashMap<String, Node>();
                    logger.log(Level.INFO, "Starting iteration - " + iterationCount);


                    Iterator nextIterationIterator = actualIterationNodes.keySet().iterator();
                    while (nextIterationIterator.hasNext()) {
                        Node tempNode = actualIterationNodes.get(nextIterationIterator.next());

                        for (int i = 0; i < rootNodes.length; i++) {
                            for (int j = 0; j < rootNodes.length; j++) {
                                if (i != j && tempNode.parents[i] != null && tempNode.parents[j] != null) {
                                    int tempdistance = 0;
                                    for (int l = 0; l < tempNode.parents.length; l++) {
                                        if (tempNode.parents[l] != null) {
                                            tempdistance += tempNode.distance[l];
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public synchronized Node getNextNode() {
        //logger.log(Level.INFO, "Semafore wait0: " + Thread.currentThread().getId());
        Node actualNode = null;
        synchronized (semafore) {
            //logger.log(Level.INFO, "Semafore get0: " + Thread.currentThread().getId());
            if (!actualIterationNodes.isEmpty()) {
                String nodeId = actualIterationNodes.keySet().iterator().next();
                actualNode = actualIterationNodes.remove(nodeId);
                workingOnNodes.put(nodeId, actualNode);
            }
            //logger.log(Level.INFO, "Semafore release0: " + Thread.currentThread().getId());
        }
        return actualNode;
    }

    public synchronized void moveNodeToFinalGraph(String nodeId) {
        //logger.log(Level.INFO, "Semafore wait: " + Thread.currentThread().getId());
        synchronized (semafore) {
            //logger.log(Level.INFO, "Semafore get: " + Thread.currentThread().getId());
            Node actualNode = workingOnNodes.remove(nodeId);

            if (finalGraph.size() < nodesMax) {
                finalGraph.put(nodeId, actualNode);

                Iterator iterator = actualNode.connections.keySet().iterator();
                //logger.log(Level.INFO, "moveNodeToFinalGraph - Node: " + actualNode.resource_id);
                //logger.log(Level.INFO, "moveNodeToFinalGraph - Parents: " + actualNode.parents.length);
                while (iterator.hasNext()) {
                    String connectionKey = (String) iterator.next();

                    boolean matching = false;
                    for (int i = 0; i < search.length; i++) {
                        if (connectionKey.toUpperCase().contains(search[i])) {
                            matching = true;
                            break;
                        }
                    }

                    if (matching) {
                        ArrayList<String> actConn = actualNode.connections.get(connectionKey);

                        for (int i = 0; i < actConn.size(); i++) {
                            Node newNode = finalGraph.get(actConn.get(i));
                            if (newNode == null) {

                                newNode = workingOnNodes.get(actConn.get(i));

                                if (newNode != null) {
                                    copyParents(actualNode, newNode.resource_id, workingOnNodes, true);
                                    // Somebody will check out this node soon to work on it
                                    //logger.log(Level.INFO, "D0: Node from workingOnNodes " + Thread.currentThread().getId());
                                    continue;
                                    //workingOnNodes.put(newNode.resource_id, newNode);
                                }
                                newNode = actualIterationNodes.get(actConn.get(i));

                                if (newNode != null) {
                                    copyParents(actualNode, newNode.resource_id, actualIterationNodes, true);
                                    // It is not checked out yet
                                    //logger.log(Level.INFO, "D0: Node from actualIterationNodes" + Thread.currentThread().getId());
                                    continue;
                                    //actualIterationNodes.put(newNode.resource_id, newNode);
                                }
                                newNode = nextIterationNodes.get(actConn.get(i));
                                if (newNode != null) {
                                    copyParents(actualNode, newNode.resource_id, nextIterationNodes, false);
                                    // It is not checked out yet
                                    //logger.log(Level.INFO, "D0: Node from nextIterationNodes" + Thread.currentThread().getId());
                                    continue;
                                    //nextIterationNodes.put(newNode.resource_id, newNode);
                                }

                                // It is not existing yet
                                //logger.log(Level.INFO, "D0: Create new Node" + Thread.currentThread().getId());
                                newNode = new Node(actConn.get(i));
                                nextIterationNodes.put(newNode.resource_id, newNode);
                                copyParents(actualNode, newNode.resource_id, nextIterationNodes, false);
                            } else {
                                copyParents(actualNode, newNode.resource_id, finalGraph, true);
                            }
                        }
                    }
                }
            }
            //logger.log(Level.INFO, "Semafore release: " + Thread.currentThread().getId());
        }
    }

    private class GetNodeContentThread
            implements Runnable {

        private GetNodeContentThread() {
        }

        public void run() {
            while (!actualIterationNodes.isEmpty()) {
                Node actualNode = getNextNode();

                // Take one element from the selected fifo
                if (actualNode == null) {
                    return;
                }


                if (retval.get(actualNode.resource_id) == null) {
                    actualNode.getConnections();
                }

                /*if (actualNode.matchContentString(search)) {
                 retval.put(actualNode.resource_id, actualNode);
                 }*/

                moveNodeToFinalGraph(actualNode.resource_id);

                if (finalGraph.size() > 10000) {
                    logger.log(Level.SEVERE, "More than 10000 nodes were requested");
                    return;
                }
            }
        }
    }

    public HashMap<String, Node> cleanupResultGraph(int maxNodes) {
        ArrayList<Node> commonNodes = new ArrayList<Node>();


        if (retval.isEmpty()) {
            return retval;
        }
        int parentsSize = retval.get(retval.keySet().iterator().next()).parents.length;
        Iterator retvalItemsIterator = Arrays.asList(retval.keySet().toArray()).iterator();
        while (retvalItemsIterator.hasNext()) {
            Node tempNode = finalGraph.get(retvalItemsIterator.next());

            HashMap<String, Node> tempList = new HashMap<String, Node>();

            ArrayList<String> toProcess = new ArrayList<String>();

            toProcess.add(tempNode.resource_id);
            tempList.put(tempNode.resource_id, tempNode);



            for (int i = 0; i < parentsSize; i++) {
                if (tempNode.parents[i] != null) {
                    String tempId = tempNode.parents[i].get(0);
                    if (!retval.containsKey(tempId) && !tempList.containsKey(tempId)) {
                        toProcess.add(tempId);
                        tempList.put(tempId, finalGraph.get(tempId));
                    }
                }
            }

            while (!toProcess.isEmpty()) {
                tempNode = finalGraph.get(toProcess.remove(0));

                for (int i = 0; i < parentsSize; i++) {
                    if (tempNode.parents[i] != null) {
                        String tempId = tempNode.parents[i].get(0);
                        if (!retval.containsKey(tempId) && !tempList.containsKey(tempId)) {
                            toProcess.add(tempId);
                            tempList.put(tempId, finalGraph.get(tempId));
                        }
                    }


                }


                if (tempList.size() + retval.size() <= maxNodes) {
                    retval.putAll(tempList);
                }
            }
        }
        logger.log(Level.INFO, "Postprocess finished: " + Thread.currentThread().getId());
        return retval;
    }

    private Node copyParents(Node actualNode, String newNodeId, HashMap<String, Node> nodeContainer, boolean checkRetval) {

        //logger.log(Level.INFO, "copyParents running: " + actualNode.resource_id+ " " + newNodeId);
        Node newNode = nodeContainer.get(newNodeId);
        if (newNode.parents == null) {
            newNode.parents = new ArrayList[actualNode.parents.length];
            newNode.distance = new int[actualNode.parents.length];
            for (int i = 0; i < actualNode.parents.length; i++) {
                newNode.distance[i] = -1;
            }
        }

        for (int j = 0; j < actualNode.parents.length; j++) {
            if (actualNode.parents[j] != null) {
                if (newNode.parents[j] == null) {
                    newNode.parents[j] = new ArrayList<String>();
                    newNode.parents[j].add(actualNode.resource_id);
                    newNode.distance[j] = actualNode.distance[j] + 1;
                } else {
                    if (newNode.distance[j] > actualNode.distance[j] + 1) {
                        newNode.parents[j] = new ArrayList<String>();
                        newNode.parents[j].add(actualNode.resource_id);
                    } else if (newNode.distance[j] == actualNode.distance[j] + 1) {
                        newNode.parents[j].add(actualNode.resource_id);
                    }
                }
            }
        }
        return newNode;
    }
}
