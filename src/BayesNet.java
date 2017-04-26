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

        // Connect variable's children to the node
        for (Node n : nodes) {
            for (char pName : n.parentNames) {
                if (varName == pName) {
                    child.children.add(n);
                    break;
                }
            }
        }

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

        // Connect variable's parents and children to the node
        int i = 0;
        for (Node n : nodes) {
            // Connect if n is parent
            if (n.var.getName() == parentNames[i]) {
                n.children.add(child);
            }
            // Connect if n is child
            for (char pName : n.parentNames) {
                if (childName == pName) {
                    child.children.add(n);
                    break;
                }
            }
        }

        // Add the node
        nodes.add(child);
    }

    /**
     * Gets all the variables in the Bayes Net in topological order with
     * evidence vars assigned. Used in enumerateAll().
     * @param evidence given variables in the query
     * @return list of variables in topological order
     */
    public ArrayList<Variable> getEnumVars(TreeSet<Variable> evidence) {
        ArrayList<Variable> vars = topologicalOrder(false, null);

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
     * Gets all the variables in the Bayes Net for eliminateAll().
     * @param evidence given variables in the query
     * @return list of variables
     */
    public ArrayList<Variable> getElimVars(TreeSet<Variable> evidence) {
        ArrayList<Variable> vars = topologicalOrder(true, evidence);

        // Need to eliminate children before parents, so reverse topological order
        Collections.reverse(vars);

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
     * Makes a factor for {@code currVar} using given evidence.
     * @param currVar the variable to make a factor for
     * @param evidence the given evidence
     * @return the factor for {@code currVar}
     */
    public Factor makeFactor(Variable currVar, TreeSet<Variable> evidence) {
        // Get the variables needed for the factor
        TreeSet<Variable> varsToAdd = new TreeSet<>();
        varsToAdd.add(currVar);
        for (Node n : nodes) {
            if (n.var.equals(currVar)) {
                for (char c : n.parentNames) {
                    varsToAdd.add(new Variable(c));
                }
                break;
            }
        }

        // Remove factors in the evidence and set their values
        for (Variable v1 : evidence) {
            for (Variable v2 : varsToAdd) {
                if (v2.equals(v1)) {
                    v2.setValue(v1.getValue());
                }
            }
            varsToAdd.remove(v1);
        }

        Variable[] vars = new Variable[varsToAdd.size()];  // New variables
        double[] probs = new double[1 << vars.length];  // New probabilities
        vars = varsToAdd.toArray(vars);

        // Generate probabilities
        for (int i = 0; i < 1 << vars.length; i++) {
            // Set variable values for the index
            int n = i;
            for (int e = vars.length-1; e >= 0; e--) {
                vars[e].setValue(n%2 == 1);
                n >>= 1;
            }
            // Copy probability for the variable assignment
            probs[i] = getProbability(currVar, varsToAdd);
        }

        // Return the generated factor
        return new Factor(vars, probs);
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
     * @param orderSize true if variables should be ordered by size before
     *                  alphabetical, used for elimination
     * @param evidence given variables in the query
     * @return list of variables in topological order
     */
    private ArrayList<Variable> topologicalOrder(boolean orderSize, TreeSet<Variable> evidence) {
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
            Node curr;
            if (!orderSize) {  // Order for enumeration
                // Just get the next node in alphabetical order with 0 in-degree
                curr = queue.remove();
            } else {  // Order for elimination
                // Since we will be reversing the topological order, we need to sort it descending
                // by factor size then reverse alphabetical order

                // Count factor size for first element in the queue
                Iterator<Node> it = queue.iterator();
                Node max = it.next();
                int maxCount = 1;
                // factorSize-- if Node variable is in the evidence
                for (Variable v : evidence) {
                    if (max.var.getName() == v.getName()) {
                        maxCount = 0;
                        break;
                    }
                }
                // factorSize-- for all parents in the evidence
                for (char par : max.parentNames) {
                    maxCount++;
                    for (Variable v : evidence) {
                        if (par == v.getName()) {
                            maxCount--;
                            break;
                        }
                    }
                }

                // Count factor size for all other elements in the queue
                while (it.hasNext()) {
                    int count = 1;
                    Node n = it.next();
                    // factorSize-- if Node variable is in the evidence
                    for (Variable v : evidence) {
                        if (n.var.getName() == v.getName()) {
                            count = 0;
                            break;
                        }
                    }
                    // factorSize-- for all parents in the evidence
                    for (char par : n.parentNames) {
                        count++;
                        for (Variable v : evidence) {
                            if (par == v.getName()) {
                                count--;
                                break;
                            }
                        }
                    }

                    // Keep the Node with the largest factor size, later alphabetically for ties
                    if (count >= maxCount) {
                        maxCount = count;
                        max = n;
                    }
                }

                // Find the Node and remove it from the queue
                it = queue.iterator();
                while (it.hasNext()) {
                    if (it.next() == max) {
                        it.remove();
                        break;
                    }
                }

                // Use that node for the topological sort
                curr = max;
            }
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
