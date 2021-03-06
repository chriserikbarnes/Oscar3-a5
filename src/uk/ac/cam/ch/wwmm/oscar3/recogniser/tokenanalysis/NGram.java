package uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.models.ExtractTrainingData;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;

/**
 * Encapsulates nGram parsing.
 *
 * Singleton pattern avoids time-consuming reinitialisation
 * and smoothing of nGram counts. Code should instance via static
 * &lt;code&gt;getInstance&lt/code&gt method.
 * @author caw47, annexed by ptc24
 */
public class NGram {
	
	/**
	 * Holds unique instance required by singleton pattern.
	 */    
	private static NGram myInstance = null;
	//private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3/recogniser/resources/");
	
	private String alphabet;
	
	// nGram counts
	private int[]       C1C;
	private int[][]     C2C;
	private int[][][]   C3C;
	private int[][][][] C4C;
	private int[]       E1C;
	private int[][]     E2C;
	private int[][][]   E3C;
	private int[][][][] E4C;
	
	// smoothed nGrams (log probs)
	private double[][][][] LP4C;
	private double[][][][] LP4E;
	
	
	private Collection<String> extraChemical;
	private Collection<String> extraEnglish;
	
	private boolean extraOnly;
	
	private Set<String> dontUse;
	
	private List<String> chemWords;
	private List<String> englishWords;
	public Set<String> chemSet;
	public Set<String> engSet;
		
	private SuffixClassifier suffixClassifier;
	
	/** Creates a new instance of nGram */
	private NGram() {
		setAlphabet("$^S0%<>&'()*+,-./:;=?@[]abcdefghijklmnopqrstuvwxyz|~");
		extraChemical = null;
		extraEnglish = null;
		extraOnly = false;
	}
	
	/**
	 * Returns instance of singleton, instancing class if necessary.
	 * @return Instance of singleton
	 * @throws Exception Training data cannot be loaded
	 */    
	public static NGram getInstance() {
		if(myInstance==null) {
			myInstance = new NGram();
			myInstance.initialise();
		}
		return myInstance;
	}	
		
	public static void reinitialise() throws Exception {
		myInstance = null;
		getInstance();
	}
	
	public static void reinitialise(Collection<String> chemicals, Collection<String> english, boolean extraOnly) throws Exception {
		myInstance = new NGram();
		myInstance.extraChemical = chemicals;
		myInstance.extraEnglish = english;
		myInstance.extraOnly = extraOnly;
		myInstance.initialise();		
	}
	
	private void initialise() {
		if(Oscar3Props.getInstance().verbose) System.out.print("Initialising nGrams... ");
		dontUse = new HashSet<String>();
		chemWords = new ArrayList<String>();
		englishWords = new ArrayList<String>();
		readTrainingData();
		train();
	}
	/**
	 * Load training data and calculate smoothed nGrams
	 */    
	private void train() {
		//try {
		//	betasquared = Double.parseDouble(Oscar3Props.getProperty("betasquared"));
		//} catch (Exception e) {
		
		// initialise count arrays        
		int a = alphabet.length();
		C1C = new int[a];
		C2C = new int[a][a];
		C3C = new int[a][a][a];
		C4C = new int[a][a][a][a];
		E1C = new int[a];
		E2C = new int[a][a];
		E3C = new int[a][a][a];
		E4C = new int[a][a][a][a];
		for(int i=0;i<a;i++) {
			C1C[i] = 0;
			E1C[i] = 0;
			for(int j=0;j<a;j++) {
				C2C[i][j] = 0;
				E2C[i][j] = 0;
				for(int k=0;k<a;k++) {
					C3C[i][j][k] = 0;
					E3C[i][j][k] = 0;
					for(int l=0;l<a;l++) {
						C4C[i][j][k][l] = 0;
						E4C[i][j][k][l] = 0;
					}
				}
			}
		}
		
		engSet = new HashSet<String>();
		for(String e : englishWords) {
			addEngNGrams(e);
			engSet.add(parseWord(e));
		}
		chemSet = new HashSet<String>();
		for(String c : chemWords) {
			addChemNGrams(c);
			chemSet.add(parseWord(c));
		}
		//System.out.println(chemSet);
		//System.out.println(engSet);
		
		// smooth nGrams and calculate logP data
		LP4C = calcLP4(C1C, C2C, C3C, C4C);
		LP4E = calcLP4(E1C, E2C, E3C, E4C);
		
		// release count arrays to free up memory
		C1C = null;
		C2C = null;
		C3C = null;
		C4C = null;
		E1C = null;
		E2C = null;
		E3C = null;
		E4C = null;
		System.gc();
		
		//suffixClassifier = new SuffixClassifier(chemSet, engSet);
		
		if(Oscar3Props.getInstance().verbose) System.out.println("nGrams initialised");
		//if(chemicalTest.size() > 0 && englishTest.size() > 0) optimiseAdjustment();
	}
	
