import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
			ArrayList<Variable> evidence = new ArrayList<>(); //List containing the evidence of the query
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
					in.nextLine();
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
			
			new Driver().enumerationAsk(queryVar, evidence, bayesNet);
        } catch (FileNotFoundException e) {
            System.out.println("File not found.");
            System.exit(1);
        }
    }
}
