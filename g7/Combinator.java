package mapthatset.g7;

import java.util.Arrays;
import java.util.Random;
import java.util.Vector;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/* Combinator class is a tool that stores a
 * number of variables and constraints for
 * them and can be iterated to give all possible
 * solutions that satisfy the constraints given
 */
class Combinator implements Iterable <int[]> {

	/* Display debug info */
	private static final boolean debug = false;

	/* Random generator */
	private static final Random random = new Random();

	/* Number of variables */
	private final int variable_count;

	/* Minimum value */
	private final int min_value;

	/* Maximum value */
	private final int max_value;

	/* Domains per variable */
	private int[][] gen_domain;

	/* Size of active domain per variable */
	private int[] gen_domain_size;

	/* Is last changes propagated ? */
	private HashSet <Integer> propagation_variables;

	/* Class representing problem's constraints
	 * There is only one type of constraints
	 * for this problem that actually function
	 * as multiple different constraints but
	 * we check them all together using this
	 */
	private class Constraint {

		/* Different variables */
		public HashSet <Integer> variables;

		/* Different values on constraint */
		public HashSet <Integer> values;

		/* Initialize constraint between variables */
		public Constraint(int[] vars, int[] vals)
		{
			/* Copy and check variables */
			variables = new HashSet <Integer> ();
			for (int i = 0 ; i != vars.length ; ++i) {
				if (vars[i] <= 0 || vars[i] > variable_count)
					throw new IllegalArgumentException();
				variables.add(vars[i] - 1);
			}

			/* Copy and check values */
			values = new HashSet <Integer> ();
			for (int i = 0 ; i != vals.length ; ++i) {
				if (vals[i] < min_value || vals[i] > max_value)
					throw new IllegalArgumentException();
				values.add(vals[i]);
			}

			/* Values cannot be less than variables */
			if (variables.size() < values.size())
				throw new IllegalArgumentException();
		}
	}

	/* Typedef array of integers to allow array creation */
	private static class IntArray extends Vector <Integer> {
		private static final long serialVersionUID = 1l;
	}

	/* Constraints for the problem */
	private Vector <Constraint> constraints;

	/* Constraints for each variable */
	private IntArray[] attach;

	/* Timeout for iterator */
	private long iterator_timeout_limit;

	/* Backtrack limit for iterator */
	private long iterator_backtrack_limit;

	/* Values used per variable limit */
	private int iterator_value_limit;

	/* Define exception for timeout */
	public static class TimeoutException extends RuntimeException {
		private static final long serialVersionUID = 2l;

		/* Diagnostic */
		public TimeoutException() {
			super("Combinator search timeout occured");
		}
	}

	/* Define exception for backtrack limit */
	public static class BacktrackException extends RuntimeException {
		private static final long serialVersionUID = 3l;

		/* Diagnostic */
		public BacktrackException() {
			super("Combinator search backtrack limit reached");
		}
	}

	/* Constructor with default minimum and maximum value */
	public Combinator(int vars)
	{
		this(vars, 1, vars);
	}

