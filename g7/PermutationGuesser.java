package mapthatset.g7;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mapthatset.sim.Guesser;
import mapthatset.sim.GuesserAction;

/**
 *  
 * @author neil
 * 
 * Huge Memory Footprint
 * Susceptible to off-by-one errors
 *
 */



public class PermutationGuesser extends Guesser {
	
	List<Set<Integer>> X = new ArrayList<Set<Integer>>();
	int m = -1;
	int turn = 0;
	boolean init = false;
	boolean debug = true;
	Set<Integer> currentQuery = new HashSet<Integer>();

	@Override
	public void startNewMapping(int intMappingLength) {
		// TODO Auto-generated method stub
		m = intMappingLength;
		for(int i = 0; i < m; i++){
			X.add(new HashSet<Integer>());
		}
	}

	@Override
	public GuesserAction nextAction() {
		if(!init){
			return getFirstQuery();
		}
		if( m / Math.pow(2, turn) == 0 ){
			return new GuesserAction("g", generateFinalGuess());
		}
		ArrayList<Integer> query = new ArrayList<Integer>();
		query.addAll(generateNextQuery());
		return new GuesserAction("q",query);
	}

	@Override
	public void setResult(ArrayList<Integer> alResult) {
		
		if(!init){
			initialize(alResult);
		}
		Set<Integer> result = new HashSet<Integer>();
		result.addAll(alResult);
		for(int i=0; i<m; i++){
			if(currentQuery.contains(i+1)){
				X.get(i).retainAll(result);				
			}else{
				X.get(i).removeAll(result);
			}
		}

	}

	@Override
	public String getID() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 
	 *  @return GuesserAction
	 *  
	 *  Just ask for all elements 1 through m  
	 *  
	 */
	private GuesserAction getFirstQuery(){
		if(m == -1){
			System.out.println("ERROR: Uninitialized PermutationGuesser is being used");
		}
		ArrayList<Integer> alActionContent = new ArrayList<Integer>();
		for(int i = 1; i <= m; i++){
			alActionContent.add(i);
		}
		return new GuesserAction("q", alActionContent);
	}
	
	/**
	 * 
	 *  @return Set<Integer>
	 *  
	 *  turn is used to track the depth in the tree
	 *  
	 */
	private Set<Integer> generateNextQuery(){
		Set<Integer> query = new HashSet<Integer>();
		turn++;
		
		for(int j=1; j <= m; j+= 2 * (m / Math.pow(2, turn))){
			for(int i=j; i<= j + m / Math.pow(2, turn) && i <= m; i++){
				query.add(i);
			}
		}	
		currentQuery = query;	
		return currentQuery;
	}
	
	
	private ArrayList<Integer> generateFinalGuess(){
		ArrayList<Integer> guess = new ArrayList<Integer>();
		for(int i=0; i<m; i++){
			if(X.get(i).size() != 1){
				System.out.println("FATAL: Making an incorrect guess!!");
			}
			guess.addAll(X.get(i));
		}
	}
	
	
	private void initialize(ArrayList<Integer> alResult){

		Set<Integer> initialSet = new HashSet<Integer>();
		initialSet.addAll(alResult);
		
		if(initialSet.size() != m){
			System.out.println("WARN: PermutationGuesser being used for a non permutation mapping");
		}
		
		for(Set<Integer> set : X){
			set.addAll(initialSet);
		}
		
		init = true;
		
		if(debug){
			System.out.println("Initialization complete.");
			printStatus();
		}
	
	}
	
	private void printStatus(){
		System.out.println("Current Status:");
		System.out.println("m = "+m);
		for(int i=0; i<m; i++){
			System.out.print(i+"-> {");
			Set<Integer> domain = X.get(i);
			for(Integer x : domain){
				System.out.print(x+", ");
			}
			System.out.println("}");
		}
	}

}
