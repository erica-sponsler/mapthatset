package mapthatset.map;

import java.util.ArrayList;
import java.util.Random;

import mapthatset.sim.GuesserAction;
import mapthatset.sim.Mapper;

public class RandomBinaryAndDistinctMapper extends Mapper {

	public String getID()
	{
		return "G7: Random number of Binary and Random number of Distinct Mapper";
	}

	private Random gen = new Random();
	
	private static void swap(int[] a, int i, int j)
	{
		int t = a[i];
		a[i] = a[j];
		a[j] = t;
	}

	public ArrayList <Integer> startNewMapping(int len)
	{
		int[] vals = new int [len];
		for (int i = 0 ; i != len ; ++i)
			vals[i] = i + 1;
		for (int i = 0 ; i != len ; ++i) {
			int ran = gen.nextInt(len - i) + i;
			swap(vals, i, ran);
		}
		ArrayList <Integer> ret = new ArrayList <Integer> ();
		
		int v1 = -1, v2 = -1;
		for (int i = 0 ; i != len ; ++i)
		{
			if (gen.nextInt(2) == 0)
			{
				if (v1 == -1)
				{
					v1 = vals[i];
					ret.add(v1);
				}
				else if (v2 == -1)
				{
					v2 = vals[i];
					ret.add(v2);
				}
				else
				{
					ret.add(gen.nextInt(2) == 0 ? v1 : v2);
				}
			}
			else
			{
				ret.add(vals[i]);
			}
		}
		
		return ret;
	}

	public void updateGuesserAction(GuesserAction g) {}
}

