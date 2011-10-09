package mapthatset.g7;

import java.util.ArrayList;
import java.util.Random;

import mapthatset.sim.GuesserAction;
import mapthatset.sim.Mapper;

public class BinaryMapper extends Mapper {
	
	static final String ID = "G7Mapper";
	static final Random random = new Random();
	private ArrayList<Integer> mapping = new ArrayList<Integer>();
	
	@Override
	public ArrayList<Integer> startNewMapping(int n) {
		int[] y = new int[2];
		y[0] = random.nextInt(n);
		y[1] = random.nextInt(n);
		
		for(int i = 0; i < n; i++){
			mapping.add(y[random.nextInt(2)]);
		}
		
		return mapping;
	}

	@Override
	public void updateGuesserAction(GuesserAction gsaGA) {
		// Do nothing, it's a coin flip mapper

	}

	@Override
	public String getID() {
		return ID;
	}

}
