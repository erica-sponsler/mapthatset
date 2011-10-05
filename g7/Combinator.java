package mapthatset.g7;

import java.util.*;

class Combinator implements Iterable <int[]> {

	/* Number of variables */
	private int variable_count;

	/* Minimum value */
	private int min_value;

	/* Maximum value */
	private int max_value;

	/* Domains per variable */
	private int[][] gen_domain;

	/* Size of active domain per variable */
	private int[] gen_domain_size;

	private class Constraint {

		/* Different variables */
		public Set <Integer> variables;

		/* Different values on constraint */
		public Set <Integer> values;

		/* Initialize constraint between variables */
		public Constraint(int[] vars, int[] vals)
		{
			variables = new HashSet <Integer> ();
			for (int i = 0 ; i != vars.length ; ++i) {
				if (vars[i] <= 0 || vars[i] > variable_count)
					throw new IllegalArgumentException();
				variables.add(vars[i] - 1);
			}
			values = new HashSet <Integer> ();
			for (int i = 0 ; i != vals.length ; ++i) {
				if (vals[i] < min_value || vals[i] > max_value)
					throw new IllegalArgumentException();
				values.add(vals[i]);
			}
			if (values.size() > variables.size())
				throw new IllegalArgumentException();
		}
	}

	private static class IntArray extends Vector <Integer> {};

	private static class ConstraintArray extends Vector <Constraint> {};

	private static class IntSet extends HashSet <Integer> {};

	/* Constraints for the problem */
	private ConstraintArray constraints;

	/* Constraints for each variable */
	private IntArray[] attach;

	public Combinator(int vars)
	{
		this(vars, 1, vars);
	}

	public Combinator(int vars, int min, int max)
	{
		if (vars <= 0 || min > max)
			throw new IllegalArgumentException();

		variable_count = vars;
		min_value = min;
		max_value = max;

		constraints = new ConstraintArray();
		attach = new IntArray [vars];
		for (int i = 0 ; i != vars ; ++i)
			attach[i] = new IntArray();

		int value_count = max_value - min_value + 1;
		gen_domain = new int [variable_count][value_count];
		gen_domain_size = new int [variable_count];
		for (int i = 0 ; i != variable_count ; ++i) {
			gen_domain_size[i] = value_count;
			for (int j = 0 ; j != value_count ; ++j)
				gen_domain[i][j] = j + min_value;
		}
	}

	/* Add a new constraint in the combinator */
	public void constraint(int[] vars, int[] vals)
	{
		/* Create the new constraint and add it in the list */
		Constraint new_constraint = new Constraint(vars, vals);
		int constraint_position = constraints.size();
		constraints.add(new_constraint);

		/* Attach the constraint to affected variables */
		for (int var_i : new_constraint.variables) {
			attach[var_i].add(constraint_position);

			/* Cut the domains of variables to match the new constraint */
			for (int val_i = 0 ; val_i != gen_domain_size[var_i] ; ++val_i)
				if (!new_constraint.values.contains(gen_domain[var_i][val_i]))
					gen_domain[var_i][val_i--] = gen_domain[var_i][--gen_domain_size[var_i]];
		}

		/* Find out cases where there the same number
		 * of free variables and unused values
		 * So each one of the free variables can
		 * only pick from those values
		 */
		boolean changes;
		do {
			changes = false;
			for (Constraint con : constraints) {
				Map <Integer, Integer> unassigned_variables = new HashMap <Integer, Integer> ();
				Set <Integer> used_values = new HashSet <Integer> ();
				for (int var_i : con.variables)
					if (gen_domain_size[var_i] != 1)
						unassigned_variables.put(var_i, gen_domain_size[var_i]);
					else
						used_values.add(gen_domain[var_i][0]);
				Set <Integer> unused_values = new HashSet <Integer> ();
				for (int value : con.values)
					if (!used_values.contains(value))
						unused_values.add(value);
				if (unassigned_variables.size() == unused_values.size())
					for (int var_i : unassigned_variables.keySet())
						for (int val_i = 0 ; val_i != gen_domain_size[var_i] ; ++val_i)
							if (!unused_values.contains(gen_domain[var_i][val_i])) {
								gen_domain[var_i][val_i--] = gen_domain[var_i][--gen_domain_size[var_i]];
								changes = true;
							}
			}
		} while (changes);
	}

