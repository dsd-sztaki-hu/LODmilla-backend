package hu.mta.sztaki.lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PathFinder class. It is able to find the shortest path(s) between any number
 * of initial lod resources.
 *
 * @author Sac
 */
public class PathFinder {

    /**
     * Logger of the class.
     */
    private static final Logger logger = Logger.getLogger("PathFinder");
    /**
     * The number of the initial resource identifiers given by the user.
     */
    private int rootNodesCount;
    /**
     * Set of nodes with downloaded content.
     */
    private HashMap<String, Node> finalGraph;
    /**
     * Set of nodes issued for content download.
     */
    private HashMap<String, Node> actualIterationNodes;
    /**
     * Set of nodes issued for content download in the next iteration.
     */
    private HashMap<String, Node> nextIterationNodes;
    /**
     * Set of nodes checked out by working threads to download the content.
     */
    HashMap<String, Node> workingOnNodes;
    /**
     * The resulting graph after running the pathfinding algorithm.
     */
    HashMap<String, Node> retval;
    /**
     * The maximum path-length.
     */
    private int pathMax;
    /**
     * The maximum nodes which can be in the result.
     */
    private int nodesMax;
    /**
     * Used for the synchronisation of the download threads.
     */
    private Object semafore = new Object();
    /**
     * The number of download threads.
     */
    public final static int DOWNLOAD_THREAD_NUMBER = 5;
    /**
     * The maximum size of the final graph.
     */
    public final static int MAX_GRAPH_SIZE = 10000;
    private int minDistance = -1;
    private int maxConn = -1;

    /**
     * Constructor of the class.
     *
     * @param actualIterationNodes Contains the starting nodes
     * @param pathMax The maximum path-length (stop condition)
     */
    public PathFinder(HashMap<String, Node> actualIterationNodes, int pathMax, int nodesMax) {
        this.finalGraph = new HashMap<String, Node>();
        this.actualIterationNodes = actualIterationNodes;
        this.rootNodesCount = actualIterationNodes.size();
        this.pathMax = pathMax;
        this.nodesMax = nodesMax;
        nextIterationNodes = new HashMap<String, Node>();
        workingOnNodes = new HashMap<String, Node>();
        retval = new HashMap<String, Node>();
    }

