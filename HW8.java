/**
* @author Nolan Orloff
* 
* perform the parallel implementation of Dijkstra's SSSP 
* and output the shortest-path tree to the command line
*
* @see Data.java
*/

import java.util.List;
import java.util.ArrayList;
import java.io.*;


class HW8 {
    // file to read input graph from
    public static final String FILENAME = "sampledata.dat";

    // number of threads to allocate for algorithm
    public static final int NUM_THREADS = 8;

    // vertex to use as root
    // must be less than total number of vertices in graph
    public static final int START_NODE = 0;

    // number of vertices in graph
    public static int n_nodes;

    /**
    * a path is a list of edges
    * the source vertex of the first edge in a path is always the root vertex
    * a path is valid if all edges in it exist in the graph and if
    * the source vertex for any edge is the destination vertex for the edege
    * immediately before it
    *
    * @param edges the edges that form the path
    */
    public static class Path {
        private List<Data.Edge> edges;

        /**
        * initialize a path with no edges in it
        */
        public Path() {
            edges = new ArrayList<Data.Edge>();
        }

        /**
        * initialize a path with a single edge in it
        *
        * @param e the edge to initialize with
        */
        public Path(Data.Edge e) {
            edges = new ArrayList<Data.Edge>();
            edges.add(e);
        }

        /**
        * initialize a path from a valid list of edges
        *
        * @param e edges to form a path using
        *           must follow above path rules
        */
        public Path(List<Data.Edge> e) {
            edges = new ArrayList<Data.Edge>();
            for (Data.Edge i : e) {
                edges.add(i);
            }
        }

        /**
        * get a new path containing the edges in this path with a new
        * edge added to the end
        *
        * @param e edge to add to this path
        *
        * @return a new path with the new edge at the end
        */
        public Path add(Data.Edge e) {
            List<Data.Edge> out = new ArrayList<Data.Edge>(edges);
            out.add(e);
            return new Path(out);
        }

        /**
        * get the total cost of the path
        * cost is the sum of weights of edges on the path
        *
        * @return sum of weights of edges
        */
        public int cost() {
            int sum = 0;
            for (Data.Edge i : edges) {
                sum += i.weight;
            }
            return sum;
        }

        /**
        * get the number of edges in the path
        *
        * @return number of edges
        */
        public int size() {
            return edges.size();
        }

        /**
        * get a copy of the edges in the path
        *
        * @return a new list of edges with the same values at the
        *          edges in this list
        */
        public List<Data.Edge> getEdges() {
            return new ArrayList<Data.Edge>(edges);
        }

        /**
        * get a reference to the last edge in the path
        *
        * @return the last edge in the path
        */
        public Data.Edge tail() {
            return edges.get(edges.size() - 1);
        }

        /**
         * does the path contain an edge that starts or ends at the
         * provided vertex?
         *
         * @param node the vertex to search for
         *
         * @return true if there is a node that starts or ends at the vertex
        */
        public boolean contains(int node) {
            for (Data.Edge i : edges) {
                if (i.start == node || i.end == node) return true;
            }
            return false;
        }

        /**
         * get a human-readable representation of the path
         *
         * @return a readable representation of the edges in and total
         *         cost of the path
        */
        public String toString() {
            String out = "";
            for (int i = 1; i < edges.size(); i++) {
                out += "{" + edges.get(i).start + ", " + edges.get(i).end + "} ";
            }
            out += "Cost: " + cost();
            return out;
        }
    }

    /**
    * Tally class for running Dijkstra's SSSP in parallel
    *
    * input is a list of edges
    * tally object has a 2d array of known edges and a list of known paths to start node
    *     on accum, add new edge to array
    *     on combine, add all new edges to array
    * at each step:
    *     look for paths to new nodes
    *     if a path exists and either:
    *         the list of paths does not have a path that ends at this path's end node
    *         this path costs less than a path in the list with the same end node
    *     add or replace the new path in the list
    * 
    * @param edges table of known edges
    * @param paths list of known shortest paths
    * @param start the root node for the shortest-path tree
    */
    public static class Dijkstras implements Tally<Data.Edge>, Cloneable {
        private int[][] edges;
        private List<Path> paths;
        private int start;

        /**
        * initialize the object 
        *
        * @param start root node for the shortest-path tree
        * @param num_nodes number of nodes in the graph
        */
        public Dijkstras(int start, int num_nodes) {
            this.start = start;
            this.edges = new int[num_nodes][num_nodes];
            this.paths = new ArrayList<Path>();
            this.paths.add(new Path(new Data.Edge(start, start, 0)));
        }

