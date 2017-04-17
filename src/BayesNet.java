import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

public class BayesNet {

    private class Node implements Comparable<Node> {
        Variable var;
        double[] probabilities;
        char[] parentNames;
        ArrayList<Node> children;

        public Node(char varName, char[] pNames, double[] probs) {
            var = new Variable(varName);
            probabilities = probs;
            parentNames = pNames;
            children = new ArrayList<>();
        }

        // Sort by alphabetical order
        public int compareTo(Node other){
            return var.getName() - other.var.getName();
        }
    }

    ArrayList<Node> nodes;

    public BayesNet() {
        nodes = new ArrayList<>();
    }

    /**
     * Adds a new variable with no parents.
     * @param varName name of the variable that is being added
     * @param probability probability that the variable is true
     */
    public void add(char varName, double probability) {
        Node child = new Node(varName, new char[]{}, new double[]{probability});
        nodes.add(~Collections.binarySearch(nodes, child), child);
    }

    /**
     * Adds a new variable with 1 or more parents.
     * @param parentNames names of the parent variables
     * @param childName name of the child variable that is being added
     * @param probabilities conditional probabilities with the binary string of index mapping
     *                      to the truth assignment (Ex: 100 -> {A=T,B=F,C=F})
     */
    public void add(char[] parentNames, char childName, double[] probabilities) {
        Node child = new Node(childName, parentNames, probabilities);
        int i = 0;
        for (Node node : nodes) {
            if (node.var.getName() == parentNames[i]) {
                node.children.add(~Collections.binarySearch(node.children, child), child);
            }
        }
        nodes.add(~Collections.binarySearch(nodes, child), child);
    }

    public ArrayList<Variable> getVars(ArrayList<Variable> evidence) {
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

    public double getProbability(Variable var, ArrayList<Variable> evidence) {
        Node queryNode = null;
        for (Node n : nodes) {
            if (n.var.getName() == var.getName()) {
                queryNode = n;
                break;
            }
        }

        Variable[] importantEvidence = new Variable[queryNode.parentNames.length];
        for (int i = 0; i < importantEvidence.length; i++) {
            for (Variable v : evidence) {
                if (v.getName() == queryNode.parentNames[i]) {
                    importantEvidence[i] = v;
                    break;
                }
            }
        }

        int index = 0;
        for (Variable v : importantEvidence) {
            index <<= 1;
            if (v.getValue()) {
                index |= 1;
            }
        }

        if (var.getValue()) {
            return queryNode.probabilities[index];
        } else {
            return 1 - queryNode.probabilities[index];
        }
    }

    private ArrayList<Variable> topologicalOrder() {
        HashMap<Character, Integer> map = new HashMap<>();
        for (Node n : nodes) {
            map.putIfAbsent(n.var.getName(), 0);
            for (Node child : n.children) {
                map.putIfAbsent(child.var.getName(), 0);
                map.put(child.var.getName(), map.get(child.var.getName())+1);
            }
        }

        LinkedList<Node> queue = new LinkedList<>();
        for (Node n : nodes) {
            if (map.get(n.var.getName()) == 0){
                queue.add(n);
            }
        }

        ArrayList<Variable> vars = new ArrayList<>();
        while (!queue.isEmpty()) {
            Node curr = queue.remove();
            vars.add(new Variable(curr.var.getName()));
            for (Node child : curr.children) {
                int numParents = map.get(child.var.getName())-1;
                if (numParents == 0) {
                    queue.add(child);
                }
                map.put(child.var.getName(), numParents);
            }
        }

        return vars;
    }
}
