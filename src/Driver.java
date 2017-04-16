import java.util.ArrayList;

public class Driver {

    public void enumerationAsk(Variable queryVar, ArrayList<Variable> evidence, BayesNet bayesNet) {
        queryVar.setValue(false);
        evidence.add(queryVar);
        double falseProbability = enumerateAll();
    }

    public double enumerateAll() {
        return 0;
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