        /**
        * get a new object with the same data as this one
        *
        * @return unique object with same edges and paths
        */
        public Dijkstras clone() {
            Dijkstras other = new Dijkstras(start, edges.length);
            for (int row = 0; row < this.edges.length; row++) {
                for (int col = 0; col < this.edges.length; row++) {
                    other.edges[row][col] = this.edges[row][col];
                }
            }
            other.paths = new ArrayList<Path>(this.paths);
            return other;
        }

        /**
        * get a human-readable representation of the shortest-path tree
        * in this obect
        *
        * @return readable represetation of paths this object knows about
        */
        @Override
        public String toString() {
            String out = "";
            for (int i = 1; i < paths.size(); i++) {
                out += paths.get(i).toString() + "\n";
            }
            return out;
        }

        /**
        * get a copy of the paths this object knows about
        * 
        * @return new list with the same data as this object's paths
        */
        public List<Path> getPaths() {
            return new ArrayList<Path>(paths);
        }

        /**
        * This method acts as a factory to create new objects of the same type.
        *
        * @return a new identity object
        */
        @Override
        public Dijkstras init() {
            return new Dijkstras(start, edges.length);
        }

        /**
        * Accumulate a data element into this object.
        *
        * @param elem data element to accumulate into the obhect
        */
        @Override
        public void accum(Data.Edge elem) {
            edges[elem.start][elem.end] = elem.weight;
            updatePaths(elem);
        }

        /**
        * Combing this object with another.
        * We assume that the the other is the exact same type and size.
        * Used to combine subtrees in a reduction or scan.
        *
        * @param other the right side of the reduction evaluation tree
        */
        @Override
        public void combine(Tally<Data.Edge> other){
            Dijkstras right = (Dijkstras) other;
            for (int i = 0; i < right.edges.length; i++) {
                for (int j = 0; j < right.edges.length; j++) {
                    if (this.edges[i][j] == 0 && right.edges[i][j] != 0)
                        accum(new Data.Edge(i, j, right.edges[i][j]));
                }
            }
        }
        /**
        * look for new shortest paths that end at the new edge
        *
        * @param newEdge a new edge that was just added to the graph
        */
        private void updatePaths(Data.Edge newEdge) {
            Path head = getPathWithEnd(newEdge.start);
            if (head.size() > 0) {
                Path newPath = head.add(newEdge);
                Path oldPath = getPathWithEnd(newEdge.end);
                if (oldPath.size() > 0 && newPath.cost() < oldPath.cost()) {
                    paths.remove(oldPath);
                    paths.add(newPath);
                    List<Path> toUpdate = getPathsContains(newEdge.end);
                    for (Path old : toUpdate) {
                        List<Data.Edge> edges = old.getEdges();
                        int i = edges.indexOf(findEdgeWithEnd(edges, newEdge.end));
                        if (i >= 0) {
                            edges = edges.subList(i, edges.size());
                            edges.addAll(0, newPath.getEdges());
                            paths.remove(old);
                            paths.add(new Path(edges));
                        }
                    }
                }
                else if (oldPath.size() == 0)
                    paths.add(newPath);
            }
        }

        /**
        * get the path that ends at the specified vertex
        *
        * @param node the vertex to search for
        * 
        * @return the path in paths that ends at the vertex
        */
        private Path getPathWithEnd(int node) {
            for (Path i : paths) {
                if (i.tail().end == node)
                    return i;
            }
            return new Path();
        }

        /**
        * get all the paths that contain the specified vertex
        *
        * @param node the vertex to search for
        *
        * @return the set of paths that contain the vertex
        */
        private List<Path> getPathsContains(int node) {
            List<Path> out = new ArrayList<Path>();
            for (Path i : paths) {
                if (i.contains(node))
                    out.add(i);
            }
            return out;
        }

        /**
        * get the index of the edge in a list of edges that ends at the specified node
        *
        * @param edges the set of edges to search
        * @param node the vertex to search for
        *
        * @return the index of the edge or -1 if no edge ends at the vertex
        */
        private int findEdgeWithEnd(List<Data.Edge> edges, int node) {
            for (int i = 0; i < edges.size(); i++) {
                if (edges.get(i).end == node)
                    return i;
            }
            return -1;
        }
    }

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
    * calculate and print the shortest-path tree for the graph in the supplied file
    *
    * @param arg unused
    */
    public static void main(String[] args) throws FileNotFoundException, InterruptedException, IOException, ClassNotFoundException {
        List<Data.Edge> obs = getObservations();
        Dijkstras factory = new Dijkstras(START_NODE, n_nodes);
        Dijkstras reduction = (Dijkstras) new Reduce(obs, NUM_THREADS, factory).reduce();
        System.out.println(reduction);
    }
}