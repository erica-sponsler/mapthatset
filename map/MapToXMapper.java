package mapthatset.map;

import java.util.ArrayList;
import java.util.Random;

import mapthatset.sim.GuesserAction;
import mapthatset.sim.Mapper;

public class MapToXMapper extends Mapper {
	private int x = 7;

	public String getID()
	{
		return "G7: MapTo" + x + "Guesser";
	}

	private Random gen = new Random();

	public ArrayList <Integer> startNewMapping(int len)
	{
		ArrayList <Integer> list = new ArrayList <Integer> ();
		ArrayList<Integer> vals = new ArrayList<Integer>();
		if (len == 1)
			list.add(1);
		else {
			for (int i = 0; i < x; ++i)
			{
				int j;
				do {
					j = gen.nextInt(len) + 1;
				} while (vals.contains(j));
				vals.add(j);
			}
			for (int i = 0 ; i != len ; ++i)
				list.add(vals.get(gen.nextInt(x)));
		}
		while (!list.containsAll(vals))
		{
			for (Integer i : vals)
			{
				if (!list.contains(i))
					list.set(gen.nextInt(len), i);
			}
		}
		return list;
	}

	public void updateGuesserAction(GuesserAction g) {}
}

