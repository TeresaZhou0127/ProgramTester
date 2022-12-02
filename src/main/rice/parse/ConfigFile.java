package main.rice.parse;

import main.rice.node.APyNode;

import java.util.List;

// TODO: implement the ConfigFile class here

/**
 * An concrete class for a Config file
 */
public class ConfigFile {
    /**
     * Set up a field for the function name under test
     */
    private String funcName;
    /**
     * Set up a field for the nodes to generate the base test set
     */
    private List<APyNode<?>> nodes;
    /**
     * Set up a field for the number of random tests to be generated
     */
    private int numRand;

    /**
     * The constructor for a ConfigFile object
     * @param funcName the function name under test
     * @param nodes the parameter to generate a base test set
     * @param numRand the number of random tests to be generated
     */
    public ConfigFile(String funcName, List<APyNode<?>> nodes, int numRand) {
        this.funcName = funcName;
        this.nodes = nodes;
        this.numRand = numRand;
    }

    /**
     * Generate the function name under test
     * @return a String; the function name under test
     */
    public String getFuncName() {
        return this.funcName;
    }

    /**
     * Get the nodes for generating base test set
     * @return a List of APyNodes
     */
    public List<APyNode<?>> getNodes() {
        return this.nodes;
    }

    /**
     * Get the number of random tests to be generated
     * @return an int; the number of random tests to be generated
     */
    public int getNumRand() {
        return this.numRand;
    }
}