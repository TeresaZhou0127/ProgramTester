package main.rice.parse;

import main.rice.node.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class for parsing the config files.
 */
public class ConfigFileParser {

    /**
     * Read out the contents of the given file
     * @param filepath the path to the file
     * @return the content of the file
     * @throws IOException if the file is invalid
     */
    public String readFile(String filepath) throws IOException {
        return Files.readString(Paths.get(filepath));
    }

    /**
     * Parse a config string
     * @param contents a string; the content of a given JSON file
     * @return a ConfigFile of the string after the process of parsing
     * @throws InvalidConfigException if the given config file is invalid
     */
    public ConfigFile parse(String contents) throws InvalidConfigException {
        // convert the JSON file into a JSONObject
        JSONObject obj = this.getJSON(contents);
        // check if the JSON file contains all the required fields
        this.checkValidity(obj);
        // read all the fields from the JSON file
        String fname = this.getFname(obj);
        JSONArray types = this.getJSONArray(obj, "types");
        JSONArray exDomain = this.getJSONArray(obj, "exhaustive domain");
        JSONArray ranDomain = this.getJSONArray(obj, "random domain");
        int numRand = this.getNumRandom(obj);
        // parse to get all the APyNodes
        List<APyNode<?>> APyNodes = this.parseAPyNodes(types, exDomain, ranDomain);
        return new ConfigFile(fname, APyNodes, numRand);
    }

    /**
     * Convert a string to a JSONObject
     * @param content a string; the content of the config
     * @return a JSONObject representing the config string
     * @throws InvalidConfigException if the input string is invalid
     */
    private JSONObject getJSON(String content) throws InvalidConfigException {
        try {
            return new JSONObject(content);
        } catch (JSONException e) {
            throw new InvalidConfigException("invalid JSONObject input");
        }
    }

    /**
     * Generate the function name under test from the given config file
     * @param content an JSONObject; the content of the config file
     * @return the function name under test extracted from the config
     * @throws InvalidConfigException if the function name is not a string
     */
    private String getFname(JSONObject content) throws InvalidConfigException {
        try {
            return content.getString("fname");
        } catch (JSONException e) {
            throw new InvalidConfigException("wrong \"fname\" type; should be String");
        }
    }

    /**
     * Generate a JSONArray from a single field of the JSONObject
     * @param content an JSONObject; the content of the config file
     * @param fieldName the name of a field in the config file
     * @return a JSONArray containing the content of the field
     * @throws InvalidConfigException if the content of the field is not a valid JSONArray
     */
    private JSONArray getJSONArray(JSONObject content, String fieldName) throws InvalidConfigException {
        try {
            JSONArray array = content.getJSONArray(fieldName);
            // make sure that every position in the array is a string
            for (int i = 0; i < array.length(); ++i) {
                array.getString(i);
            }
            return array;
        } catch (JSONException e) {
            throw new InvalidConfigException(String.format("wrong \"%s\" type; should be Array of strings", fieldName));
        }
    }

    /**
     * Generate the random number from the given JSONObject
     * @param obj an JSONObject; the content of the config file
     * @return an Integer which is the random number for the test cases
     * @throws InvalidConfigException if the random number is not an Integer
     */
    private int getNumRandom(JSONObject obj) throws InvalidConfigException {
        try {
            Object data = obj.get("num random");
            // if the random number is not an integer, throws an exception
            if (!(data instanceof Integer))
                throw new InvalidConfigException("invalid \"num random\" type; should be Integer");
            int numRandom = obj.getInt("num random");
            // if the random number is less than zero, throws an exception
            if (numRandom < 0) throw new InvalidConfigException("num random should be greater than 0");
            return numRandom;

        } catch (JSONException e) {
            throw new InvalidConfigException("invalid \"num random\" type; should be Integer");
        }
    }

    /**
     * Check if the given config file contains all the required fields
     * @param content a JSONObject; the content of the config file
     * @throws InvalidConfigException if the given JSONObject misses any required field
     */
    private void checkValidity(JSONObject content) throws InvalidConfigException {
        boolean invalid = false;
        // generate an error message
        StringBuilder error = new StringBuilder("JSON missing fields: ");
        for (String str : List.of("fname", "types", "exhaustive domain", "random domain", "num random")) {
            // The value is null if obj misses the corresponding field
            if (content.opt(str) == null) {
                invalid = true;
                // Build the error message
                error.append(str).append(", ");
            }
        }
        // if there are missing fields, use the error message constructed
        // above to throw an exception
        if (invalid) {
            throw new InvalidConfigException(error.substring(0, error.length()-2));
        }
    }