    /**
     * Finds the shortest path between the starting nodes. It will stop in the
     * iteration where the first node is found which is reachable from at least
     * two startpoints.
     *
     * @return A graph containing nodes along paths between the startpoints.
     */
    public HashMap<String, Node> findShortestPath() {
        int iterationCount = 0;

        logger.log(Level.INFO, "Starting iteration: " + iterationCount + " " + Thread.currentThread().getId());
        while (true) {
            // Start MAX_DOWNLOAD_THREAD_NUMBER threads to http download the node contents
            Thread[] threads = new Thread[DOWNLOAD_THREAD_NUMBER];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = new Thread(new GetNodeContentThread(), "httpharvester");
                threads[i].start();
                //logger.log(Level.INFO, "Starting thread( " + Thread.currentThread().getId() + "): " + threads[j].getId());
            }

            boolean threadsrunning = true;
            while (threadsrunning) {
                threadsrunning = false;
                for (int i = 0; i < threads.length; i++) {
                    // wait until all downloads from the actual iteration ends
                    // (or any other stop condition is met by the threads which
                    // stops their work)
                    if (threads[i].isAlive()) {
                        threadsrunning = true;
                        try {
                            threads[i].join();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }

            if (finalGraph.size() > MAX_GRAPH_SIZE) {
                logger.log(Level.SEVERE, "More than MAX_GRAPH_SIZE nodes were requested");
                return retval;
            }

            if (retval.size() > 0) {
                logger.log(Level.SEVERE, "Final value found");
                return retval;
            }

            if (actualIterationNodes.isEmpty()) {
                // The fifos are empty
                logger.log(Level.INFO, "The fifos are empty");

                iterationCount++;
                if (iterationCount * 2 > pathMax) {
                    logger.log(Level.INFO, "Max path reached - no common path");
                    return retval;
                }
                if (nextIterationNodes.isEmpty()) {
                    logger.log(Level.INFO, "Search finished - no common path");
                    return retval;
                } else {
                    actualIterationNodes = nextIterationNodes;
                    nextIterationNodes = new HashMap<String, Node>();
                    logger.log(Level.INFO, "Starting iteration - " + iterationCount + " " + Thread.currentThread().getId());

                    Iterator nextIterationIterator = actualIterationNodes.keySet().iterator();
                    while (nextIterationIterator.hasNext()) {
                        Node tempNode = actualIterationNodes.get(nextIterationIterator.next());

                        for (int i = 0; i < rootNodesCount; i++) {
                            for (int j = 0; j < rootNodesCount; j++) {
                                if (i != j && tempNode.parents[i] != null && tempNode.parents[j] != null) {

                                    boolean alreadyIn = false;
                                    if (retval.containsKey(tempNode.resource_id)) {
                                        alreadyIn = true;
                                    }

                                    retval.put(tempNode.resource_id, tempNode);
                                    if (!alreadyIn) {
                                        spGraphUpdate(tempNode);
                                    }

                                    int tempconncumb = 0;
                                    int tempdistance = 0;
                                    for (int l = 0; l < tempNode.parents.length; l++) {
                                        if (tempNode.parents[l] != null) {
                                            tempconncumb++;
                                            tempdistance += tempNode.distance[l];
                                        }
                                    }
                                    if (tempconncumb > maxConn || minDistance == -1 || minDistance > tempdistance) {
                                        maxConn = tempconncumb;
                                        minDistance = tempdistance;
                                    }
                                    logger.log(Level.INFO, "Found common node(0): " + tempNode.resource_id);
                                }
                            }
                        }
                    }

                    if (!retval.isEmpty()) {
                        logger.log(Level.INFO, "Search finished - final value found - last layer not requested");
                        return retval;
                    }
                }
            }
        }
    }

    /**
     *
     * @param retval
     * @param finalGraph
     * @param actualIterationNodes
     */
    private synchronized void spGraphUpdate(Node targetNode) {
        ArrayList<String> nodesToCheck = new ArrayList<String>();
        if (targetNode == null) {
            Iterator spTraverseIterator = retval.keySet().iterator();
            while (spTraverseIterator.hasNext()) {
                String nodeId = (String) spTraverseIterator.next();
                if (!nodesToCheck.contains(nodeId)) {
                    nodesToCheck.add(nodeId);
                }
            }
        } else {
            nodesToCheck.add(targetNode.resource_id);
        }


        while (!nodesToCheck.isEmpty()) {
            String nodeId = (String) nodesToCheck.remove(0);
            Node node = retval.get(nodeId);
            for (int i = 0; i < node.parents.length; i++) {
                if (node.parents[i] != null) {
                    int parentsToKeepCount = node.parents[i].size();
                    for (int j = 0; j < parentsToKeepCount; j++) {
                        String tempNodeId = node.parents[i].get(j);
                        if (!retval.containsKey(tempNodeId)) {
                            Node toaddnode = finalGraph.get(tempNodeId);
                            if (toaddnode == null) {
                                actualIterationNodes.get(tempNodeId);
                            }
                            if (toaddnode == null) {
                                logger.log(Level.SEVERE, "Where is that node, anyway?");
                            }
                            if (toaddnode.distance[i] < node.distance[i]) {
                                retval.put(toaddnode.resource_id, toaddnode);
                                nodesToCheck.add(toaddnode.resource_id);
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

    /**
     * A synchronised function used by the http download threads to move the
     * downloaded nodes to the finalGraph and put the connected node_ids to the
     * nextIteration map (if they were not requested in the actual iteration or
     * before).
     *
     * @param nodeId The id of the node to be moved from the workingOnNodes map
     * to the finalGraph
     */
    public synchronized void moveNodeToFinalGraph(String nodeId) {
        synchronized (semafore) {
            Node actualNode = workingOnNodes.remove(nodeId);
            finalGraph.put(nodeId, actualNode);
            Iterator iterator = actualNode.connections.keySet().iterator();
            while (iterator.hasNext()) {
                String connectionKey = (String) iterator.next();

                ArrayList<String> actConn = actualNode.connections.get(connectionKey);

                for (int i = 0; i < actConn.size(); i++) {
                    Node newNode = finalGraph.get(actConn.get(i));
                    if (newNode != null) {
                        copyParentsAndCheck(actualNode, newNode.resource_id, finalGraph, true);
                        if (retval.size() > nodesMax) {
                            return;
                        }
                        continue;
                    }

                    newNode = workingOnNodes.get(actConn.get(i));

                    if (newNode != null) {
                        // Somebody will check out this node soon to work on it
                        copyParentsAndCheck(actualNode, newNode.resource_id, workingOnNodes, true);
                        if (retval.size() > nodesMax) {
                            return;
                        }
                        continue;
                    }
                    newNode = actualIterationNodes.get(actConn.get(i));

                    if (newNode != null) {
                        // It is not checked out yet
                        //logger.log(Level.INFO, "D0: Node from actualIterationNodes" + Thread.currentThread().getId());
                        copyParentsAndCheck(actualNode, newNode.resource_id, actualIterationNodes, true);
                        if (retval.size() > nodesMax) {
                            return;
                        }
                        continue;
                        //actualIterationNodes.put(newNode.resource_id, newNode);
                    }
                    newNode = nextIterationNodes.get(actConn.get(i));
                    if (newNode != null) {
                        // It is not checked out yet
                        //logger.log(Level.INFO, "D0: Node from nextIterationNodes" + Thread.currentThread().getId());
                        copyParentsAndCheck(actualNode, newNode.resource_id, nextIterationNodes, false);
                        if (retval.size() > nodesMax) {
                            return;
                        }
                        continue;
                        //nextIterationNodes.put(newNode.resource_id, newNode);
                    }

                    // It is not existing yet
                    //logger.log(Level.INFO, "D0: Create new Node" + Thread.currentThread().getId());
                    newNode = new Node(actConn.get(i));
                    nextIterationNodes.put(newNode.resource_id, newNode);
                    copyParentsAndCheck(actualNode, newNode.resource_id, nextIterationNodes, false);

                    // It is possible, that previously in the same iteration somebody has 
                    // set the parent of this node and it become to be a common node.

                    int tempConnectionCount = 0;
                    int tempDistance = 0;
                    for (int s = 0; s < actualNode.parents.length; s++) {
                        if (actualNode.parents[s] != null) {
                            tempConnectionCount++;
                            tempDistance += actualNode.distance[s];
                        }
                    }
                    if (tempConnectionCount > 1 && (minDistance == -1 || (tempConnectionCount == maxConn && tempDistance <= minDistance) || tempConnectionCount > maxConn)) {
                        maxConn = tempConnectionCount;
                        minDistance = tempDistance;

                        boolean alreadyIn = false;
                        if (retval.containsKey(actualNode.resource_id)) {
                            alreadyIn = true;
                        }
                        retval.put(actualNode.resource_id, actualNode);
                        spGraphUpdate(actualNode);
                        if (!alreadyIn) {
                            logger.log(Level.INFO, "Found common node0: " + actualNode.resource_id);
                            logger.log(Level.INFO, "Retval size0: " + retval.size());
                        } else {
                            logger.log(Level.INFO, "Retval updated0: " + actualNode.resource_id);
                            logger.log(Level.INFO, "Retval size0: " + retval.size());
                        }
                    }


                    if (retval.size() > nodesMax) {
                        return;
                    }
                }
            }
            //logger.log(Level.INFO, "Semafore release: " + Thread.currentThread().getId());
        }
    }

    /**
     * It is called from a synchronised function. Copies the parents of node to
     * another one and checks if parents are set from more than one sources. If
     * it happens, this node and the route to it will be placed into the retval
     * HashMap.
     *
     * @param actualNode The node which parents will be copied
     * @param newNodeId The id of the target node
     * @param nodeContainer The container where the target node is placed
     * @return The target node
     */
    private Node copyParentsAndCheck(Node actualNode, String newNodeId, HashMap<String, Node> nodeContainer, boolean checkRetval) {
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

        if (checkRetval) {
            for (int s = 0; s < actualNode.parents.length; s++) {
                for (int j = 0; j < actualNode.parents.length; j++) {
                    if (s != j && newNode.parents[s] != null && newNode.parents[j] != null && (actualNode.parents[s] == null || actualNode.parents[j] == null)) {
                        boolean alreadyIn = false;
                        if (retval.containsKey(newNode.resource_id)) {
                            alreadyIn = true;
                        }
                        retval.put(newNode.resource_id, newNode);
                        spGraphUpdate(newNode);
                        int tempconnnumb = 0;
                        int tempdistance = 0;
                        if (!alreadyIn) {
                            logger.log(Level.INFO, "Found common node: " + newNode.resource_id);
                            logger.log(Level.INFO, "Retval size: " + retval.size());
                        } else {
                            logger.log(Level.INFO, "Retval updated: " + newNode.resource_id);
                            logger.log(Level.INFO, "Retval size: " + retval.size());
                        }
                        for (int l = 0; l < newNode.parents.length; l++) {
                            if (newNode.parents[l] != null) {
                                tempconnnumb++;
                                tempdistance += newNode.distance[l];
                            }
                        }
                        if (tempconnnumb > maxConn || minDistance == -1 || minDistance > tempdistance) {
                            maxConn = tempconnnumb;
                            minDistance = tempdistance;
                        }
                    }
                }
            }
        }
        return newNode;
    }

    /**
     * Inner class to download node contents via http.
     */
    private class GetNodeContentThread implements Runnable {

        /**
         * The constructor of the class
         */
        private GetNodeContentThread() {
        }

        /**
         * The run method of the thread. The thread will stop when there are no
         * more consumable items in the actual iteration map or if the final
         * graph size is above MAX_GRAPH_SIZE.
         */
        public void run() {
            while (!actualIterationNodes.isEmpty() && finalGraph.size() < MAX_GRAPH_SIZE && retval.size() < nodesMax) {
                Node actualNode = getNextNode();

                // Take one element from the selected fifo
                if (actualNode == null) {
                    return;
                }

                // Download the local representation of the node
                if (retval.get(actualNode.resource_id) == null) {
                    actualNode.getConnections();
                }

                moveNodeToFinalGraph(actualNode.resource_id);
            }
            logger.log(Level.FINE, "Download thread finished");
        }
    }

    /**
     *
     * @return
     */
    public HashMap<String, Node> cleanupResultGraph() {
        logger.log(Level.INFO, "Result cleanup started - " + retval.size());
        logger.log(Level.INFO, "mindistance: " + minDistance);
        ArrayList<Node> commonNodes = new ArrayList<Node>();
        HashMap<String, Node> actretval = new HashMap<String, Node>();

        if (retval.isEmpty()) {
            return actretval;
        }
        int parentsSize = retval.get(retval.keySet().iterator().next()).parents.length;
        Iterator ri = retval.keySet().iterator();

        ArrayList<String> toProcess = new ArrayList<String>();
        // Find the nodes which has 2 non empty parent array
        while (ri.hasNext()) {
            Node tempNode = retval.get(ri.next());
            boolean askNext = false;
            for (int i = 0; i < parentsSize; i++) {
                for (int j = 0; j < parentsSize; j++) {
                    if (i != j && tempNode.parents[i] != null && tempNode.parents[j] != null) {
                        toProcess.add(tempNode.resource_id);
                        askNext = true;
                        break;
                    }
                }
                if (askNext) {
                    break;
                }
            }
        }

        logger.log(Level.INFO, "Common nodes found");

        // Propagate the parent status from the first source to all reachable one
        while (!toProcess.isEmpty()) {
            String actId = toProcess.remove(0);
            Node tempNode = retval.get(actId);

            for (int i = 0; i < parentsSize; i++) {
                ArrayList parentArray = tempNode.parents[i];
                for (int j = 0; j < parentArray.size(); j++) {
                    Node parentNode = retval.get(parentArray.get(j));
                    boolean toadd = false;
                    for (int k = 0; k < parentsSize; k++) {
                        if (parentNode.parents[k] == null) {
                            parentNode.parents[k] = new ArrayList<String>();
                            parentNode.parents[k].add(actId);
                            parentNode.distance[k] = tempNode.distance[k] + 1;
                            toadd = true;
                        } else {
                            if (parentNode.distance[k] > tempNode.distance[k] + 1) {
                                parentNode.parents[k].remove(0);
                                parentNode.parents[k].add(actId);
                                parentNode.distance[k] = tempNode.distance[k] + 1;
                            }
                        }
                    }
                    if (toadd) {
                        toProcess.add(parentNode.resource_id);
                    }
                }
            }
        }
        logger.log(Level.INFO, "Propagated the parent status from the first source to all reachable one");

        // Remove the nodes which are not on the optimal paths

        ri = retval.keySet().iterator();


        while (ri.hasNext()) {
            Node tempNode = retval.get(ri.next());
            int tempdistance = 0;
            for (int i = 0; i < tempNode.parents.length; i++) {
                if (tempNode.parents[i] != null) {
                    tempdistance += tempNode.distance[i];
                }
            }
            if (tempdistance > minDistance) {
                toProcess.add(tempNode.resource_id);
            }
        }

        while (!toProcess.isEmpty()) {
            String actId = toProcess.remove(0);
            retval.remove(actId);
        }

        // Select routes which summa do not exceed the maxNode property

        ri = retval.keySet().iterator();

        while (ri.hasNext()) {
            HashMap<String, Node> tempList = new HashMap<String, Node>();
            Node tempNode = retval.get(ri.next());
            if (!actretval.containsKey(tempNode.resource_id)) {
                tempList.put(tempNode.resource_id, retval.get(tempNode.resource_id));
                for (int i = 0; i < parentsSize; i++) {
                    if (tempNode.parents[i] != null
                            && !actretval.containsKey(tempNode.parents[i].get(0))
                            && !tempList.containsKey(tempNode.parents[i].get(0))) {
                        toProcess.add(tempNode.parents[i].get(0));
                        tempList.put(tempNode.parents[i].get(0), retval.get(tempNode.parents[i].get(0)));
                        while (!toProcess.isEmpty()) {
                            Node tempTraverseNode = retval.get(toProcess.remove(0));
                            if (tempTraverseNode.parents[i] != null
                                    && !actretval.containsKey(tempTraverseNode.parents[i].get(0))
                                    && !tempList.containsKey(tempTraverseNode.parents[i].get(0))) {
                                toProcess.add(tempTraverseNode.parents[i].get(0));
                                tempList.put(tempTraverseNode.parents[i].get(0), retval.get(tempTraverseNode.parents[i].get(0)));
                            }
                        }
                    }
                }
            }

            if (tempList.size() + actretval.size() <= nodesMax) {
                actretval.putAll(tempList);
            }
        }
        logger.log(Level.INFO, "Postprocess finished " + retval.size() + " vs. " + actretval.size());
        return actretval;
    }
}
