package main.rice;

// TODO: implement the Main class here

import main.rice.basegen.BaseSetGenerator;
import main.rice.concisegen.ConciseSetGenerator;
import main.rice.parse.ConfigFile;
import main.rice.parse.ConfigFileParser;
import main.rice.parse.InvalidConfigException;
import main.rice.test.TestCase;
import main.rice.test.Tester;

import java.io.IOException;
import java.util.Set;

/**
 * An entry point to the test case generator; tie together all the different
 * components that have been completed in order to generate a concise set of test case
 */
public class Main {
    /**
     * Compute the concise test set
     * @param args an array; the command line argument
     * @throws IOException if an I/O operation fails
     * @throws InvalidConfigException if the config file is of invalid format
     * @throws InterruptedException if the process is interrupted
     */
    public static void main(String[] args) throws IOException, InvalidConfigException, InterruptedException {
        System.out.println(generateTests(args));
    }

    /**
     * An helper for main(); generate the concise test set
     * @param args an array; the command line arguments
     * @return the concise test set
     * @throws IOException if an I/O operation fails
     * @throws InvalidConfigException if the config file is of invalid format
     * @throws InterruptedException if the process is interrupted
     */
    public static Set<TestCase> generateTests(String[] args) throws IOException, InvalidConfigException, InterruptedException {
        // create a new ConfigFileParser
        ConfigFileParser parser = new ConfigFileParser();
        // read the content of the file in the command into a config file
        ConfigFile contents = parser.parse(parser.readFile(args[0]));
        // construct a new BaseSetGenerator based on the parsed config file
        BaseSetGenerator base = new BaseSetGenerator(contents.getNodes(), contents.getNumRand());
        Tester tester = new Tester(contents.getFuncName(), args[2], args[1], base.genBaseSet());
        tester.computeExpectedResults();
        // return the concise test set
        return ConciseSetGenerator.setCover(tester.runTests());

    }

}