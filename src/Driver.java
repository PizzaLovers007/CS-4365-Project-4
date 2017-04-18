import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

public class Driver {

    public void enumerationAsk(Variable queryVar, TreeSet<Variable> evidence, BayesNet bayesNet) {
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

        evidence.remove(queryVar);

        System.out.println();
        System.out.println("RESULT:");

        queryVar.setValue(false);
        System.out.printf("P(%s | %s) = %.16f%n", queryVar, evidence.toString().replaceAll("[\\[\\]]", ""), falseProbability/(falseProbability+trueProbability));

        queryVar.setValue(true);
        System.out.printf("P(%s | %s) = %.16f%n", queryVar, evidence.toString().replaceAll("[\\[\\]]", ""), trueProbability/(falseProbability+trueProbability));
    }

    public double enumerateAll(ArrayList<Variable> vars, TreeSet<Variable> evidence, BayesNet bayesNet) {
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
            evidence.remove(first);

            first.setValue(true);
            probability = bayesNet.getProbability(first, evidence);
            evidence.add(first);
            retVal += probability * enumerateAll(vars, evidence, bayesNet);
            evidence.remove(first);

            first.unsetValue();
        }
        vars.add(0, first);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bayesNet.size(); i++) {
            if (i < vars.size()) {
                sb.append(vars.get(i).getName()).append(' ');
            } else {
                sb.append("  ");
            }
        }
        sb.append("      | ");
        Iterator<Variable> it = evidence.iterator();
        for (int i = 0; i < bayesNet.size(); i++) {
            if (it.hasNext()) {
                Variable v = it.next();
                sb.append(v.getName()).append('=').append(v.getValue() ? 't' : 'f').append(' ');
            } else {
                sb.append("    ");
            }
        }
        sb.append("      = ").append(String.format("%.8f", retVal));
        System.out.println(sb);

        return retVal;
    }

    public static void main(String[] args) {
        //alarm.bn
//        Variable queryVar = new Variable('E');
//        TreeSet<Variable> evidence = new TreeSet<>();
//        evidence.add(new Variable('J', true));
//        evidence.add(new Variable('M', true));
//        BayesNet bayesNet = new BayesNet();
//        bayesNet.add('B', 0.001);
//        bayesNet.add('E', 0.002);
//        bayesNet.add(new char[]{'B', 'E'}, 'A', new double[]{0.001, 0.29, 0.94, 0.95});
//        bayesNet.add(new char[]{'A'}, 'J', new double[]{0.05, 0.9});
//        bayesNet.add(new char[]{'A'}, 'M', new double[]{0.01, 0.7});

        //Some random test
//        Variable queryVar = new Variable('A');
//        TreeSet<Variable> evidence = new TreeSet<>();
//        evidence.add(new Variable('B', true));
//        BayesNet bayesNet = new BayesNet();
//        bayesNet.add('A', 0.001);
//        bayesNet.add(new char[]{'A'}, 'B', new double[]{0.05, 0.9});

        //ex2.bn
        Variable queryVar = new Variable('A');
        TreeSet<Variable> evidence = new TreeSet<>();
        evidence.add(new Variable('D', true));
        evidence.add(new Variable('E', true));
        BayesNet bayesNet = new BayesNet();
        bayesNet.add('A', 0.3);
        bayesNet.add('B', 0.6);
        bayesNet.add(new char[]{'A'}, 'C', new double[]{0.4, 0.8});
        bayesNet.add(new char[]{'A', 'B'}, 'D', new double[]{0.2, 0.1, 0.8, 0.7});
        bayesNet.add(new char[]{'C'}, 'E', new double[]{0.2, 0.7});

        new Driver().enumerationAsk(queryVar, evidence, bayesNet);
    }
}