    /**
     * Parse the types, exDomain, and ranDomain and generate a list of APyNodes corresponding to that.
     * @param types a JSONArray; strings of types
     * @param exDomain a JSONArray; strings of exhaustive domains
     * @param ranDomain a JSONArray; strings of random domains
     * @return a list of APyNodes
     * @throws InvalidConfigException if the given JSONArrays are invalid
     */
    private List<APyNode<?>> parseAPyNodes(JSONArray types, JSONArray exDomain, JSONArray ranDomain) throws InvalidConfigException {
        // check if the types, exDomain and ranDomain are all the same length
        if (types.length() != exDomain.length() || types.length() != ranDomain.length()) {
            throw new InvalidConfigException("Inconsistent lengths for types and domains");
        }
        List<APyNode<?>> APyNodes = new ArrayList<>();
        // parse each single node one after one
        for (int i = 0; i < types.length(); ++i) {
            APyNodes.add(this.parseSingleNode(types.getString(i).strip(), exDomain.getString(i).strip(),
                    ranDomain.getString(i).strip()));
        }
        return APyNodes;
    }

    /**
     * Parse a single APyNode.
     * @param singleType a string; the type of APyNode
     * @param singleExDomain a string; the exhaustive domain of the single APyNode
     * @param singleRanDomain a string; the random domain of the single APyNode
     * @return an APyNode with it corresponding exhaustive domain and random domain
     * @throws InvalidConfigException if singleType, singleExDomain, or singleExDomain is invalid
     */
    private APyNode<?> parseSingleNode(String singleType, String singleExDomain, String singleRanDomain) throws InvalidConfigException {
        APyNode<?> node = this.parseType(singleType);
        // generate the exDomain and the ranDomain based on the type of the node
        List<Number> exDomain = this.parseDomain(singleExDomain, node, true);
        List<Number> ranDomain = this.parseDomain(singleRanDomain, node, false);
        node.setExDomain(exDomain);
        node.setRanDomain(ranDomain);
        return node;
    }

    /**
     * Parse the Python type.
     * @param type a string; a Python type
     * @return an APyNode of the given type
     * @throws InvalidConfigException if the string is an invalid Python type
     */
    private APyNode<?> parseType(String type) throws InvalidConfigException {
        int delimiterIdx = type.indexOf("(");
        // if there is no "(" in the type, it is a simple Python object
        if (delimiterIdx < 0) {
            return this.parseSimplePythonType(type);
        }
        // when the type is a string
        if (type.startsWith("str")) {
            return this.parseString(type.substring(delimiterIdx+1));
        }
        // when the type is a dictionary
        if (type.startsWith("dict")) {
            return this.parseDict(type.substring(delimiterIdx+1));
        }
        // when the type is not one of above, then it is an iterable type
        return this.parseIterableType(type);
    }

    /**
     * Parse simple Python object types.
     * @param type a string; the type of the object
     * @return an new APyNode of the input type
     * @throws InvalidConfigException if the string type is invalid
     */
    private APyNode<?> parseSimplePythonType(String type) throws InvalidConfigException {
        // construct new simple Python objects based on their types
        switch (type.strip()) {
            case "int":
                return new PyIntNode();
            case "float":
                return new PyFloatNode();
            case "bool":
                return new PyBoolNode();
            default:
                throw new InvalidConfigException(String.format("wrong Python simple type: \"%s\"", type));
        }
    }

    /**
     * Parse the value of a string type.
     * @param content a string representing the value of a string type
     * @return a new PyStringNode consisting of the input string
     */
    private PyStringNode parseString(String content){
        return new PyStringNode(content);
    }

    /**
     * Parse the dictionary types.
     * @param dict a string of the content of a Python dictionary
     * @return a PyDictNode
     * @throws InvalidConfigException if the string is not a valid Python dictionary
     */
    private PyDictNode<?,?> parseDict(String dict) throws InvalidConfigException{
        int delimiterIdx = dict.indexOf(":");
        // Throw an exception if there is no ':'
        if (delimiterIdx < 0) throw new InvalidConfigException("wrong Python dictionary type");
        return new PyDictNode<>(this.parseType(dict.substring(0, delimiterIdx)),
                this.parseType(dict.substring(delimiterIdx+1)));
    }

