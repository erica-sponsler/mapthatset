package mapthatset.g7.dev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import mapthatset.sim.Guesser;
import mapthatset.sim.GuesserAction;

public class MixGuesser extends Guesser {

	public String getID()
	{
		return "G7: Guesser";
	}

	private int variable_count;
	private int value_count;
	private int round;
	private boolean guess;
	private ArrayList <Integer> query;
	private Combinator engine;
	private Random random;
	private Vector <Pair <ArrayList <Integer>, ArrayList <Integer>>> history;

	public void startNewMapping(int len)
	{
		variable_count = len;
		value_count = 0;
		round = 0;
		guess = false;
		binary = false;
		distinct = false;
		cross = false;
		random = new Random();
		engine = new Combinator(len);
		history = new Vector <Pair <ArrayList <Integer>, ArrayList <Integer>>> ();
	}

	public GuesserAction nextAction()
	{
		round++;
		query = new ArrayList <Integer> ();
		for (int i = 1 ; i <= variable_count && query != null ; ++i) {
			int[] domain = engine.domain(i);
			if (domain.length == 1)
				query.add(domain[0]);
			else
				query = null;
		}
		if (query != null) {
			guess = true;
			return new GuesserAction("g", query);
		}
		if (value_count == 0)
			query = firstQuery();
		else if (binary)
			query = binaryQuery();
		else if (distinct)
			query = distinctQuery();
		else if (cross)
			query = crossQuery();
		guess = false;
		return new GuesserAction("q", query);
	}

	public void setResult(ArrayList <Integer> result)
	{
		if (guess) return;
		history.add(new Pair <ArrayList <Integer>, ArrayList <Integer>> (query, result));
		engine.constraint(toArray(query), toArray(result));
		if (round == 1) {
			value_count = result.size();
			if (value_count == 2)
				binary = true;
			else if (value_count == variable_count)
				distinct = true;
			else
				cross = true;
			return;
		}
		if (binary)
			binaryResult(result);
		else if (distinct)
			distinctResult(result);
		else if (cross)
			crossResult(result);
	}

	/* Ask for all variables */
	private ArrayList <Integer> firstQuery()
	{
		ArrayList <Integer> list = new ArrayList <Integer> ();
		for (int i = 1 ; i <= variable_count ; ++i)
			list.add(i);
		return list;
	}

	/* Optimal strategy for binary mapper */
	private boolean binary;

	private HashSet <Integer> active_variables;

	private ArrayList <Integer> binaryQuery()
	{
		if (round == 2) {
			active_variables = new HashSet <Integer> ();
			for (int i = 1 ; i <= variable_count ; ++i)
				active_variables.add(i);
		}
		ArrayList <Integer> list = new ArrayList <Integer> ();
		for (int i = 0 ; i != 2 && active_variables.size() != 0 ; ++i) {	
			int x = active_variables.iterator().next();
			active_variables.remove(x);
			list.add(x);
		}
		return list;
	}

	private void binaryResult(ArrayList <Integer> result)
	{
		if (result.size() == 2)
			active_variables.add(query.get(0));
		if (active_variables.size() == 0)
			engine.refine();
	}

	/* Optimal strategy for distinct mapper */
	private boolean distinct;

	private ArrayList <Integer> distinctQuery()
	{
		ArrayList <Integer> list = new ArrayList <Integer> ();
		int limit = ceilLog(variable_count);
		int set_size = 1 << (limit - round + 1);
		int var = 1;
		do {
			for (int i = 0 ; i != set_size ; ++i) {
				if (var > variable_count)
					return list;
				list.add(var++);
			}
			var += set_size;
		} while (var <= variable_count);
		return list;
	}

	private void distinctResult(ArrayList <Integer> result)
	{
		ArrayList <Integer> rest_vars = new ArrayList <Integer> ();
		ArrayList <Integer> rest_vals = new ArrayList <Integer> ();
		for (int i = 1 ; i <= variable_count ; ++i) {
			if (!query.contains(i))
				rest_vars.add(i);
			if (!result.contains(i))
				rest_vals.add(i);
		}
		engine.constraint(toArray(rest_vars), toArray(rest_vals));
	}

	/* Cross cutting technique */
	private boolean cross;

	/* Uses of variables */
	private int[] uses;

	/* Active variables */
	private HashSet <Integer> active;