	/* Size of one variable's domain
	 * This is an approximate estimate
	 */
	public int[] domain(int var_i)
	{
		if (var_i <= 0 || var_i > variable_count)
			throw new IllegalArgumentException();
		return copy(gen_domain[var_i - 1], gen_domain_size[var_i - 1]);
	}

	/* The actual values found in each variable
	 * after enumerating all solutions
	 */
	public class Values {

		/* Stores the used values */
		private IntSet [] values;

		/* Private constructor
		 * You cannot create this class independently
		 */
		private Values()
		{
			values = new IntSet [variable_count];
			for (int i = 0 ; i != variable_count ; ++i)
				values[i] = new IntSet();
		}

		/* Add a new used value */
		private void add(int i, int value)
		{
			values[i].add(value);
		}

		/* Ask for the values of a specific variable */
		public int[] values(int var_i) {
			if (var_i <= 0 || var_i > variable_count)
				throw new IllegalArgumentException();
			IntSet set = values[var_i - 1];
			int[] result = new int [set.size()];
			int i = 0;
			for (Integer num : set)
				result[i++] = num;
			Arrays.sort(result);
			return result;
		}
	}

	/* Get all values used after running the iterator */
	public Values values()
	{
		Values values = new Values();
		for (int[] solution : this)
			for (int i = 0 ; i != variable_count ; ++i)
				values.add(i, solution[i]);
		return values;
	}

	/* Iterator over valid combinations */
	public Iterator <int[]> iterator()
	{
		/* Anonymous class for iterator */
		return new Iterator <int[]> () {

	/* Indicates if we have finished the search for combinations
	 * Also used as a trick to call a "constructor" function
	 */
	boolean finished = init();

	/* "Constructor" for the anonymous iterator */
	private boolean init()
	{
		/* Initialize variables and order */
		variable = new int [variable_count];
		order = new int [variable_count];
		for (int i = 0 ; i != variable_count ; ++i)
			order[i] = i;
		variables_fixed = 0;
		next_found = false;
		finished = false;

		/* Initialize domains and copy them */
		int value_count = max_value - min_value + 1;
		constraint_count = constraints.size();
		domain = new int [variable_count][value_count];
		domain_size = new int [variable_count];
		domain_offset = new int [variable_count];
		for (int i = 0 ; i != variable_count ; ++i) {
			domain_size[i] = gen_domain_size[i];
			for (int j = 0 ; j != domain_size[i] ; ++j)
				domain[i][j] = gen_domain[i][j];
		}

		/* Initialize values */
		unused_value = new int [constraint_count];
		unknown_variable = new int [constraint_count];
		for (int i = 0 ; i != unknown_variable.length ; ++i) {
			unused_value[i] = constraints.get(i).values.size();
			unknown_variable[i] = constraints.get(i).variables.size();
		}

		/* Initialize used values */
		value_uses = new int [constraint_count][value_count];
		for (int i = 0 ; i != constraint_count ; ++i)
			for (int j = 0 ; j != value_count ; ++j)
				value_uses[i][j] = 0;
		return false;
	}

	/* The backtracking algorithm
	 * Stops when it finds next solution
	 * Will resume to find next one if called again
	 */
	private void go()
	{
		boolean go_on = (variables_fixed == 0);
		int var_i;
		next_variable:
		do {
			if (!go_on) {
				/* Clear effects of last value of top variable */
				var_i = order[variables_fixed - 1];
				int value = domain[var_i][domain_offset[var_i]];
				for (int con_pos_i = 0 ; con_pos_i != attach[var_i].size() ; ++con_pos_i) {
					int con_pos = attach[var_i].get(con_pos_i);
					if (con_pos >= constraint_count)
						break;
					if (--value_uses[con_pos][value - min_value] == 0)
						unused_value[con_pos]++;
					unknown_variable[con_pos]++;
				}
			} else {
				/* Next solution found */
				if (variables_fixed == variable_count) {
					next_found = true;
					return;
				}
				/* Add new variable in the stack */
				int min_order_i = variables_fixed;
				for (int order_i = variables_fixed + 1 ; order_i != variable_count ; ++order_i)
					if (domain_size[order[order_i]] < domain_size[order[min_order_i]])
						min_order_i = order_i;
				var_i = order[min_order_i];
				order[min_order_i] = order[variables_fixed];
				order[variables_fixed++] = var_i;
				domain_offset[var_i] = -1;
			}
			/* Try next value */
			next_value:
			while (++domain_offset[var_i] != domain_size[var_i]) {
				/* Apply effects of new value */
				var_i = order[variables_fixed - 1];
				int value = domain[var_i][domain_offset[var_i]];
				variable[var_i] = value;
				for (int con_pos_i = 0 ; con_pos_i != attach[var_i].size() ; ++con_pos_i) {
					int con_pos = attach[var_i].get(con_pos_i);
					if (con_pos >= constraint_count)
						break;
					/* Too many values for remaining variables */
					if (value_uses[con_pos][value - min_value] != 0 && unused_value[con_pos] == unknown_variable[con_pos]) {
						/* Undo partial changes */
						while (con_pos_i-- != 0) {
							con_pos = attach[var_i].get(con_pos_i);
							if (con_pos >= constraint_count)
								break;
							if (--value_uses[con_pos][value - min_value] == 0)
								unused_value[con_pos]++;
							unknown_variable[con_pos]++;
						}
						/* Go for next value */
						continue next_value;
					}
					if (value_uses[con_pos][value - min_value]++ == 0)
						unused_value[con_pos]--;
					unknown_variable[con_pos]--;
				}
				go_on = true;
				continue next_variable;
			}
			/* No values matching - Backtrack */
			go_on = false;
			variables_fixed--;
		} while (variables_fixed != 0);
		next_found = false;
	}

	/* Check if has next iterator function */
	public boolean hasNext()
	{
		if (finished)
			return false;
		if (next_found)
			return true;
		go();
		if (next_found)
			return true;
		finished = true;
		return false;
	}

	/* Get next iterator */
	public int[] next()
	{
		if (finished)
			throw new NoSuchElementException();
		if (!next_found) {
			go();
			if (!next_found) {
				finished = true;
				throw new NoSuchElementException();
			}
		}
		next_found = false;
		return copy(variable, variable_count);
	}

	/* Remove iterator function */
	public void remove() {}

	/* Variable values */
	int[] variable;

	/* Order of variable usage */
	int[] order;

	/* Number of constraints used */
	int constraint_count;

	/* Domains per variable */
	int[][] domain;

	/* Size of active domain per variable */
	int[] domain_size;

	/* Number of unused values per constraint */
	int[] unused_value;

	/* Number of unknown variables per constraint */
	int[] unknown_variable;

	/* Number of uses per value per constraint */
	int[][] value_uses;

	/* Variables_fixed */
	int variables_fixed;

	/* Domains used */
	int[] domain_offset;

	/* Indicates if next element found */
	boolean next_found;

		/* End of anonymous class for iterator */
		};
	}

