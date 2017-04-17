import java.util.ArrayList;

public class Driver {

    public void enumerationAsk(Variable queryVar, ArrayList<Variable> evidence, BayesNet bayesNet) {
        ArrayList<Variable> vars = bayesNet.getVars(evidence);
        evidence.add(queryVar);

        for (Variable v : vars) {
            if (v.getName() == queryVar.getName()) {
                v.setValue(false);
                break;
            }
        }
        queryVar.setValue(false);
        double falseProbability = enumerateAll(vars, evidence, bayesNet);

        for (Variable v : vars) {
            if (v.getName() == queryVar.getName()) {
                v.setValue(true);
                break;
            }
        }
        queryVar.setValue(true);
        double trueProbability = enumerateAll(vars, evidence, bayesNet);

//        evidence.remove(evidence.size()-1);
//        double evidenceProbability = enumerateAll(vars, evidence, bayesNet);

        System.out.println();
        System.out.println("RESULT:");

        queryVar.setValue(false);
        System.out.printf("P(%s | %s) = %.16f%n", queryVar, evidence.toString().replaceAll("[\\[\\]]", ""), falseProbability/(falseProbability+trueProbability));

        queryVar.setValue(true);
        System.out.printf("P(%s | %s) = %.16f%n", queryVar, evidence.toString().replaceAll("[\\[\\]]", ""), trueProbability/(falseProbability+trueProbability));
    }

    public double enumerateAll(ArrayList<Variable> vars, ArrayList<Variable> evidence, BayesNet bayesNet) {
        if (vars.isEmpty()) {
            return 1;
        }
        Variable first = vars.remove(0);
        double retVal = 0;
        if (first.isSet()) {
            double probability = bayesNet.getProbability(first, evidence);
            retVal = probability * enumerateAll(vars, evidence, bayesNet);
        } else {
            first.setValue(false);
            double probability = bayesNet.getProbability(first, evidence);
            evidence.add(first);
            retVal += probability * enumerateAll(vars, evidence, bayesNet);
            evidence.remove(evidence.size()-1);

            first.setValue(true);
            probability = bayesNet.getProbability(first, evidence);
            evidence.add(first);
            retVal += probability * enumerateAll(vars, evidence, bayesNet);
            evidence.remove(evidence.size()-1);

            first.unsetValue();
        }
        vars.add(0, first);
        System.out.printf("%s    %s    %.8f%n", vars, evidence, retVal);

        return retVal;
    }

    public static void main(String[] args) {
        Variable queryVar = new Variable('B');
        ArrayList<Variable> evidence = new ArrayList<>();
        evidence.add(new Variable('J', true));
        evidence.add(new Variable('M', true));
        BayesNet bayesNet = new BayesNet();
        bayesNet.add('B', 0.001);
        bayesNet.add('E', 0.002);
        bayesNet.add(new char[]{'B', 'E'}, 'A', new double[]{0.001, 0.29, 0.94, 0.95});
        bayesNet.add(new char[]{'A'}, 'J', new double[]{0.05, 0.9});
        bayesNet.add(new char[]{'A'}, 'M', new double[]{0.01, 0.7});

        new Driver().enumerationAsk(queryVar, evidence, bayesNet);
    }
}