	public void dumpData() {
		
	}
	
	/*public void dumpNGramData() throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		int a = alphabet.length();
		for(int i=0;i<a;i++) {
			for(int j=0;j<a;j++) {
				for(int k=0;k<a;k++) {
					for(int l=0;l<a;l++) {
						dos.writeDouble(LP4C[i][j][k][l]);
						dos.writeDouble(LP4E[i][j][k][l]);
					}
				}
			}
		}		
	}*/
	
	/**
	 * Read training data from plain text
	 */
	private void readTrainingData() {
		if(!extraOnly) {
			readStopWordsTrainingData();
			readExtractedTrainingData();
			readChemNameDictTrainingData();
			readElementsTrainingData();
			readUdwTrainingData();
			readAseTrainingData();			
		}
		readExtraTrainingData();
	}
	
	private void readCollection(Collection<String> coll, boolean isChemical) {
		if(coll == null) return;
		for(String s : coll) {
			String [] sa = s.split("\\s+");
			for(int i=0;i<sa.length;i++) {
				if(!sa[i].matches(".*[a-z][a-z].*")) continue;
				if(isChemical) {
					addChemical(sa[i]);
				} else {
					addEnglish(sa[i]);						
				}
			}
		} 
	}
		
	private void readStopWordsTrainingData() {
		readCollection(TermSets.getStopWords(), false);
		readCollection(ChemNameDictSingleton.getStopWords(), false);
	}

	private void readChemNameDictTrainingData() {
		try {
			readCollection(ChemNameDictSingleton.getAllNames(), true);			
		} catch (Exception e) {
			throw new Error();
		}
	}	
	
	private void readElementsTrainingData() {
			readCollection(TermSets.getElements(), true);
	}	
	
	private void readUdwTrainingData() {
		Set<String> goodUDW = new HashSet<String>();
		for(String word : TermSets.getUsrDictWords()) {
			if(!(ChemNameDictSingleton.hasName(word) || ExtractTrainingData.getInstance().chemicalWords.contains(word))) {
				goodUDW.add(word);
			}
		}
		readCollection(goodUDW, false);
	}
	
	private void readExtractedTrainingData() {
		ExtractTrainingData etd = ExtractTrainingData.getInstance();
		readCollection(etd.chemicalWords, true);
		readCollection(etd.nonChemicalWords, false);	
	}
	
	private void readAseTrainingData() {
		readCollection(TermSets.getChemAses(), true);
		readCollection(TermSets.getNonChemAses(), false);
	}
	
	private void readExtraTrainingData(){
		readCollection(extraChemical, true);
		readCollection(extraEnglish, false);
	}
	
