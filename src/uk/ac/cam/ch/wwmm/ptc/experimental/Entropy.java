package uk.ac.cam.ch.wwmm.ptc.experimental;

public class Entropy {
	
	public static double getEntropy(double logPbylogPprime) {
		double pByPprime = Math.exp(logPbylogPprime);
		double pPrime = 1.0/(1+pByPprime);
		double p = 1.0 - pPrime;
		
		if(p == 0 || pPrime == 0) return 0;
		
		return -(p*log2(p) + pPrime*log2(pPrime));
	}

	public static double log2(double p) {
		return Math.log(p)/Math.log(2);
	}
	
	public static double getEntropyForProb(double p) {
		double pPrime = 1.0 - p;
		if(p == 0 || pPrime == 0) return 0;
		
		return -(p*log2(p) + pPrime*log2(pPrime));
	}
	
	public static void main(String[] args) {
		System.out.println(getEntropy(-7.0));
		System.out.println(getEntropy(-6.0));
		System.out.println(getEntropy(-5.0));
		System.out.println(getEntropy(-4.0));
		System.out.println(getEntropy(-3.0));
		System.out.println(getEntropy(-2.0));
		System.out.println(getEntropy(-1.0));
		System.out.println(getEntropy(0.0));
		System.out.println(getEntropy(1.0));
		System.out.println(getEntropy(2.0));
		System.out.println(getEntropy(3.0));
		System.out.println(getEntropy(4.0));
		System.out.println(getEntropy(5.0));
		System.out.println(getEntropy(6.0));
		System.out.println(getEntropy(7.0));

	}

}
