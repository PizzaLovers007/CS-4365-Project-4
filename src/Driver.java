import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
//        Variable queryVar = new Variable('A');
//        TreeSet<Variable> evidence = new TreeSet<>();
//        evidence.add(new Variable('D', true));
//        evidence.add(new Variable('E', true));
//        BayesNet bayesNet = new BayesNet();
//        bayesNet.add('A', 0.3);
//        bayesNet.add('B', 0.6);
//        bayesNet.add(new char[]{'A'}, 'C', new double[]{0.4, 0.8});
//        bayesNet.add(new char[]{'A', 'B'}, 'D', new double[]{0.2, 0.1, 0.8, 0.7});
//        bayesNet.add(new char[]{'C'}, 'E', new double[]{0.2, 0.7});

        BayesNet bayesNet = new BayesNet();

		//DEBUG remember to change 0 to 3
		if (args.length != 3) {
            System.out.println("Incorrect number of arguments.");
            System.exit(1);
        }
        try {
            Scanner in = new Scanner(new File(args[0]));
			String mechanism = args[1];
			String query = args[2];

			//DEBUG Remember to uncomment above
			/*Scanner in = new Scanner(new File("input.txt"));
			String mechanism = "elim";
			String query = "P(B|J=t,M=t)";*/
			
			//Parser for query
			Pattern queryvariable = Pattern.compile("P\\([A-Z]");
			Pattern queryevidence = Pattern.compile("[A-Z]\\=[t,f]");
			Matcher qv = queryvariable.matcher(query);
			Matcher qe = queryevidence.matcher(query);
			qv.find();
			Variable queryVar = new Variable(qv.group().charAt(2));
			TreeSet<Variable> evidence = new TreeSet<>();
			while(qe.find())
			{
				String temp = qe.group();
				if(temp.charAt(2)=='t')
				{
					evidence.add(new Variable(temp.charAt(0),true));
				}
				else
				{
					evidence.add(new Variable(temp.charAt(0),false));
				}
			}
			
			// Parser for input file
			Pattern single = Pattern.compile("P\\([A-Z]\\)");
			Pattern multi = Pattern.compile("([A-Z]\\s)+\\|\\s[A-Z]");
			Pattern truefalse = Pattern.compile("([t,f]\\s)+");
			Pattern probability = Pattern.compile("\\d*\\.\\d+");
			while(in.hasNextLine())
			{
				String givenLine = in.nextLine();
				Matcher s = single.matcher(givenLine);
				Matcher m = multi.matcher(givenLine);
				if(s.find())
				{
					Matcher p = probability.matcher(givenLine);
					p.find();
					bayesNet.add(s.group().charAt(2),Double.parseDouble(p.group()));
					in.nextLine();
				}
				else if(m.find())
				{
					String variables = m.group().replaceAll(" ","").replaceAll("\\|","");
					in.nextLine();
					int totalvariables = variables.length()-1;
					char[] leftVariables = variables.substring(0,totalvariables).toCharArray();
					char rightVariable = variables.charAt(totalvariables);
					int totalCombinations = (int)Math.pow(2,totalvariables);
					double[] probabilitiesOfNode = new double[totalCombinations];
					for(int x = 0; x < totalCombinations; x++)
					{
						givenLine = in.nextLine();
						Matcher p = probability.matcher(givenLine);
						Matcher tf = truefalse.matcher(givenLine);
						p.find();
						tf.find();
						probabilitiesOfNode[Integer.parseInt(tf.group().replaceAll("t ","1").replaceAll("f ","0"),2)] = Double.parseDouble(p.group());
					}
					System.out.printf("%s %s\n%s",Arrays.toString(leftVariables),rightVariable,Arrays.toString(probabilitiesOfNode)); //DEBUG
					bayesNet.add(leftVariables,rightVariable,probabilitiesOfNode);
				}
				
			}
			
			new Driver().enumerationAsk(queryVar, evidence, bayesNet);
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
            System.exit(1);
        }
    }
}
