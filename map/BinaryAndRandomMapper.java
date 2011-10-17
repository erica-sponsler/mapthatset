package mapthatset.map;

import java.util.ArrayList;
import java.util.Random;

import mapthatset.sim.GuesserAction;
import mapthatset.sim.Mapper;

public class BinaryAndRandomMapper extends Mapper {

	public String getID()
	{
		return "G7: Half Binary and Half Random Mapper";
	}

	private Random gen = new Random();

	public ArrayList <Integer> startNewMapping(int len)
	{
		int binary = 0, random = 0;
		int v3, v1, v2 = gen.nextInt(len) + 1;
		do {
			v1 = gen.nextInt(len) + 1;
		} while (v1 == v2);
		
		ArrayList<Integer> ret = new ArrayList<Integer>();

		for (int i = 0 ; i != len ; ++i)
		{
			if ((binary < len/2 && gen.nextInt(2) == 0) || random >= len/2)
			{
				ret.add(gen.nextInt(2) == 0 ? v1 : v2);
				++binary;
			}
			else
			{
				do {
					v3 = gen.nextInt(len) + 1;
				} while (v3 == v1 || v3 == v2);
				ret.add(v3);
				++random;
			}
		}
		
		return ret;
	}

	public void updateGuesserAction(GuesserAction g) {}
}

