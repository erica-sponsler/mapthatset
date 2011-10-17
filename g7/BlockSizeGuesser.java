package mapthatset.g7;


public class BlockSizeGuesser {
	
	public static int guessBlockSize(int n, int m, double confidence){
		int k=1;
		double p = 1;
		//System.out.println("Confidence="+confidence);
		for(k=1; k<=m && p >= confidence; k++){
			p *= (((double)(m-k+1))/m);
			//System.out.println("k="+k+" p="+p);
		}
		return k<2 ? (m > 2? 2 : k) : k-2;
	}
	
	public static void main(String[] args){
		System.out.println(guessBlockSize(0, 40, 740.5/800));
		System.out.println(0.975 * (38.0/40));
	}

}
