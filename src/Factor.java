import java.util.TreeSet;

/**
 * Object to represent a factor.
 */
public class Factor {

    private Variable[] vars;  // Variables in the factor
    private double[] probabilities;  // Probabilities for the truth assignments

    /**
     * Creates a new factor.
     * @param vars variables in the factor
     * @param probs probabilities for the truth assignments
     */
    public Factor(Variable[] vars, double[] probs) {
        this.vars = vars;
        probabilities = probs;
    }

	/**
     * Sums the probabilities of a Factor which involve similar Variable states.
     * @param toMerge variable of relevant states
     */
    public void sumOut(Variable toMerge) {
        // Search for the toMerge variable in the factor
        // If it's not in the factor, then there is nothing to sum out
        int index = -1;
        for (int i = 0; i < vars.length; i++) {
            if (vars[i].getName() == toMerge.getName()) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            return;
        }

        Variable[] newVars = new Variable[vars.length-1]; // Array containing the Variables which are relevant
        double[] newProbs = new double[probabilities.length >> 1]; // Array containing the resulting probabilities

        // Copy the vars over minus the variable we are summing over
        for (int i = 0; i < vars.length; i++) {
            if (i < index) {
                newVars[i] = vars[i];
            } else if (i > index) {
                newVars[i-1] = vars[i];
            }
        }

        // Generate the new probabilities
        for (int i = 0; i < probabilities.length; i++) {
            int pi = 0; // new probability index
            int n = i;

            // Create the new probability index (reversed)
            for (int e = vars.length-1; e >= 0; e--) {
                if (e == index) {
                    n >>= 1;
                    continue;
                }
                pi <<= 1;
                pi |= n%2;
                n >>= 1;
            }

            // Unreverse the new probability index
            n = pi;
            pi = 0;
            for (int e = 0; e < vars.length-1; e++) {
                pi <<= 1;
                pi |= n%2;
                n >>= 1;
            }

            // Add one of the old probabilities to the new probability
            newProbs[pi] += probabilities[i];
        }

        // Apply variable removal
        vars = newVars;
        probabilities = newProbs;
    }

	/**
     * Multiplies two Factors against each other in terms of Variable states.
     * @param toMerge Variable of relevant states
     * @param other Factor to be multiplied against
	 * @return Factor resulting factor from multiplication
     */
    public Factor pointwiseMultiply(Variable toMerge, Factor other) {
        // Search for the toMerge in both factors (this and other)
        // If it is not in either factor, cannot pointwise multiply
        boolean found = false;
        for (Variable v : vars) {
            if (v.equals(toMerge)) {
                found = true;
                break;
            }
        }
        if (!found) {
            return null;
        }
        found = false;
        for (Variable v : other.vars) {
            if (v.equals(toMerge)) {
                found = true;
                break;
            }
        }
        if (!found) {
            return null;
        }

        // Find the variables of the new factor that will be generated
        TreeSet<Variable> merge = new TreeSet<>();
        for (Variable v : vars) {
            merge.add(v);
        }
        for (Variable v : other.vars) {
            merge.add(v);
        }

        Variable[] newVars = new Variable[merge.size()];  // New variables
        double[] probs = new double[1 << merge.size()];  // New probabilities
        newVars = merge.toArray(newVars);

        // Generate the new probabilities
        for (int i = 0; i < probabilities.length; i++) {
            for (int e = 0; e < other.probabilities.length; e++) {
                // Set this factor's variables
                int n = i;
                for (int w = vars.length-1; w >= 0; w--) {
                    vars[w].setValue(n % 2 == 1);
                    n >>= 1;
                }

                // Set other factor's variables
                n = e;
                for (int w = other.vars.length-1; w >= 0; w--) {
                    other.vars[w].setValue(n % 2 == 1);
                    n >>= 1;
                }

                // Generate probability if common variables match their values
                if (matches(vars, other.vars)) {
                    // Get the index of the probability
                    int index = 0;
                    for (Variable v1 : merge) {
                        index <<= 1;
                        for (Variable v2 : vars) {
                            if (v1.equals(v2)) {
                                v1.setValue(v2.getValue());
                                break;
                            }
                        }
                        for (Variable v2 : other.vars) {
                            if (v1.equals(v2)) {
                                v1.setValue(v2.getValue());
                                break;
                            }
                        }
                        index |= v1.getValue() ? 1 : 0;
                    }

                    // Create the probability
                    probs[index] = probabilities[i] * other.probabilities[e];
                }
            }
        }

        // Return the generated factor
        return new Factor(newVars, probs);
    }

	/**
     * Checks if values of shared Variables are the same.
     * @param one first set of variables
     * @param two second set of variables
	 * @return true if they are the same
     */
    private boolean matches(Variable[] one, Variable[] two) {
        for (Variable v1 : one) {
            boolean match = true;
            for (Variable v2 : two) {
                if (v1.equals(v2) && v1.getValue() != v2.getValue()) {
                    match = false;
                    break;
                }
            }
            if (!match) {
                return false;
            }
        }
        return true;
    }

	/**
     * Used to retrieve the probabilities.
     * @param index place the probability is in the list
     * @return double of the probability
     */
    public double getProbability(int index) {
        return probabilities[index];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < probabilities.length; i++) {
            int n = i;
            for (int e = vars.length-1; e >= 0; e--) {
                vars[e].setValue(n%2 == 1);
                n >>= 1;
            }
            for (int e = 0; e < vars.length; e++) {
                if (e != 0) {
                    sb.append(" ");
                }
                sb.append(String.format("%c=%c", vars[e].getName(), vars[e].getValue() ? 't' : 'f'));
            }
            sb.append(":  ").append(probabilities[i]).append("\n");
        }
        return sb.toString();
    }
}