	/* Functional dependency */
	private HashMap <Integer, Pair <Integer, HashMap <Integer, Integer>>> dependency;

	/* Variables already combined in query */
	private HashSet <Integer> no_combine;

	/* All solutions */
	private int[][] solutions;

	private class UseCompare implements Comparator <Integer> {

		public int compare(Integer var_i, Integer var_j)
		{
			return uses[var_i - 1] - uses[var_j - 1];
		}
	}

	private ArrayList <Integer> crossQuery()
	{
		ArrayList <Integer> list = new ArrayList <Integer> ();
		if (round == 2) {
			uses = new int [variable_count];
			active = new HashSet <Integer> ();
			for (int i = 0 ; i != variable_count ; ++i) {
				uses[i] = 0;
				active.add(i + 1);
			}
			no_combine = new HashSet <Integer> ();
			solutions = null;
			dependency = new HashMap <Integer, Pair <Integer, HashMap <Integer, Integer>>> ();
		}
		int limit = (int) Math.ceil(Math.sqrt(variable_count));
		if (active.size() == 0)
			return nextAction().getContent();
		Integer[] least_used = new Integer [active.size()];
		int i = 0;
		for (int var : active)
			least_used[i++] = var;
		Arrays.sort(least_used, 0, active.size(), new UseCompare());
		next_var:
		for (i = 0 ; i != active.size(); ++i) {
			int var_i = least_used[i];
			for (int var_j : list)
				if (no_combine.contains((var_i - 1) * variable_count + var_j - 1))
					continue next_var;
			list.add(var_i);
			uses[var_i - 1]++;
			if (list.size() == limit)
				break;
		}
		return list;
	}

	private void crossResult(ArrayList <Integer> answer)
	{
		for (int var_i : query)
			for (int var_j : query) {
				if (var_i >= var_j) continue;
				no_combine.add((var_i - 1) * variable_count + var_j - 1);
				no_combine.add((var_j - 1) * variable_count + var_i - 1);
			}
		engine.timeoutLimit(100);
		if (solutions == null)
			solutions = engine.findall();
		else
			solutions = engine.filter(solutions);
		if (solutions != null)
			functionalDependency();
	//	dependencyResolve();
		Iterator <Integer> it = active.iterator();
		while (it.hasNext()) {
			int var = it.next();
			if (engine.domain(var).length == 1) {
				active.remove(var);
				it = active.iterator();
			}
		}
	}

	private void functionalDependency()
	{
		/* Eliminate variables that are uniquely identified by others */
		for (int i = 1 ; i <= variable_count ; ++i) {
			if (!active.contains(i) || engine.domain(i).length == 1)
				continue;
			next_pair:
			/* Check x_i, x_j where j > i */
			for (int j = 1 ; j <= variable_count ; ++j) {
				if (i == j || !active.contains(j) || engine.domain(j).length == 1)
					continue;
				HashMap <Integer, Integer> value_mapping = new HashMap <Integer, Integer> ();
				for (int[] solution : solutions) {
					int vi = solution[i - 1];
					int vj = solution[j - 1];
					if (value_mapping.get(vi) != null && value_mapping.get(vi) != vj)
						continue next_pair;
					value_mapping.put(vi, vj);
				//	no_combine.add((i - 1) * variable_count + j - 1);
				//	no_combine.add((j - 1) * variable_count + i - 1);
				}
				dependency.put(i, new Pair <Integer, HashMap <Integer, Integer>> (j, value_mapping));
				System.out.println("FD: " + i + " -> " + j);
			}
		}
	}

	private void dependencyResolve()
	{
		Iterator <Integer> it = dependency.keySet().iterator();
		while (it.hasNext()) {
			int i = it.next();
			if (engine.domain(i).length != 1)
				continue;
			int vi = engine.domain(i)[0];
			int j = dependency.get(i).fst;
			if (engine.domain(j).length != 1) {
				int vj = dependency.get(i).snd.get(vi);
				int[] j_a = {j};
				int[] vj_a = {vj};
				engine.constraint(j_a, vj_a);
			}
			dependency.remove(i);
			it = dependency.keySet().iterator();
			System.out.println("Resolve: " + i + " -> " + j);
		}
	}

	private static int ceilLog(int n)
	{
		int i = 0;
		while ((1 << i) < n)
			i++;
		return i;
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
