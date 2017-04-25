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

    public void sumOut(Variable toMerge) {
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

        Variable[] newVars = new Variable[vars.length-1];
        double[] newProbs = new double[probabilities.length >> 1];

        for (int i = 0; i < vars.length; i++) {
            if (i < index) {
                newVars[i] = vars[i];
            } else if (i > index) {
                newVars[i-1] = vars[i];
            }
        }

        for (int i = 0; i < probabilities.length; i++) {
            int pi = 0;
            int n = i;
            for (int e = vars.length-1; e >= 0; e--) {
                if (e == index) {
                    n >>= 1;
                    continue;
                }
                pi <<= 1;
                pi |= n%2;
                n >>= 1;
            }
            n = pi;
            pi = 0;
            for (int e = 0; e < vars.length-1; e++) {
                pi <<= 1;
                pi |= n%2;
                n >>= 1;
            }
            newProbs[pi] += probabilities[i];
        }

        vars = newVars;
        probabilities = newProbs;
    }

    public Factor pointwiseMultiply(Variable toMerge, Factor other) {
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

        TreeSet<Variable> merge = new TreeSet<>();
        for (Variable v : vars) {
            merge.add(v);
        }
        for (Variable v : other.vars) {
            merge.add(v);
        }
        Variable[] newVars = new Variable[merge.size()];
        double[] probs = new double[1 << merge.size()];
        newVars = merge.toArray(newVars);

        for (int i = 0; i < probabilities.length; i++) {
            for (int e = 0; e < other.probabilities.length; e++) {
                int n = i;
                for (int w = vars.length-1; w >= 0; w--) {
                    vars[w].setValue(n % 2 == 1);
                    n >>= 1;
                }
                n = e;
                for (int w = other.vars.length-1; w >= 0; w--) {
                    other.vars[w].setValue(n % 2 == 1);
                    n >>= 1;
                }
                if (matches(vars, other.vars)) {
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
                    probs[index] = probabilities[i] * other.probabilities[e];
                }
            }
        }

        return new Factor(newVars, probs);
    }

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

    public double getProbability(int index) {
        return probabilities[index];
    }

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
