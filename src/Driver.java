import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Driver {

    /**
     * Query to enumerate over the Bayes Net to find the probabilities of {@code queryVar}
     * given {@code evidence}.
     * @param queryVar variable to query
     * @param evidence given variables in the query
     * @param bayesNet Bayes Net for the query
     */
    public void enumerationAsk(Variable queryVar, TreeSet<Variable> evidence, BayesNet bayesNet) {
        // Get the variable ordering from the Bayes Net
        ArrayList<Variable> vars = bayesNet.getEnumVars(evidence);

        // Add query variable to the evidence
        evidence.add(queryVar);

        // Set the query variable to false
        for (Variable v : vars) {
            if (v.getName() == queryVar.getName()) {
                v.setValue(false);
                break;
            }
        }
        queryVar.setValue(false);

        // Get the probability that the query variable is false
        double falseProbability = enumerateAll(vars, evidence, bayesNet);

        // Set the query variable to true
        for (Variable v : vars) {
            if (v.getName() == queryVar.getName()) {
                v.setValue(true);
                break;
            }
        }
        queryVar.setValue(true);

        // Get the probability that the query variable is true
        double trueProbability = enumerateAll(vars, evidence, bayesNet);

        // Remove the query variable from the evidence
        evidence.remove(queryVar);

        // Print "RESULT" header
        System.out.println();
        System.out.println("RESULT:");

        // Print false probability
        queryVar.setValue(false);
        System.out.printf("P(%s%s) = %.16f%n",
                queryVar,
                String.format("%s%s", evidence.isEmpty() ? "" : " | ", evidence.toString().replaceAll("[\\[\\]]", "")),
                falseProbability/(falseProbability+trueProbability));

        // Print true probability
        queryVar.setValue(true);
        System.out.printf("P(%s%s) = %.16f%n",
                queryVar,
                String.format("%s%s", evidence.isEmpty() ? "" : " | ", evidence.toString().replaceAll("[\\[\\]]", "")),
                trueProbability/(falseProbability+trueProbability));
    }

    /**
     * Recursive call to calculate the probabilities of all the variables in {@code vars}.
     * @param vars variables to calculate probabilities of
     * @param evidence given variables in the query
     * @param bayesNet Bayes Net for the query
     * @return combined probability of all the variables in {@code vars}
     */
    public double enumerateAll(ArrayList<Variable> vars, TreeSet<Variable> evidence, BayesNet bayesNet) {
        // Base case
        if (vars.isEmpty()) {
            return 1;
        }

        // Get the next variable to process
        Variable first = vars.remove(0);
        double retVal = 0;

        if (first.isSet()) {
            // Variable is set so only get the probability for its value
            double probability = bayesNet.getProbability(first, evidence);
            retVal = probability * enumerateAll(vars, evidence, bayesNet);
        } else {
            // Variable is not set so get the total probability

            // Calculate false first
            first.setValue(false);
            double probability = bayesNet.getProbability(first, evidence);
            evidence.add(first);
            retVal += probability * enumerateAll(vars, evidence, bayesNet);
            evidence.remove(first);

            // Then true
            first.setValue(true);
            probability = bayesNet.getProbability(first, evidence);
            evidence.add(first);
            retVal += probability * enumerateAll(vars, evidence, bayesNet);
            evidence.remove(first);

            // Unset value to undo recursive steps
            first.unsetValue();
        }
        // Put variable back in the list to undo recursive steps
        vars.add(0, first);

        // Build the output line
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

        // Return the combined probability
        return retVal;
    }

    /**
     * Query to run elimination over the Bayes Net to find the probabilities of {@code queryVar}
     * given {@code evidence}.
     * @param queryVar variable to query
     * @param evidence given variables in the query
     * @param bayesNet Bayes Net for the query
     */
    public void eliminationAsk(Variable queryVar, TreeSet<Variable> evidence, BayesNet bayesNet) {
        // No factors at the very start
        ArrayList<Factor> factors = new ArrayList<>();

        // Get the variables in the Bayes Net in the following order:
        // Children before parents -> smallest factor -> alphabetical
        ArrayList<Variable> vars = bayesNet.getElimVars(evidence);

        // Loop through every variable
        for (Variable currVar : vars) {
            // Print variable header
            System.out.printf("----- Variable: %c -----%n", currVar.getName());

            // Create factor for current variable
            factors.add(bayesNet.makeFactor(currVar, evidence));

            // Sum out if current variable is a hidden variable
            if (!queryVar.equals(currVar) && !evidence.contains(currVar)) {
                sumOut(currVar, factors);
            }

            // Print current factors
            System.out.println("Factors:");
            for (Factor f : factors) {
                System.out.println(f);
            }
        }

        // Do final pointwise multiplication to make one factor
        pointwiseMultiply(queryVar, factors);

        // Get true and false probabilities (not normalized)
        double trueProbability = factors.get(0).getProbability(1);
        double falseProbability = factors.get(0).getProbability(0);

        // Print result header
        System.out.println("RESULT:");

        // Print false probability
        queryVar.setValue(false);
        System.out.printf("P(%s%s) = %.16f%n",
                queryVar,
                String.format("%s%s", evidence.isEmpty() ? "" : " | ", evidence.toString().replaceAll("[\\[\\]]", "")),
                falseProbability/(falseProbability+trueProbability));

        // Print true probability
        queryVar.setValue(true);
        System.out.printf("P(%s%s) = %.16f%n",
                queryVar,
                String.format("%s%s", evidence.isEmpty() ? "" : " | ", evidence.toString().replaceAll("[\\[\\]]", "")),
                trueProbability/(falseProbability+trueProbability));
    }

    /**
     * Sums out all the factors that have {@code currVar}.
     * @param currVar the variable to sum out
     * @param factors the factors that need to be summed out
     */
    public void sumOut(Variable currVar, ArrayList<Factor> factors) {
        // First pointwise multiply to combine common factors
        pointwiseMultiply(currVar, factors);

        // Then sum out all the factors individually
        for (Factor f : factors) {
            f.sumOut(currVar);
        }
    }

    /**
     * Pointwise multiplies all pairs of factors over {@code currVar}.
     * @param currVar the variable to pointwise multiply over
     * @param factors the factors that need to be pointwise multiplied
     */
    public void pointwiseMultiply(Variable currVar, ArrayList<Factor> factors) {
        for (int i = 0; i < factors.size(); i++) {
            for (int e = i+1; e < factors.size(); e++) {
                // Try to multiply
                Factor mult = factors.get(i).pointwiseMultiply(currVar, factors.get(e));

                // If successful multiply, replace old factors with new factor
                if (mult != null) {
                    factors.set(i, mult);
                    factors.remove(e);
                    e--;
                }
            }
        }
    }

	/**
     * Parses args from the command line and the input file.
     * @param args command line arguments
     */
    public static void main(String[] args) {
        BayesNet bayesNet = new BayesNet();

		if (args.length != 3) {
            System.out.println("Incorrect number of arguments.");
            System.exit(1);
        }
        try {
            Scanner in = new Scanner(new File(args[0]));
			String mechanism = args[1];
			String query = args[2];
			
			//Parser for query
			Pattern queryvariable = Pattern.compile("P\\([A-Z]"); //Matches to find the query variable
			Pattern queryevidence = Pattern.compile("[A-Z]=[t,f]"); //Matches to find the evidence variables and their values
			Matcher qv = queryvariable.matcher(query);
			Matcher qe = queryevidence.matcher(query);
			qv.find();
			Variable queryVar = new Variable(qv.group().charAt(2)); //Variable to be queried
			TreeSet<Variable> evidence = new TreeSet<>(); //List containing the evidence of the query
			while(qe.find())
			{
				String temp = qe.group();
				if(temp.charAt(2)=='t') //True if evidence is true and to the evidence
				{
					evidence.add(new Variable(temp.charAt(0),true));
				}
				else
				{
					evidence.add(new Variable(temp.charAt(0),false));
				}
			}
			
			// Parser for input file
			Pattern single = Pattern.compile("P\\([A-Z]\\)"); //Matches to find the variable of a node with no parents
			Pattern multi = Pattern.compile("([A-Z]\\s)+\\|\\s[A-Z]"); //Matches to find the variables of a node with parents
			Pattern truefalse = Pattern.compile("([t,f]\\s)+"); //Matches to determine the values of the ancestor nodes for a probability
			Pattern probability = Pattern.compile("\\d*\\.\\d+"); //Matches the probability
			while(in.hasNextLine()) //Runs until the end of the file
			{
				String givenLine = in.nextLine();
				Matcher s = single.matcher(givenLine);
				Matcher m = multi.matcher(givenLine);
				if(s.find()) //True if the line is for a node with no parents
				{
					Matcher p = probability.matcher(givenLine);
					p.find();
					bayesNet.add(s.group().charAt(2),Double.parseDouble(p.group())); //Adds the variable to the bayes net with its probability
				}
				else if(m.find()) //True if the line is for a node with parents
				{
					String variables = m.group().replaceAll(" ","").replaceAll("\\|","");
					in.nextLine();
					int totalvariables = variables.length()-1; //Determines the total variables of the table (parents plus current node)
					char[] leftVariables = variables.substring(0,totalvariables).toCharArray(); //Gets the variables which are the parents of the node
					char rightVariable = variables.charAt(totalvariables); //Gets the variable of the node
					int totalCombinations = (int)Math.pow(2,totalvariables); //Determines the number of combinations that are needed (2^parents)
					double[] probabilitiesOfNode = new double[totalCombinations]; //Array containing the probabilities of a node
					for(int x = 0; x < totalCombinations; x++) //Retrieves and sets all of the probabilities of a given node
					{
						givenLine = in.nextLine();
						Matcher p = probability.matcher(givenLine);
						Matcher tf = truefalse.matcher(givenLine);
						p.find();
						tf.find();
						/*
						 * The probability is placed into the array using a binary value which is calculated from the true and false values
						 * For example, a node with two parents will have a probability where both of those parents are true
						 * which maps to the binary value 11. That integer value is 3 so the probability gets put into the array's index of 3.
						 */
						probabilitiesOfNode[Integer.parseInt(tf.group().replaceAll("t ","1").replaceAll("f ","0"),2)] = Double.parseDouble(p.group());
					}
					bayesNet.add(leftVariables,rightVariable,probabilitiesOfNode); //Adds the variable to the bayes net with its probabilities and corresponding parent values
				}

			}

			if (mechanism.equals("enum")) {
                new Driver().enumerationAsk(queryVar, evidence, bayesNet);
            } else if (mechanism.equals("elim")) {
			    new Driver().eliminationAsk(queryVar, evidence, bayesNet);
            } else {
                System.out.println("Invalid mechanism, should be either \"enum\" or \"elim\"");
            }
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
            System.exit(1);
        }
    }
}
