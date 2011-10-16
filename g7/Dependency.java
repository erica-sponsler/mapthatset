package mapthatset.g7;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Dependency {

	/* Solutions */
	private int[][] solutions;

	/* CSP engine */
	private Combinator csp;

	/* Values that exhaust domain */
	private HashMap <Integer, Pair <int[], HashSet <Integer>>> exhaust;

	/* Functional dependency */
	private HashMap <Integer, Pair <int[], HashMap <ArrayList <Integer>, Integer>>> dependency;

	/* Initialize dependency class */
	public Dependency(Combinator csp)
	{
		solutions = null;
		this.csp = csp;
		exhaust = new HashMap <Integer, Pair <int[], HashSet <Integer>>> ();
		dependency = new HashMap <Integer, Pair <int[], HashMap <ArrayList <Integer>, Integer>>> ();
	}

	/* Update solutions */
	public void update(int[][] solutions)
	{
		this.solutions = solutions;
	}

	/* Check if first variable is dependent on a
	 * list of variables given as rest arguments
	 */
	public boolean check(int to, int ... from)
	{
		if (solutions == null)
			return exhaust(to, from);
		if (exhaust.containsKey(to) || dependency.containsKey(to))
			return true;
		Arrays.sort(from);
		HashMap <ArrayList <Integer>, Integer> value_map = new HashMap <ArrayList <Integer>, Integer> ();
		for (int[] solution : solutions) {
			ArrayList <Integer> vector = new ArrayList <Integer> ();
			for (int i = 0 ; i != from.length ; ++i)
				vector.add(solution[from[i] - 1]);
			int to_value = solution[to - 1];
			Integer target_value = value_map.get(vector);
			if (target_value != null && target_value != to_value)
				return false;
			value_map.put(vector, target_value);
		}
		dependency.put(to, new Pair <int[], HashMap <ArrayList <Integer>, Integer>> (from, value_map));
		return true;
	}

	/* Check if values are a domain exhaustion */
	private boolean exhaust(int to, int[] from)
	{
		if (exhaust.containsKey(to))
			return true;
		HashSet <Integer> values = new HashSet <Integer> ();
		int[] domain = csp.domain(to);
		for (int i = 0 ; i != domain.length ; ++i)
			values.add(domain[i]);
		for (int v = 0 ; v != from.length ; ++v) {
			domain = csp.domain(from[v]);
			for (int i = 0 ; i != domain.length ; ++i)
				values.add(domain[i]);
		}
		if (values.size() != from.length + 1)
			return false;
		exhaust.put(to, new Pair <int[], HashSet <Integer>> (Arrays.copyOf(from, from.length), values));
		return true;
	}

	/* Extending to create array of hash sets */
	private static class IntSet extends HashSet <Integer> {
		private static final long serialVersionUID = 1L;
	}

	/* Resolve all dependencies and return
	 * final solution with all variables set
	 * If some variable is ambiguous return null
	 */
	public int[] solve()
	{
		if (solutions == null)
			return null;
		int variable_count = solutions[0].length;
		IntSet [] real_domain = new IntSet [variable_count];
		for (int[] solution : solutions)
			for (int val_i = 0 ; val_i != variable_count ; ++val_i)
				real_domain[val_i].add(solution[val_i]);
		int[] solution = new int [variable_count];
		for (int i = 1 ; i <= variable_count ; ++i) {
			if (dependency.containsKey(i))
				continue;
			if (real_domain[i-1].size() != 1)
				return null;
			solution[i-1] = real_domain[i-1].iterator().next();
		}
		for (int to : dependency.keySet()) {
			int[] vars = dependency.get(to).fst;
			ArrayList <Integer> values = new ArrayList <Integer> ();
			for (int i = 0 ; i != vars.length ; ++i)
				values.add(solution[vars[i]-1]);
			solution[to-1] = dependency.get(to).snd.get(values);
		}
		for (int to : exhaust.keySet()) {
			int[] vars = exhaust.get(to).fst;
			HashSet <Integer> vals = exhaust.get(to).snd;
			for (int i = 0 ; i != vars.length ; ++i)
				vals.remove(solution[vars[i]-1]);
			solution[to-1] = vals.iterator().next();
		}
		return solution;
	}
}
