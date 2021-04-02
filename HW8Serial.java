/**
* @author Nolan Orloff
* a serial implementation of Dijkstra's SSSP
* uses same data and path class as parallel solution to ensure valid
* comparison
* prints out the shortest-path tree for the input graph
*/

import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class HW8Serial {
    // file to read input graph from
    public static final String FILENAME = "sampledata.dat";

    // vertex to use as root
    // must be less than total number of vertices in graph
    public static final int START_NODE = 0;

    // number of vertices in graph
    public static int n_nodes;

    // table representation of edges
    // used to find edges that start at a specific vertex
    public static int[][] graph;

    /**
    * read edges from the supplied file
    * and assign n_nodes to provided value
    *
    * @return a list of the edges in the file
    */
    public static List<Data.Edge> getObservations() throws IOException, ClassNotFoundException {
        List<Data.Edge> out = new ArrayList<Data.Edge>();
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
        n_nodes = (Integer) in.readObject();
        Data.Edge obs = (Data.Edge) in.readObject();
        while(!obs.isEOF()) {
            out.add(obs);
            obs = (Data.Edge) in.readObject();
        }
        return out;
    }

    /**
    * transform list of edges to table representation
    *
    * @param edges list of edges read from supplied file
    *
    * @return n_nodes x n_nodes table where cell (start, end) = weight
    */
    public static int[][] makeGraph(List<Data.Edge> edges) {
        int[][] out = new int[n_nodes][n_nodes];
        for (Data.Edge i : edges) {
            out[i.start][i.end] = i.weight;
        }
        return out;
    }

    /**
    * get all edges in the graph from a vertex to unvisited vertices
    *
    * @param node the source vertex to search using
    * @param visited destination nodes to exclude
    */
    public static List<Data.Edge> getChildren(int node, List<Integer> visited) {
        List<Data.Edge> out = new ArrayList<Data.Edge>();
        for (int i = 0; i < n_nodes; i++) {
            if (graph[node][i] > 0 && !visited.contains((Integer) i))
                out.add(new Data.Edge(node, i, graph[node][i])); 
        }
        return out;
    }

    /**
    * get a path that ends at the specified vertex
    * at any point in time, there can be either 0
    * or 1 paths that end at the vertex in the paths list
    *
    * @param node the vertex to search for
    * @param paths the list of paths to search in
    *
    * @return a path that ends at the vertex or a path with no edges in it
    */
    public static HW8.Path getPathWithEnd(int node, List<HW8.Path> paths) {
        for (HW8.Path i : paths) {
            if (i.tail().end == node)
                return i;
        }
        return new HW8.Path();
    }

    /**
    * run Dijkstra's SSSP on the supplied dataset
    *
    * @param start the root node of the shortest-path tree
    *
    * @return a list of paths that represents a shortest-path tree of the graph
    */
    public static List<HW8.Path> dijkstras(int start) {
        List<HW8.Path> paths = new ArrayList<HW8.Path>();               // initialize data structures for algorithm
        List<Integer> visited = new ArrayList<Integer>();
        List<Data.Edge> frontier = new ArrayList<Data.Edge>();
        paths.add(new HW8.Path(new Data.Edge(start, start, 0)));        // add the identity path as a start point for other paths
        visited.add(start);                                             // visit start
        frontier.addAll(getChildren(start, visited));                   // and add its associated edges to the frontier
        while(frontier.size() > 0) {                                    
            Collections.sort(frontier, new Comparator<Data.Edge>() {    // sort the frontier so the first edge has the lowest weight
                public int compare(Data.Edge left, Data.Edge right) {
                    return left.weight - right.weight;
                }
            });
            Data.Edge nextEdge = frontier.get(0);                       // pop the first edge
            frontier.remove(0);
            HW8.Path head = getPathWithEnd(nextEdge.start, paths);      // get the path that ends at the start of this edge
            visited.add(nextEdge.end);                                  // traverse the edge and visit its destination vertex
            frontier.addAll(getChildren(nextEdge.end, visited));        // and add its associated edges to the frontier
            HW8.Path nextPath = head.add(nextEdge);                     // make the path that ends at the new vertex
            HW8.Path oldPath = getPathWithEnd(nextEdge.end, paths);     // is there already a path that ends at this vertex?
            if (oldPath.size() == 0 || (oldPath.size() > 0 && nextPath.cost() < oldPath.cost())) {
                paths.remove(oldPath);                                  // if it's more costly than the new path, replace it
                paths.add(nextPath);
            }
        }
        paths.remove(0);                                                // don't return the identity path
        return paths;                                                   // return the other paths
    }

    /**
    * run the algorithm on the provided graph and print the shortest-path tree
    * 
    * @param args unused
    */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        graph = makeGraph(getObservations());
        List<HW8.Path> result = dijkstras(START_NODE);
        for (HW8.Path i : result) {
            System.out.println(i);
        }
    }
}