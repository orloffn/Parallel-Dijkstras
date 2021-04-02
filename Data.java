/**
* @author Nolan Orloff
* 
* Create a sample graph and output it to a file
* The graph is formatted as a list of weighted edges
* Also output the number of vertices in the graph
* This is necessary because the graph is not contained in one object
* and the number of edges in the graph can be anywhere from 0 to the 
* number of vertices squared
*/

import java.io.*;
import java.util.Random;
import java.lang.Math;

class Data {
    // the file to write the graph to
    public static final String FILENAME = "sampledata.dat";

    // an edge with start and end equal to this signals the end of the graph
    // do not include that edge in the graph
    public static final int EOF_FLAG = -1;

    // the number of nodes in the graph
    // some nodes may not be reachable
    public static final int NUM_NODES = 1 << 10;


    /**
    * one edge in the graph
    * base data type used to create paths and shortest-path trees
    *
    * @param start the source vertex for the edge
    * @param end the end vertex for the edge
    * @param weight the cost of traversing the edge
    */
    public static class Edge implements Serializable {
        public int start;
        public int end;
        public int weight;

        /**
        * initialize the class
        *
        * @param start the source vertex for the edge
        * @param end the end vertex for the edge
        * @param weight the cost of traversing the edge
        */
        public Edge(int start, int end, int weight) {
            this.start = start;
            this.end = end;
            this.weight = weight;
        }

        /**
        * initialize an object of the class that represents the end of a graph
        */
        public Edge() {
            this.start = EOF_FLAG;
            this.end = EOF_FLAG;
            this.weight = 0;
        }

        /**
        * does this edge represent the end of a graph?
        *
        * @return true when start and end are the EOF magic number
        */
        public boolean isEOF() {
            return start == EOF_FLAG && end == EOF_FLAG;
        }

        /**
        * get a human-readable representation of the edge
        *
        * @return a string describing the edge
        */
        public String toString() {
            return start + " -> " + end + " {" + weight + "}";
        }
    }

    /**
    * write a series of random edges that satisfy some conditions to a file
    * then read them back and print them as a sanity check
    * must satisfy:
    *     edges do not have the same source and destination
    *     weight is a positive integer that is small enough to be human-meaningful
    *
    * @param args unused
    */
    public static void main(String[] args) {
        try {
            Random rand = new Random();
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILENAME));
            out.writeObject(new Integer(NUM_NODES));
            for (int i = 0; i < NUM_NODES; i++) {
                for (int j = i + 1; j < i + NUM_NODES / 4 && j < NUM_NODES; j++) {
                    out.writeObject(new Edge(i, j, Math.abs(rand.nextInt()) % NUM_NODES));
                }
            }
            for (int i = NUM_NODES - 1; i > NUM_NODES / 2; i--) {
                out.writeObject(new Edge(i, Math.abs(rand.nextInt()) % ((i / 2) - (i / 4)) + (i / 4), Math.abs(rand.nextInt()) % NUM_NODES));
            }
            out.writeObject(new Edge());
            out.close();
        } catch (IOException e) {
            System.out.println("writing to " + FILENAME + "failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }

        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILENAME));
            System.out.println((Integer) in.readObject() + " nodes");
            Edge obs = (Edge) in.readObject();
            while (!obs.isEOF()) {
                System.out.println(obs);
                obs = (Edge) in.readObject();
            }
            in.close();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("reading from " + FILENAME + "failed: " + e);
            e.printStackTrace();
            System.exit(1);
        }
    }
}