	/**
	 * Calculate logP of quadrigram using modified Kneser-Ney smoothing
	 * (Chen & Goodman, 1999)
	 */
	private double[][][][] calcLP4
	(int[] C1, int[][] C2, int[][][] C3, int[][][][] C4) {
		// General constants
		int A = alphabet.length();	// Alphabet size
		
		// Unigrams ////////////////////////////////////////
		
		int N = 0;
		int T1 = 0;
		int Z1 = 0;
		for(int i=0;i<A;i++) {
			N += C1[i];
			if(C1[i]>0) {
				T1++;
			} else {
				Z1++;
			}
		}
		double[] P1 = new double[A];
		for(int i=0;i<A;i++) {
			if(C1[i]>0) {
				P1[i] = (1.0*C1[i])/(1.0*(N+T1));
			} else {
				P1[i] = (1.0*T1)/(1.0*Z1*(N+T1));
			}
		}
		
		
		
		// Bigrams ////////////////////////////////////////
		
		int n1=0;
		int n2=0;
		int n3=0;
		int n4=0;
		int[] N1 = new int[A];
		int[] N2 = new int[A];
		int[] N3plus = new int[A];
		
		for(int i=0;i<A;i++) {
			N1[i] = 0;
			N2[i] = 0;
			N3plus[i] = 0;
			for(int j=0;j<A;j++) {
				if(C2[i][j]==1) {
					N1[i]++;
					n1++;
				} else if(C2[i][j]==2) {
					N2[i]++;
					n2++;
				} else if(C2[i][j]==3) {
					N3plus[i]++;
					n3++;
				} else if(C2[i][j]==4) {
					N3plus[i]++;
					n4++;
				} else if(C2[i][j]>4) {
					N3plus[i]++;
				}
			}
		}
		
		double Y = (1.0*n1)/(1.0*(n1+2*n2));
		double D1 = 1 - 2*Y*n2/(1.0*n1);
		double D2 = 1 - 2*Y*n3/(1.0*n2);
		double D3plus = 1 - 2*Y*n4/(1.0*n3);
		
		double[] gamma = new double[A];
		for(int i=0;i<A;i++) {
			double sum = 0;
			for(int j=0;j<A;j++) {
				sum += C2[i][j];
			}
			gamma[i] = (D1*N1[i] + D2*N2[i] + D3plus*N3plus[i])/sum;
		}
		
		double[][] P2 = new double[A][A];
		for(int i=0;i<A;i++) {
			double sum = 0;
			double Pcum = 0;
			double D;
			for(int j=0;j<A;j++) {
				sum += C2[i][j];
			}
			if(sum>0) {
				for(int j=0;j<A;j++) {
					if(C2[i][j]==0) {
						D = 0;
					} else if(C2[i][j]==1) {
						D = D1;
					} else if(C2[i][j]==2) {
						D = D2;
					} else {
						D = D3plus;
					}
					P2[i][j] = ((C2[i][j] - D)/sum) + gamma[i]*P1[j];
					Pcum += P2[i][j];
					if((P2[i][j]<=0)|(P2[i][j]>=1)) {
						//System.out.println("\t\t***\t" + P2[i][j]);
					}
				}
			} else {
				for(int j=0;j<A;j++) {
					P2[i][j] = P1[j];
					Pcum += P2[i][j];
					if((P2[i][j]<=0)|(P2[i][j]>=1)) {
						//System.out.println("\t\t***\t" + P2[i][j]);
					}
				}
			}
		}
		
		
		// Trigrams ////////////////////////////////////////
		
		int Tn1=0;
		int Tn2=0;
		int Tn3=0;
		int Tn4=0;
		int[][] TN1 = new int[A][A];
		int[][] TN2 = new int[A][A];
		int[][] TN3plus = new int[A][A];
		
		for(int i=0;i<A;i++) {
			for(int j=0;j<A;j++) {
				TN1[i][j] = 0;
				TN2[i][j] = 0;
				TN3plus[i][j] = 0;
				for(int k=0;k<A;k++) {
					if(C3[i][j][k]==1) {
						TN1[i][j]++;
						Tn1++;
					} else if(C3[i][j][k]==2) {
						TN2[i][j]++;
						Tn2++;
					} else if(C3[i][j][k]==3) {
						TN3plus[i][j]++;
						Tn3++;
					} else if(C3[i][j][k]==4) {
						TN3plus[i][j]++;
						Tn4++;
					} else if(C3[i][j][k]>4) {
						TN3plus[i][j]++;
					}
				}
			}
		}
		
		double TY = (1.0*Tn1)/(1.0*(Tn1+2*Tn2));
		double TD1 = 1 - 2*TY*Tn2/(1.0*Tn1);
		double TD2 = 1 - 2*TY*Tn3/(1.0*Tn2);
		double TD3plus = 1 - 2*TY*Tn4/(1.0*Tn3);
		
		double[][] Tgamma = new double[A][A];
		for(int i=0;i<A;i++) {
			for(int j=0;j<A;j++) {
				double sum = 0;
				for(int k=0;k<A;k++) {
					sum += C3[i][j][k];
				}
				Tgamma[i][j] = (TD1*TN1[i][j] + TD2*TN2[i][j] + TD3plus*TN3plus[i][j])/sum;
			}
		}
		
		double[][][] P3 = new double[A][A][A];
		for(int i=0;i<A;i++) {
			for(int j=0;j<A;j++) {
				double sum = 0;
				double Pcum = 0;
				double D;
				for(int k=0;k<A;k++) {
					sum += C3[i][j][k];
				}
				if(sum>0) {
					for(int k=0;k<A;k++) {
						if(C3[i][j][k]==0) {
							D = 0;
						} else if(C3[i][j][k]==1) {
							D = TD1;
						} else if(C3[i][j][k]==2) {
							D = TD2;
						} else {
							D = TD3plus;
						}
						P3[i][j][k] = ((C3[i][j][k] - D)/sum) + Tgamma[i][j]*P2[j][k];
						Pcum += P3[i][j][k];
						if((P3[i][j][k]<=0)|(P3[i][j][k]>=1)) {
							//System.out.println("\t\t***\t" + P3[i][j][k]);
						}
					}
				} else {
					for(int k=0;k<A;k++) {
						P3[i][j][k] = P2[j][k];
						Pcum += P3[i][j][k];
						if((P3[i][j][k]<=0)|(P3[i][j][k]>=1)) {
							//System.out.println("\t\t***\t" + P3[i][j][k]);
						}
					}
				}
			}
		}
		
		
		
		// Quadrigrams /////////////////////////////////////
		
		int Qn1=0;
		int Qn2=0;
		int Qn3=0;
		int Qn4=0;
		int[][][] QN1 = new int[A][A][A];
		int[][][] QN2 = new int[A][A][A];
		int[][][] QN3plus = new int[A][A][A];
		
		for(int i=0;i<A;i++) {
			for(int j=0;j<A;j++) {
				for(int k=0;k<A;k++) {
					QN1[i][j][k] = 0;
					QN2[i][j][k] = 0;
					QN3plus[i][j][k] = 0;
					for(int l=0;l<A;l++) {
						if(C4[i][j][k][l]==1) {
							QN1[i][j][k]++;
							Qn1++;
						} else if(C4[i][j][k][l]==2) {
							QN2[i][j][k]++;
							Qn2++;
						} else if(C4[i][j][k][l]==3) {
							QN3plus[i][j][k]++;
							Qn3++;
						} else if(C4[i][j][k][l]==4) {
							QN3plus[i][j][k]++;
							Qn4++;
						} else if(C4[i][j][k][l]>4) {
							QN3plus[i][j][k]++;
						}
					}
				}
			}
		}
		
		double QY = (1.0*Qn1)/(1.0*(Qn1+2*Qn2));
		double QD1 = 1 - 2*QY*Qn2/(1.0*Qn1);
		double QD2 = 1 - 2*QY*Qn3/(1.0*Qn2);
		double QD3plus = 1 - 2*QY*Qn4/(1.0*Qn3);
		
		double[][][] Qgamma = new double[A][A][A];
		for(int i=0;i<A;i++) {
			for(int j=0;j<A;j++) {
				for(int k=0;k<A;k++) {
					double sum = 0;
					for(int l=0;l<A;l++) {
						sum += C4[i][j][k][l];
					}
					Qgamma[i][j][k] = (QD1*QN1[i][j][k] + QD2*QN2[i][j][k] + QD3plus*QN3plus[i][j][k])/sum;
				}
			}
		}
		
		double[][][][] P4 = new double[A][A][A][A];
		for(int i=0;i<A;i++) {
			for(int j=0;j<A;j++) {
				for(int k=0;k<A;k++) {
					double sum = 0;
					double Pcum = 0;
					double D;
					for(int l=0;l<A;l++) {
						sum += C4[i][j][k][l];
					}
					if(sum>0) {
						for(int l=0;l<A;l++) {
							if(C4[i][j][k][l]==0) {
								D = 0;
							} else if(C4[i][j][k][l]==1) {
								D = QD1;
							} else if(C4[i][j][k][l]==2) {
								D = QD2;
							} else {
								D = QD3plus;
							}
							P4[i][j][k][l] = ((C4[i][j][k][l] - D)/sum) + Qgamma[i][j][k]*P3[j][k][l];
							Pcum += P4[i][j][k][l];
							if((P4[i][j][k][l]<=0)|(P4[i][j][k][l]>=1)) {
								//System.out.println("\t\t***\t" + P4[i][j][k][l]);
							}
						}
					} else {
						for(int l=0;l<A;l++) {
							P4[i][j][k][l] = P3[j][k][l];
							Pcum += P4[i][j][k][l];
							if((P4[i][j][k][l]<=0)|(P4[i][j][k][l]>=1)) {
								//System.out.println("\t\t***\t" + P4[i][j][k][l]);
							}
						}
					}
				}
			}
		}
		
		for(int i=0;i<A;i++) {
			for(int j=0;j<A;j++) {
				for(int k=0;k<A;k++) {
					for(int l=0;l<A;l++) {
						P4[i][j][k][l] = Math.log(P4[i][j][k][l]);
					}
				}
			}
		}
		
		
		return P4;
	}    
	
	
	/**
	 * Test a word against training data.
	 * Returned score represents relative log probabilities of chemical vs
	 * english; i.e. scores > 0 are probably chemical.
	 * @param word String to be tested
	 * @return <PRE>ln(P(chemical|word)/P(english|word))</PRE>
	 */
	public double testWord(String word){
		//if(cache.containsKey(word)) {
		//	return cache.get(word);
		//}
		String w = parseWord(word);
		//if(chemSet.contains(w)) return 100.0;
		//if(engSet.contains(w)) return -100.0;
		int l = w.length();
		if(l<=1) {
			return 0;
		}
		w = addStartAndEnd(w);
		l = w.length();
		int s1 = alphabet.indexOf(w.charAt(0));
		int s2 = alphabet.indexOf(w.charAt(1));
		int s3 = alphabet.indexOf(w.charAt(2));
		int s0 = 0;
		double logP = 0;
		for(int i=3;i<l;i++) {
			s0 = s1;
			s1 = s2;
			s2 = s3;
			s3 = alphabet.indexOf(w.charAt(i));
			double score = LP4C[s0][s1][s2][s3] - LP4E[s0][s1][s2][s3];
			logP += score;
		}
		//if(!Token.suffixPattern.matcher(w.substring(1, w.length()-2)).matches()) logP = -100.0;
		//try {
		//	if(WordLists.getInstance().usrDictWords.contains(word)) logP -= 4; 
		//} catch (Exception e) {
		//	e.printStackTrace();
		//}
		//cache.put(word, logP);
		return logP;
	}
	