	/* Constructor */
	public Combinator(int vars, int min, int max)
	{
		/* Check and copy basic parameters */
		if (vars <= 0 || min > max)
			throw new IllegalArgumentException();
		variable_count = vars;
		min_value = min;
		max_value = max;

		/* Create array for constraints and attachments */
		constraints = new Vector <Constraint> ();
		attach = new IntArray [vars];
		for (int i = 0 ; i != vars ; ++i)
			attach[i] = new IntArray();

		/* Create domains and their sizes */
		int value_count = max_value - min_value + 1;
		gen_domain = new int [variable_count][value_count];
		gen_domain_size = new int [variable_count];
		for (int i = 0 ; i != variable_count ; ++i) {
			gen_domain_size[i] = value_count;
			for (int j = 0 ; j != value_count ; ++j)
				gen_domain[i][j] = j + min_value;
		}
		iterator_timeout_limit = Long.MAX_VALUE;
		iterator_backtrack_limit = Long.MAX_VALUE;
		iterator_value_limit = Integer.MAX_VALUE;
		propagation_variables = new HashSet <Integer> ();
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

			/* Cut the domains of variable to match the new constraint */
			for (int val_i = 0 ; val_i != gen_domain_size[var_i] ; ++val_i)
				if (!new_constraint.values.contains(gen_domain[var_i][val_i])) {
					gen_domain[var_i][val_i--] = gen_domain[var_i][--gen_domain_size[var_i]];
					propagation_variables.add(var_i);
				}
		}
	}

	/* Find out cases where there the same number
	 * of free variables and unused values
	 * So each one of the free variables can
	 * only pick from those values
	 */
	private void propagate()
	{
		/* Check propagation between variables */
		while (propagation_variables.size() != 0) {
			int var = propagation_variables.iterator().next();
			propagation_variables.remove(var);
			for (int con_pos : attach[var]) {
				Constraint con = constraints.get(con_pos);

				/* Get unassigned variables and values */
				HashSet <Integer> unassigned_variables = new HashSet <Integer> ();
				HashSet <Integer> used_values = new HashSet <Integer> ();
				for (int var_i : con.variables)
					if (gen_domain_size[var_i] != 1)
						unassigned_variables.add(var_i);
					else
						used_values.add(gen_domain[var_i][0]);
				HashSet <Integer> unused_values = new HashSet <Integer> ();
				for (int value : con.values)
					if (!used_values.contains(value))
						unused_values.add(value);

				/* Cut domains if number of free variables is same
				 * as number of unused values in the constraint
				 */
				if (unassigned_variables.size() != unused_values.size())
					continue;
				for (int var_i : unassigned_variables)
					for (int val_i = 0 ; val_i != gen_domain_size[var_i] ; ++val_i) {
						if (!unused_values.contains(gen_domain[var_i][val_i])) {
							gen_domain[var_i][val_i--] = gen_domain[var_i][--gen_domain_size[var_i]];
							propagation_variables.add(var_i);
						}
						if (gen_domain_size[var_i] == 0)
							throw new RuntimeException();
					}
			}
		}
	}

	/* Size of one variable's domain
	 * WITHOUT running the search
	 */
	public int[] domain(int var_i)
	{
		propagate();
		if (var_i <= 0 || var_i-- > variable_count)
			throw new IllegalArgumentException();
		return Arrays.copyOf(gen_domain[var_i], gen_domain_size[var_i]);
	}

	/* Returns unique solution else null
	 * Throws exception if unsolvable
	 */
	public int[] unique()
	{
		Iterator <int[]> it = iterator();
		if (!it.hasNext())
			throw new NoSuchElementException();
		int[] solution = it.next();
		if (it.hasNext())
			return null;
		return solution;
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
		variables_fixed = 0;
		variable = new int [variable_count];
		order = new int [variable_count];
		for (int i = 0 ; i != variable_count ; ++i) {
			variable[i] = min_value - 1;
			order[i] = i;
		}

		/* Initialize domains and copy them */
		propagate();
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

		/* Cut variable stack and counters */
		cut_values = new int [variable_count];
		cut_stack = new int [variable_count * value_count];
		cut_stack_top = 0;

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

		/* Set space search limits */
		timeout_limit = iterator_timeout_limit;
		backtrack_limit = iterator_backtrack_limit;
		value_limit = iterator_value_limit;

		/* Set search statistics */
		used_time = 0;
		backtracks = 0;

		/* Haven't found first yet and not finished */		
		return next_found = false;
	}

	/* The backtracking algorithm
	 * Stops when it finds next solution
	 * Will resume to find next one if called again
	 */
	private boolean go()
	{
		long tick = System.currentTimeMillis();
		boolean go_on = (variables_fixed == 0);
		int var_i, rand, offset;
		next_variable:
		do {
			if (!go_on) {

				/* Clear effects of last value of top variable */
				var_i = order[variables_fixed - 1];
				int value = variable[var_i];
				variable[var_i] = min_value - 1;

				/* Clear modifications on constraints */
				for (int con_pos_i = 0 ; con_pos_i != attach[var_i].size() ; ++con_pos_i) {
					int con_pos = attach[var_i].get(con_pos_i);
					if (con_pos >= constraint_count)
						break;
					if (--value_uses[con_pos][value - min_value] == 0)
						unused_value[con_pos]++;
					unknown_variable[con_pos]++;
				}

				/* Restore cut values */
				for (int cut = 0 ; cut != cut_values[var_i] ; ++cut) {
					int cut_var_i = cut_stack[--cut_stack_top];
					domain_size[cut_var_i]++;
				}
				cut_values[var_i] = 0;
			} else {

				/* Next solution found */
				if (variables_fixed == variable_count) {
					used_time += System.currentTimeMillis() - tick;
					return true;
				}

				/* Add new variable in the stack
				 * Pick the one with the smallest
				 * domain (MRV in AI literature)
				 */
				int min_order_i = variables_fixed;
				for (int order_i = variables_fixed + 1 ;
				     order_i != variable_count ; ++order_i)
					if (domain_size[order[order_i]] < domain_size[order[min_order_i]])
						min_order_i = order_i;

				/* Fix the order array and initialize stuff */
				swap(order, min_order_i, variables_fixed);
				var_i = order[variables_fixed++];
				domain_offset[var_i] = -1;
				cut_values[var_i] = 0;
			}

			/* Print out all assigned values and all domain values */
			if (debug) {
				System.out.println("");
				for (int i = 0 ; i != variable_count ; ++i) {
					int var = order[i]; 
					System.out.print("x" + (var + 1) + "::[" + domain[var][0]);
					for (int j = 1 ; j != domain_size[var] ; ++j)
						System.out.print("," + domain[var][j]);
					if (i < variables_fixed)
						System.out.println("] ( x" + (var + 1) + " <- " + variable[var] + " )");
					else
						System.out.println("]");
				}
				System.out.println("");
			}

			/* Try next value in domain for variable */
			next_value:
			while (++domain_offset[var_i] != domain_size[var_i]) {

				/* Pick a random value to put from the domain */
				offset = domain_offset[var_i];
				rand = random.nextInt(domain_size[var_i] - offset);
				swap(domain[var_i], domain_offset[var_i], rand + offset);

				/* Limit used values */
				if (offset == value_limit)
					break;

				/* Set new value to variable */
				int value = domain[var_i][offset];
				variable[var_i] = value;

				/* Print debug for value placed */
				if (debug)
					System.out.println("x" + (var_i + 1) + " <- " + value);

				/* Check all constraints */
				for (int con_pos_i = 0 ; con_pos_i != attach[var_i].size() ; ++con_pos_i) {
					int con_pos = attach[var_i].get(con_pos_i);
					if (con_pos >= constraint_count)
						break;

					/* If free variables now less than
					 * unused values try next value
					 */
					if (value_uses[con_pos][value - min_value] != 0 &&
					    unused_value[con_pos] == unknown_variable[con_pos]) {

						/* Undo partial changes to constraints */
						while (con_pos_i-- != 0) {
							con_pos = attach[var_i].get(con_pos_i);
							if (con_pos >= constraint_count)
								break;
							if (--value_uses[con_pos][value - min_value] == 0)
								unused_value[con_pos]++;
							unknown_variable[con_pos]++;
						}

						/* Undo partial changes to domains */
						for (int cut = 0 ; cut != cut_values[var_i] ; ++cut) {
							int cut_var_i = cut_stack[--cut_stack_top];
							domain_size[cut_var_i]++;
						}
						cut_values[var_i] = 0;

						/* Try next value */
						continue next_value;
					}

					/* Update constraints */
					if (value_uses[con_pos][value - min_value]++ == 0)
						unused_value[con_pos]--;
					unknown_variable[con_pos]--;

					/* If exactly as many unknown variables as unused 
					 * values you will need to shorten domains for every
					 * other variable attached to the constraint
					 */
					if (unused_value[con_pos] == unknown_variable[con_pos])
						for (int aff_var_i : constraints.get(con_pos).variables) {

							/* Ignore already set variables */
							if (variable[aff_var_i] != min_value - 1)
								continue;

							/* Cut used values of constraint from the domains of unknown variables */
							for (int val_i = 0 ; val_i != domain_size[aff_var_i] ; ++val_i)
								if (value_uses[con_pos][domain[aff_var_i][val_i] - min_value] != 0) {
									cut_values[var_i]++;
									swap(domain[aff_var_i], val_i--, --domain_size[aff_var_i]);
									cut_stack[cut_stack_top++] = aff_var_i;
								}

							/* If domain size becomes zero you have to undo domain cuts */
							if (domain_size[aff_var_i] == 0) {

								/* Undo partial changes to constraints */
								while (con_pos_i-- != 0) {
									con_pos = attach[aff_var_i].get(con_pos_i);
									if (con_pos >= constraint_count)
										break;
									if (--value_uses[con_pos][value - min_value] == 0)
										unused_value[con_pos]++;
									unknown_variable[con_pos]++;
								}

								/* Undo partial changes to domains */
								for (int cut = 0 ; cut != cut_values[var_i] ; ++cut) {
									int cut_var_i = cut_stack[--cut_stack_top];
									domain_size[cut_var_i]++;
								}
								cut_values[var_i] = 0;

								/* Try next value */
								continue next_value;
							}
						}
				}
				/* Value was set successfully
				 * Go to try for the next variable
				 */
				go_on = true;
				continue next_variable;
			}
			/* No values matching - Backtrack */
			go_on = false;
			variables_fixed--;

			/* Check backtrack limit */
			if (backtracks++ == backtrack_limit)
				throw new BacktrackException();

			/* Check timeout limit every 4096 backtracks */
			if ((backtracks & 4095) == 0) {
				long new_tick = System.currentTimeMillis();
				used_time += new_tick - tick;
				if (used_time > timeout_limit)
					throw new TimeoutException();
				tick = new_tick;
			}

		/* Failed when backtracking required for 1st variable */
		} while (variables_fixed != 0);
		return false;
	}

	/* Check if has next iterator function */
	public boolean hasNext()
	{
		if (finished)
			return false;
		if (next_found || go())
			return next_found = true;
		finished = true;
		return false;
	}

	/* Get next iterator */
	public int[] next()
	{
		if (finished)
			throw new NoSuchElementException();
		if (!next_found && !go()) {
			finished = true;
			throw new NoSuchElementException();
		}
		next_found = false;
		return Arrays.copyOf(variable, variable_count);
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

	/* Number of values cut from each domain */
	int[] cut_values;

	/* Domain cuts while running */
	int[] cut_stack;

	/* Number of elements in cut array */
	int cut_stack_top;

	/* Indicates if next element found */
	boolean next_found;

	/* Timeout for iterator */
	long timeout_limit;

	/* Backtrack limit for iterator */
	long backtrack_limit;

	/* Values used per variable limit */
	int value_limit;

	/* Used time */
	long used_time;

	/* Backtrack count */
	long backtracks;

		/* End of anonymous class for iterator */
		};
	}

	/* Typedef set of integers to allow array creation */
	private static class IntSet extends HashSet <Integer> {
		private static final long serialVersionUID = 4l;
	}

	/* Set timeout for the iterators
	 * that are produced by this class
	 * Zero or negative means infinity
	 */
	public void timeoutLimit(long millis)
	{
		iterator_timeout_limit = !debug && millis > 0 ? millis : Long.MAX_VALUE;
	}

	/* Set limit in the number of backtracks
	 * Negative means infinity
	 */
	public void backtrackLimit(long count)
	{
		iterator_backtrack_limit = count >= 0 ? count : Long.MAX_VALUE;
	}

	/* Number of values tried for each variable
	 * Zero or negative means infinity
	 */
	public void valueLimit(int count)
	{
		iterator_value_limit = count > 0 ? count : Integer.MAX_VALUE;
	}

	/* Find all solutions and use them to refine domains
	 * Returns number of solutions found
	 * If no solutions are found 0 is returned and
	 * the domains are left untouched
	 * Returns -1 if a timeout occurs
	 * Returns -2 if backtracking limit is reached
	 */
	public long refine()
	{
		long[] count = new long [1];
		try {
			findall(false, count);
		} catch (NoSuchElementException e) {
			return 0l;
		} catch (TimeoutException e) {
			return -1l;
		} catch (BacktrackException e) {
			return -2l;
		}
		return count[0];
	}

	/* Find all solutions and return them if possible
	 * Returns null if timeout occurs or the problem
	 * is unsolvable or the number of solutions is very big
	 * Throws exception if no solution is found
	 */
	public int[][] findall()
	{
		long[] count = new long [1];
		try {
			return findall(true, count);
		} catch (TimeoutException e) {
			return null;
		} catch (BacktrackException e) {
			return null;
		}
	}

	/* Find all solutions and refine domains
	 * Returns solutions depending on argument
	 * Used to implement findall() and refine()
	 * If problem cannot be solved throw exception
	 * If timeout occurs of backtrack limit is
	 * reached also throw exception for each case
	 */
	private int[][] findall(boolean return_solutions, long[] count)
	{
		propagate();
		Iterator <int[]> it = iterator();
		IntSet [] real_domain = new IntSet [variable_count];
		for (int i = 0 ; i != variable_count ; ++i)
			real_domain[i] = new IntSet();
		Vector <int[]> all_solutions = new Vector <int[]> ();
		long solutions = 0;
		while (it.hasNext()) {
			solutions++;
			int[] solution = it.next();
			if (return_solutions)
				all_solutions.add(solution);
			for (int val_i = 0 ; val_i != variable_count ; ++val_i)
				real_domain[val_i].add(solution[val_i]);
		}
		/* If no solution found throw exception */
		if (solutions == 0)
			throw new NoSuchElementException();
		/* Cut domains to contain only values found in solutions */
		for (int var_i = 0 ; var_i != variable_count ; ++var_i)
			for (int val_i = 0 ; val_i != gen_domain_size[var_i] ; ++val_i)
				if (!real_domain[var_i].contains(gen_domain[var_i][val_i])) {
					gen_domain[var_i][val_i--] = gen_domain[var_i][--gen_domain_size[var_i]];
					propagation_variables.add(var_i);
				}
		count[0] = solutions;
		/* Solutions not required or too many */
		if (!return_solutions || solutions > (long) Integer.MAX_VALUE)
			return null;
		/* Return solutions as an array */
		int[][] all_solutions_arr = new int [all_solutions.size()][];
		int i = 0;
		for (int[] solution : all_solutions)
			all_solutions_arr[i++] = solution;
		return all_solutions_arr;
	}

	/* Filter based on a superset of solutions
	 * Also refines the domains
	 * If no solution throw exception
	 */
	public int[][] filter(int[][] solutions_superset)
	{
		IntSet [] real_domain = new IntSet [variable_count];
		for (int i = 0 ; i != variable_count ; ++i)
			real_domain[i] = new IntSet();
		Vector <int[]> all_solutions = new Vector <int[]> ();
		next_solution:
		for (int[] solution : solutions_superset) {
			/* Check all constraints */
			for (Constraint con : constraints) {
				HashSet <Integer> used_values = new HashSet <Integer> ();
				for (int var_i : con.variables) {
					int value = solution[var_i];
					if (!con.values.contains(value))
						continue next_solution;
					used_values.add(value);
				}
				if (used_values.size() != con.values.size())
					continue next_solution;
			}
			all_solutions.add(solution);
			for (int val_i = 0 ; val_i != variable_count ; ++val_i)
				real_domain[val_i].add(solution[val_i]);
		}
		/* If no solution found throw exception */
		if (all_solutions.size() == 0)
			throw new NoSuchElementException();
		/* Cut domains to contain only values found in solutions */
		for (int var_i = 0 ; var_i != variable_count ; ++var_i)
			for (int val_i = 0 ; val_i != gen_domain_size[var_i] ; ++val_i)
				if (!real_domain[var_i].contains(gen_domain[var_i][val_i])) {
					gen_domain[var_i][val_i--] = gen_domain[var_i][--gen_domain_size[var_i]];
					propagation_variables.add(var_i);
				}
		/* Return solutions as an array */
		int[][] all_solutions_arr = new int [all_solutions.size()][];
		int i = 0;
		for (int[] solution : all_solutions)
			all_solutions_arr[i++] = solution;
		return all_solutions_arr;
	}

	/* Swap elements of array */
	private static void swap(int[] a, int i, int j)
	{
		int t = a[i];
		a[i] = a[j];
		a[j] = t;
	}

	/* Testing main */
	public static void main(String[] args)
	{
		int size = 32;
		int[] mapping = randomMapping(size);
		mapping = distinctMapping(size);
		System.out.println("Mapping:  " + toString(mapping));
		HashSet <Integer> query = new HashSet <Integer> ();
		HashSet <Integer> result = new HashSet <Integer> ();
		int qsize = (int) Math.ceil(Math.sqrt(size));
		int dsize, turn = 1;
		Combinator engine = new Combinator(size);
		engine.timeoutLimit(1000);
		int reds = 0;
		do {
			query.clear();
			result.clear();
			for (int i = 0 ; i != (turn == 1 ? size * 100 : qsize) ; ++i)
				query.add(random.nextInt(size) + 1);
			for (int x : query)
				result.add(mapping[x - 1]);
			System.out.println("\nTurn: " + turn);
			engine.constraint(toArray(query), toArray(result));
			System.out.println("Query:   " + toString(toArray(query)));
			System.out.println("Result:  " + toString(toArray(result)));
			int bsize = 0;
			for (int v = 1 ; v <= size ; ++v)
				bsize += engine.domain(v).length;
			long time = System.currentTimeMillis();
			System.out.print("Searching...");
			boolean timeout = true;
			try {
				engine.findall();
				timeout = false;
			} catch (TimeoutException e) {
				System.out.println("   Timeout.  :(");
			}
			if (timeout == false) {
				time = System.currentTimeMillis() - time;
				System.out.println("   Done!  :D    (" + time + " ms)");
			}
			dsize = 0;
			System.out.println("Domains:");
			for (int v = 1 ; v <= size ; ++v) {
				dsize += engine.domain(v).length;
				System.out.println("Domain " + v + ": {" + toString(engine.domain(v)) + "}");
			}
			if (bsize == dsize)
				System.out.print("No reduction...");
			else {
				System.out.print("Reduction!");
				reds++;
			}
			if (timeout)
				System.out.println("   (timeout)");
			else
				System.out.println("   (no timeout)");
			turn++;
		} while (dsize != size);
		System.out.println("\nMapping:  " + toString(mapping));
		System.out.println("\nReductions:  " + reds);
	}

	private static int[] randomMapping(int size)
	{
		Random gen = new Random();
		int[] mapping = new int [size];
		for (int i = 0 ; i != size ; ++i)
			mapping[i] = gen.nextInt(size) + 1;
		return mapping;
	}

	private static int[] distinctMapping(int size)
	{
		Random gen = new Random();
		int[] mapping = new int [size];
		for (int i = 0 ; i != size ; ++i)
			mapping[i] = i + 1;
		for (int i = 0 ; i != size ; ++i)
			swap(mapping, i, gen.nextInt(size - i) + i);
		return mapping;
	}

	private static String toString(int[] a) {
		if (a.length == 0)
			return "";
		StringBuffer buf = new StringBuffer();
		buf.append(a[0]);
		for (int i = 1 ; i != a.length ; ++i) {
			buf.append(',');
			buf.append(a[i]);
		}
		return buf.toString();
	}

	private static int[] toArray(HashSet <Integer> s)
	{
		int[] r = new int [s.size()];
		int i = 0;
		for (int n : s)
			r[i++] = n;
		return r;
	}
}