    /**
     * Parse iterable types.
     * @param iterable a string of an iterable type
     * @return a APyNode containing that type
     * @throws InvalidConfigException if the string is not a valid Python iterable type
     */
    private APyNode<?> parseIterableType(String iterable) throws InvalidConfigException {
        int delimiterIdx = iterable.indexOf("(");
        // when the iterable type is a list
        if (iterable.startsWith("list")) {
            return new PyListNode<>(this.parseType(iterable.substring(delimiterIdx + 1).strip()));
        }
        // when the iterable type is a tuple
        if (iterable.startsWith("tuple")) {
            return new PyTupleNode<>(this.parseType(iterable.substring(delimiterIdx + 1).strip()));
        }
        // when the iterable type is a set
        if (iterable.startsWith("set")) {
            return new PySetNode<>(this.parseType(iterable.substring(delimiterIdx+1).strip()));
        }

        throw new InvalidConfigException("wrong Python iterable type");
    }

    /**
     * Parse the domain of a node.
     * @param domain a string; the domain of the node
     * @param type an APyNode; the type of the node
     * @param isExhaustive true if parsing an exhaustive domain; false if parsing a random domain
     * @return a list of Numbers; the domain of the node; either exhaustive or random
     * @throws InvalidConfigException if the domain or type is invalid
     */
    private List<Number> parseDomain(String domain, APyNode<?> type, boolean isExhaustive) throws InvalidConfigException {
        // if the domain contains "(", then it is a simple or iterable domain
        if (!domain.contains("(")) return this.parseSimpleIterableDomain(domain, type);
        // if the domain contains ":", then it is a compound domain
        if (!domain.contains(":")) return this.parseCompoundDomain(domain, type, isExhaustive);
        // otherwise, it is a dictionary domain
        return this.parseDictDomain(domain, type, isExhaustive);
    }

    /**
     * Parse a simple or an iterable domain.
     * @param domain a string; the domain to be parsed
     * @param type the node type
     * @return a list of Numbers; the domain after parsing
     * @throws InvalidConfigException if the domain is invalid
     */
    private List<Number> parseSimpleIterableDomain(String domain, APyNode<?> type) throws InvalidConfigException {
        // if the domain contains "~", then it is a range
        if (domain.indexOf("~") > 0) return this.parseRange(domain, type);
        // otherwise, it is an array
        return this.parseArrayDomain(domain, type);
    }

    /**
     * Parse an interval domain.
     * @param domain a string of domain
     * @param type the type of the node
     * @return a list of Numbers representing the domain
     * @throws InvalidConfigException if the domain is invalid
     */
    private List<Number> parseRange(String domain, APyNode<?> type) throws InvalidConfigException {
        int delimiterIdx = domain.indexOf("~");
        int lower;
        int upper;
        // check if the upper and lower bounds of a range is integers or not
        try {
            lower = Integer.parseInt(domain.substring(0, delimiterIdx).strip());
            upper = Integer.parseInt(domain.substring(delimiterIdx + 1).strip());
        } catch (Exception e) {
            throw new InvalidConfigException("invalid upper or lower bound in a range of the domain");
        }
        // if upper bound is larger than the lower bound, throw an exception
        if (upper < lower) throw new InvalidConfigException("upper bound should be larger than the lower bound");
        // construct the result list which contains all the valid values within
        // the range in the domain
        List<Number> res = new ArrayList<>();
        for (int i = lower; i <= upper; ++i) {
            if (type instanceof PyIntNode) {
                res.add(i);
            }
            else if (type instanceof PyFloatNode) {
                res.add((double) i);
            }
            else if (type instanceof PyBoolNode) {
                // check if the boolean value is 1 or 0
                if(i!=0 && i!=1) throw new InvalidConfigException("wrong boolean domain value");
                res.add(i);
            } else  {
                if (i < 0) throw new InvalidConfigException("string domain values should be larger than 0");
                res.add(i);
            }
        }
        return res;
    }