	public double testWordProb(String word){
		double score = testWord(word);
		double prior = chemSet.size() / (chemSet.size() + engSet.size() + 0.0);
		score = Math.log(prior) - Math.log(1-prior) + score;
		return Math.exp(score) / (1 + Math.exp(score));
	}
	
	public double testWordSuffixProb(String word) {
		String w = parseWord(word);
		double p = suffixClassifier.scoreWord(w);
		return p;
	}

	public double testWordSuffix(String word) {
		String w = parseWord(word);
		double p = suffixClassifier.scoreWord(w);
		return Math.log(p) - Math.log(1-p);
		//return Math.log(p)/Math.log(1-p);
	}
	
	public int lookupSuffixCount(String word, boolean chemical) {
		String w = parseWord(word);
		return suffixClassifier.lookupCount(w, chemical);
	}
	
	/** Add a chemical name to training frequency data */
	private void addChemical(String word) {
		//if(englishWords.contains(word)) return;
		chemWords.add(word);
	}    
	
	/** Add an english word to training frequency data */
	private void addEnglish(String word) {
		//if(chemWords.contains(word)) return;
		englishWords.add(word);	
	}
	
	private void addEngNGrams(String word) {
		addWordNGrams(word, E1C, E2C, E3C, E4C);
	}

	private void addChemNGrams(String word) {
		addWordNGrams(word, C1C, C2C, C3C, C4C);		
	}
	