	/* Copy len first elements of array */
	private static int[] copy(int[] arr, int len)
	{
		int[] copy = new int [len];
		for (int i = 0 ; i != len ; ++i)
			copy[i] = arr[i];
		return copy;
	}

	/* Print array of ints */
	private static String print(int[] arr)
	{
		if (arr.length == 0)
			return "";
		StringBuffer buf = new StringBuffer();
		buf.append(arr[0]);
		for (int i = 1 ; i != arr.length ; ++i) {
			buf.append(",");
			buf.append(String.valueOf(arr[i]));
		}
		return buf.toString();
	}

	/* Testing main */
	public static void main(String[] args)
	{
		int size = 4;
		int count = 0;
		Combinator engine = new Combinator(size);
		System.out.println("Domains are:");
		for (int v = 1 ; v <= size ; ++v)
			System.out.println("Domain " + v + ": {" + print(engine.domain(v)) + "}");
		int[] q1 = {1, 2, 3, 4};
		int[] a1 = {1, 2, 3};
		int[] q2 = {3, 4};
		int[] a2 = {3};
		engine.constraint(q1, a1);
		engine.constraint(q2, a2);
		System.out.println("Constraints added");
		System.out.println("Domains are:");
		for (int v = 1 ; v <= size ; ++v)
			System.out.println("Domain " + v + ": {" + print(engine.domain(v)) + "}");
		Values engine_values = engine.values();
		System.out.println("Values are:");
		for (int v = 1 ; v <= size ; ++v)
			System.out.println("Values of " + v + ": {" + print(engine_values.values(v)) + "}");
		System.out.println("Solutions:");
		for (int[] a : engine)
			System.out.println(++count + ":\t" + print(a));
	}
}
