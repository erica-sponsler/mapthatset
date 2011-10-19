package mapthatset.g7.dev;

import java.util.HashSet;
import java.util.Set;

public class SimpleTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		Set<Integer> query = new HashSet<Integer>();
		
		int m = 20;
		for(int turn=1; turn <= Math.log(m)/Math.log(2); turn++){
			System.out.println("k = "+turn);
			for(int j=1; j <= m; j+= 2 * (m / Math.pow(2, turn))){
				for(int i=j; i<= j + m / Math.pow(2, turn) && i <= m; i++){
					query.add(i);
				}
			for(Integer x : query){
				System.out.print(x+" ");
			}
			query.clear();
			System.out.println();
			}
		}
		

	}

}