    /**
     * Parse the domain which is represented by an array.
     * @param domain a string; the array domain
     * @param type the node type
     * @return a list of Numbers; the domain after parsing
     * @throws InvalidConfigException if the domain is invalid
     */
    private List<Number> parseArrayDomain(String domain, APyNode<?> type) throws InvalidConfigException {
        // get rid of the staring and ending square brackets and split the array on ","
        String[] content = domain.replaceAll("\\[|\\]", "").split(",");
        // use set to prevent duplication
        Set<Number> res = new HashSet<>();
        // add values of the array domain in to the set above one after one
        for (String num : content) {
            try {
                if (type instanceof PyIntNode) {
                    res.add(Integer.parseInt(num.strip()));
                }
                else if (type instanceof PyFloatNode) {
                    res.add(Double.parseDouble(num.strip()));
                }
                else if (type instanceof PyBoolNode) {
                    // check if the boolean value is 1 or 0
                    if (!num.strip().equals("0") && !num.strip().equals("1")) throw new Exception();
                    res.add(Integer.parseInt(num.strip()));
                } else {
                    if (Integer.parseInt(num.strip()) < 0) throw new Exception();
                    res.add(Integer.parseInt(num.strip()));
                }
            } catch (Exception e) {
                throw new InvalidConfigException("invalid type and domain");
            }
        }
        // return the list instead of the set for future use
        return new ArrayList<>(res);
    }

    /**
     * Parse a compound domain including "(", but not containing ":"
     * @param domain a string; the domain
     * @param type an APyNode; the type of the node
     * @param isExhaustive true if an exhaustive domain; false if a random domain
     * @return a list of Numbers; the domain after parsing
     * @throws InvalidConfigException if the domain or the type is invalid
     */
    private List<Number> parseCompoundDomain(String domain, APyNode<?> type, boolean isExhaustive) throws InvalidConfigException {
        int delimiterIdx = domain.indexOf("(");
        // check if the "(" is at the start or the end of the domain
        if (delimiterIdx == 0 || delimiterIdx == domain.length()-1)
            throw new InvalidConfigException("invalid domain syntax");
        // check if type has a leftChild; if not, it is not valid
        if (type.getLeftChild() == null) throw new InvalidConfigException("invalid domain");

        if (isExhaustive) {
            // parse the exhaustive domain of the leftChild
            type.getLeftChild().setExDomain(this.parseDomain(domain.substring(delimiterIdx+1),
                    type.getLeftChild(), true));
        } else {
             // parse the random domain of the leftChild
            type.getLeftChild().setRanDomain(this.parseDomain(domain.substring(delimiterIdx+1),
                    type.getLeftChild(), false));
        }

        return this.parseSimpleIterableDomain(domain.substring(0, delimiterIdx), type);
    }

    /**
     * Parse a compound domain including "(" and ":"
     * @param domain a string; the compound domain
     * @param type an APyNode; type of the node
     * @param isExhaustive true if an exhaustive domain; false if a random domain
     * @return a list of Numbers; the domain after parsing
     * @throws InvalidConfigException if the domain or the type is invalid
     */
    private List<Number> parseDictDomain(String domain, APyNode<?> type, boolean isExhaustive) throws InvalidConfigException {
        int delimiterIdx1 = domain.indexOf("(");
        int delimiterIdx2 = domain.indexOf(":");
        // check if the "(" and the ":" are at the start or the end of the domain
        // if so, it is invalid
        if (delimiterIdx1 == 0 || delimiterIdx1==domain.length()-1
                || delimiterIdx2 == 0 || delimiterIdx2 == domain.length()-1)
            throw new InvalidConfigException("wrong domain syntax");
        // check if the type have both leftChild and rightChild
        // if it doesn't have any one of the two, it is invalid
        if (type.getLeftChild() == null || type.getRightChild() == null)
            throw new InvalidConfigException("inconsistent domain");
        if (isExhaustive) {
            // parse exhaustive domains of both children
            type.getLeftChild().setExDomain(this.parseDomain(domain.substring(delimiterIdx1 + 1, delimiterIdx2),
                    type.getLeftChild(), true));
            type.getRightChild().setExDomain(this.parseDomain(domain.substring(delimiterIdx2 + 1),
                    type.getRightChild(), true));
        } else {
            // parse random domains of both children
            type.getLeftChild().setRanDomain(this.parseDomain(domain.substring(delimiterIdx1 + 1, delimiterIdx2),
                    type.getLeftChild(), false));
            type.getRightChild().setRanDomain(this.parseDomain(domain.substring(delimiterIdx2 + 1),
                    type.getRightChild(), false));
        }

        return this.parseSimpleIterableDomain(domain.substring(0, delimiterIdx1), type);
    }
}