	private void addWordNGrams(String word, int[] C1, int[][] C2, int[][][] C3, int [][][][] C4) {
		if(dontUse.contains(word)) return;
		String w = parseWord(word);
		//englishTest.add(w);
		int l = w.length();
		if(l<=1) {
			return;
		}
		w = addStartAndEnd(w);
		l = w.length();
		int s0 = 0;
		int s1 = 0;
		int s2 = 0;
		int s3 = 0;
		for(int i=0;i<l;i++) {
			if(i>2) {
				s0 = s1;
			}
			if(i>1) {
				s1 = s2;
			}
			if(i>0) {
				s2 = s3;
			}
			s3 = alphabet.indexOf(w.charAt(i));
			C1[s3]++;
			if(i>0) {
				C2[s2][s3]++;
				if(i>1) {
					C3[s1][s2][s3]++;
					if(i>2) {
						C4[s0][s1][s2][s3]++;
					}
				}
			}
		}		
	}

	/** Register new alphabet of valid characters */
	private void setAlphabet(String alphabet) {
		this.alphabet = alphabet;
	}
	
	
	/**
	 * Prepare a word for nGram parsing by lowercasing and tokenising symbols
	 * TODO should be improved, esp. wrt numbers
	 */
	public static String parseWord(String w) {
		String l = w.toLowerCase();
		l=searchReplace(l, "1","0"); 
		l=searchReplace(l, "2","0");
		l=searchReplace(l, "3","0");
		l=searchReplace(l, "4","0");
		l=searchReplace(l, "5","0");
		l=searchReplace(l, "6","0");
		l=searchReplace(l, "7","0");
		l=searchReplace(l, "8","0");
		l=searchReplace(l, "9","0");
		l=searchReplace(l, "00", "0");
		l=searchReplace(l, "00", "0");
		l=searchReplace(l, "00", "0");
		l=searchReplace(l, "\"", "'");
		l=searchReplace(l, "\u2013","-");
		l=searchReplace(l, "\u2014","-");
		l=searchReplace(l, "{", "[");
		l=searchReplace(l, "}", "]");
		for(int j=0;j<l.length();j++) {
			// replace unknown symbols with S (NB capital)
			if("$^S0%<>&'()*+,-./:;=?@[]abcdefghijklmnopqrstuvwxyz|~".indexOf(l.charAt(j))<0)
				l=searchReplace(l, l.substring(j,j+1), "S");
		}
		
		return l;
	}
	
	public static String addStartAndEnd(String s) {
		return "^^^" + s + "$";
	}
	
	
	//TODO this is insanity!
	/** Do a find and replace in supplied string */
	private static String searchReplace(String inString, String find, String replace) {
		if (inString==null || find == null || replace == null) {
			return inString;
		}
		if(inString.length() == 0) {
			return inString;
		}
		if(find.length() == 0) {
			return inString;
		}
		int i = inString.indexOf(find);
		if (i == -1) {
			return inString;
		} else {
			return inString.substring(0,i) + replace +
			searchReplace(inString.substring(i + find.length()), find, replace);
		}
	}
	
	public static void main(String[] args) throws Exception {
		NGram ng = getInstance();
		long time = System.currentTimeMillis();
		File f = File.createTempFile("foo", "bar");
		f.deleteOnExit();
		ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
		oos.writeObject(ng.LP4C);
		oos.writeObject(ng.LP4E);
		oos.close();
		System.out.println(System.currentTimeMillis() - time);
	}
	
}
