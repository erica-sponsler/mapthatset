package mapthatset.g7;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import mapthatset.sim.Guesser;
import mapthatset.sim.GuesserAction;

public class G7Guesser extends Guesser {

	public String getID()
	{
		return "G7: Guesser";
	}

	/* Number of variables */
	private int variable_count;

	/* Number of distinct values */
	private int value_count;

	/* Last action was a guess */
	private boolean guess;

	/* Last query issued */
	private ArrayList <Integer> query;

	/* Random generator */
	private static final Random random = new Random();

	/* History of queries and answers */
	private Vector <Pair <ArrayList <Integer>, ArrayList <Integer>>> history;

	/* Engine that solves the problem
	 * as a constraint satisfaction problem
	 * Will use this to get domains of variables
	 * and find the set of all solutions when
	 * possible and refine them
	 */
	private Combinator csp_engine;

	/* Dependency engine
	 * Will identify functional dependencies
	 * and remove variables not used
	 */
	private Dependency dep_engine;

	/* Active variables */
	private HashSet <Integer> active;

	/* Unused variables in the last round */
	private LinkedList <Integer> unused;

	/* Variables have been combined in a query
	 * First query does not count
	 */
	private HashSet <Pair <Integer, Integer>> combined;

	/* All solutions */
	private int[][] solutions;

	/* Dividing of variables */
	private int[] dividing;

	/* Phase in dividing */
	private int dividing_pos;

	public void startNewMapping(int len)
	{
		variable_count = len;
		value_count = 0;
		solutions = null;
		if (variable_count == 1) {
			int[][] one = {{1}};
			solutions = one;
		}
		csp_engine = new Combinator(len);
		dep_engine = new Dependency(csp_engine);
		active = new HashSet <Integer> ();
		unused = new LinkedList <Integer> ();
		for (int i = 1 ; i <= variable_count ; ++i) {
			active.add(i);
			unused.add(i);
		}
		combined = new HashSet <Pair <Integer, Integer>> ();
		history = new Vector <Pair <ArrayList <Integer>, ArrayList <Integer>>> ();
		query = new ArrayList <Integer> ();
		dividing = new int [1];
		dividing[0] = variable_count;
		dividing_pos = 0;
	}

	public GuesserAction nextAction()
	{
		if (guess)
			return new GuesserAction("g", query);
		query.clear();
		int size = dividing[dividing_pos++];
		while (query.size() != size) {
			int var = unused.removeFirst();
			boolean conflict = false;
			for (int svar : query)
				if (combined.contains(new Pair <Integer, Integer> (var, svar))) {
					conflict = true;
					break;
				}
			if (conflict)
				unused.addLast(var);
			else
				query.add(var);
		}
		return new GuesserAction("q", query);
	}

	public void setResult(ArrayList <Integer> result)
	{
		/* Ignore guess results */
		if (guess) {
			guess = false;
			return;
		}
		/* Update history */
		history.add(new Pair <ArrayList <Integer>, ArrayList <Integer>> (query, result));
		/* Add constraint to the engine */
		csp_engine.constraint(toArray(query), toArray(result));
		/* Check bypassing of last part query */
		if (dividing_pos + 1 == dividing.length && dividing_pos != 0) {
			/* All variables of previous groups */
			HashSet <Integer> restVars = new HashSet <Integer> ();
			int h = history.size() - 1 - dividing_pos;
			for (; h != history.size() ; ++h)
				restVars.addAll(history.get(h).fst);
			/* Get values of first groups */
			HashSet <Integer> restVals = allDomains(restVars);
			/* Get values of last group */
			HashSet <Integer> remVals = allDomains(unused);
			/* All values */
			HashSet <Integer> allVals = addSets(restVals, remVals);
			/* If unused values are equal to the number of remaining */
			if (allVals.size() - restVals.size() == unused.size()) {
				/* Constraint remaining variables
				 * to take remaining unused values
				 */
				csp_engine.constraint(toArray(unused), toArray(remVals));
				dividing_pos++;
			}
		}
		/* Check if first result and update value count */
		if (value_count == 0)
			value_count = result.size();
		else
			/* Check combinations */
			for (int var_i : query)
				for (int var_j : query)
					if (var_i != var_j) {
						combined.add(new Pair<Integer, Integer> (var_i, var_j));
						combined.add(new Pair<Integer, Integer> (var_j, var_i));
					}
		/* Try to find all solutions */
		csp_engine.timeoutLimit(100);
		/* If not found */
		if (solutions == null)
			/* Start from scratch */
			solutions = csp_engine.findall();
		else
			/* Filter already known solutions */
			solutions = csp_engine.filter(solutions);
		/* Check if problem can be solved */
		int[] solution = dep_engine.solve();
		/* Set unique solution for next guess */
		if (solution != null) {
			solutions = new int [1][];
			solutions[0] = solution;
			query.clear();
			for (int i = 0 ; i != solutions[0].length ; ++i)
				query.add(solutions[0][i]);
			guess = true;
			return;
		}
		/* Remove unary domain variables from the pool */
		boolean last;
		do {
			last = true;
			for (int var : active)
				if (csp_engine.domain(var).length == 1) {
					last = false;
					active.remove(var);
					break;
				}
		} while (!last);
		/* Remove dependencies from the pool */
		//TODO remove dependencies
		/* Check if end of round and restart */
		if (dividing_pos == dividing.length) {
			HashSet <Integer> active_vals = allDomains(active);
			int groups = 2;
			//TODO figure out groups based on active variables
			//     and active values
			dividing = divide(active.size(), groups);
			dividing_pos = 0;
			unused.clear();
			/* Reset the unused variables */
			for (int var : active)
				unused.add(var);
		}
	}

	private HashSet <Integer> allDomains(Collection <Integer> vars)
	{
		HashSet <Integer> vals = new HashSet <Integer> ();
		for (int var : vars) {
			int[] domain = csp_engine.domain(var);
			for (int i = 0 ; i != domain.length ; ++i)
				vars.add(domain[i]);
		}
		return vals;
	}

	private static int nearestInt(double n)
	{
		int i = (int) n;
		if (n > i + 0.5)
			i++;
		return i;
	}

	private int[] divide(int all, int parts)
	{
		int[] res = new int [parts];
		int i = 0;
		while (parts != 0) {
			res[i] = nearestInt(all / (double) parts);
			all -= res[i++];
			parts--;
		}
		return res;
	}

	private HashSet <Integer> addSets(HashSet <Integer> a, HashSet <Integer> b)
	{
		HashSet <Integer> r = new HashSet <Integer> (a);
		r.addAll(b);
		return r;
	}

	private static int[] toArray(Collection <Integer> al)
	{
		int[] arr = new int [al.size()];
		int i = 0;
		for (int n : al)
			arr[i++] = n;
		return arr;
	}

	private static String toString(Collection <?> a)
	{
		if (a.size() == 0)
			return "";
		boolean first = true;
		StringBuffer buf = new StringBuffer();
		for (Object o : a) {
			if (!first)
				buf.append(',');
			else
				first = false;
			buf.append(o.toString());
		}
		return buf.toString();
	}
}
