import java.util.ArrayList;
import java.util.Collections;

public class BayesNet {

    private class Node implements Comparable<Node> {
        Variable var;
        double[] probabilities;
        char[] parentNames;
        ArrayList<Node> children;

        public Node(char varName, char[] parentNames, double[] probs) {
            var = new Variable(varName);
            probabilities = probs;
        }

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
                node.children.add(child);
            }
        }
    }
}
