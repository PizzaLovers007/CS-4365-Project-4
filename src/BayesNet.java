import java.util.*;

/**
 * Object to represent the Bayesian Network.
 */
public class BayesNet {

    /**
     * Object that represents a node in the Bayes Net.
     */
    private class Node implements Comparable<Node> {
        Variable var;  // Holds information about the variable this node is representing
        double[] probabilities;  // Probabilities given the parent nodes' assignments
        char[] parentNames;  // Names of the parent nodes
        TreeSet<Node> children;  // child nodes

        /**
         * Creates a node in the Bayes Net.
         * @param varName name of the variable
         * @param pNames names of the parents
         * @param probs probabilities given the parents' assignments
         */
        public Node(char varName, char[] pNames, double[] probs) {
            var = new Variable(varName);
            probabilities = probs;
            parentNames = pNames;
            children = new TreeSet<>();
        }

        // Sort by alphabetical order
        public int compareTo(Node other){
            return var.getName() - other.var.getName();
        }
    }

    private TreeSet<Node> nodes;  // Holds all the nodes in the Bayes Net

    /**
     * Creates an empty Bayes Net.
     */
    public BayesNet() {
        nodes = new TreeSet<>();
    }

    /**
     * Adds a new variable with no parents.
     * @param varName name of the variable that is being added
     * @param probability probability that the variable is true
     */
    public void add(char varName, double probability) {
        Node child = new Node(varName, new char[]{}, new double[]{probability});
        nodes.add(child);
    }

    /**
     * Adds a new variable with at least 1 parent.
     * @param parentNames names of the parent variables
     * @param childName name of the child variable that is being added
     * @param probabilities conditional probabilities with the binary string of index mapping
     *                      to the truth assignment (Ex: 100 -> {A=T,B=F,C=F})
     */
    public void add(char[] parentNames, char childName, double[] probabilities) {
        // Create node
        Node child = new Node(childName, parentNames, probabilities);

        // Connect variable's parents to the node
        int i = 0;
        for (Node node : nodes) {
            if (node.var.getName() == parentNames[i]) {
                node.children.add(child);
            }
        }

        // Add the node
        nodes.add(child);
    }

    /**
     * Gets all the variables in the Bayes Net in topological order with
     * evidence vars assigned.
     * @param evidence given variables in the query
     * @return list of variables in topological order
     */
    public ArrayList<Variable> getVars(TreeSet<Variable> evidence) {
        ArrayList<Variable> vars = topologicalOrder();

        // Set evidence variables
        for (Variable v : vars) {
            for (Variable ev : evidence) {
                if (v.getName() == ev.getName()) {
                    v.setValue(ev.getValue());
                    break;
                }
            }
        }

        return vars;
    }

    /**
     * Gets the probability of {@code var} given the current evidence.
     * @param var variable to find the probability of
     * @param evidence given variables in the query
     * @return the probability {@code var} is true/false (depends on
     *         {@code var}'s value
     */
    public double getProbability(Variable var, TreeSet<Variable> evidence) {
        // Find the node of the variable that is being asked for
        Node queryNode = null;
        for (Node n : nodes) {
            if (n.var.getName() == var.getName()) {
                queryNode = n;
                break;
            }
        }

        // Filter out evidence variables not part of the asked variable's parents
        Variable[] importantEvidence = new Variable[queryNode.parentNames.length];
        for (int i = 0; i < importantEvidence.length; i++) {
            for (Variable v : evidence) {
                if (v.getName() == queryNode.parentNames[i]) {
                    importantEvidence[i] = v;
                    break;
                }
            }
        }

        // Generate the index for the probability using the evidence truth assignments
        int index = 0;
        for (Variable v : importantEvidence) {
            index <<= 1;
            if (v.getValue()) {
                index |= 1;
            }
        }

        // Return the value (1-value if asked for false)
        if (var.getValue()) {
            return queryNode.probabilities[index];
        } else {
            return 1 - queryNode.probabilities[index];
        }
    }

    /**
     * Gets the number of variables in the Bayes Net.
     * @return the number of variables in the Bayes Net
     */
    public int size() {
        return nodes.size();
    }

    /**
     * Gets the list of variables in topological order.
     * @return list of variables in topological order
     */
    private ArrayList<Variable> topologicalOrder() {
        // Count the number of parents each variable has
        HashMap<Character, Integer> map = new HashMap<>();
        for (Node n : nodes) {
            map.putIfAbsent(n.var.getName(), 0);
            for (Node child : n.children) {
                map.putIfAbsent(child.var.getName(), 0);
                map.put(child.var.getName(), map.get(child.var.getName())+1);
            }
        }

        // Put all no-parent variables in a queue
        LinkedList<Node> queue = new LinkedList<>();
        for (Node n : nodes) {
            if (map.get(n.var.getName()) == 0){
                queue.add(n);
            }
        }

        // Get topological order
        ArrayList<Variable> vars = new ArrayList<>();
        while (!queue.isEmpty()) {
            Node curr = queue.remove();
            vars.add(new Variable(curr.var.getName()));

            for (Node child : curr.children) {
                // Decrement parent count by 1 from the current child
                int numParents = map.get(child.var.getName())-1;
                map.put(child.var.getName(), numParents);

                // Add to queue if no parents remaining to be searched
                if (numParents == 0) {
                    queue.add(child);
                }
            }
        }

        return vars;
    }
}
