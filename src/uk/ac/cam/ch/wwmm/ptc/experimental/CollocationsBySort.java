package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.distribution.ChiSquaredDistribution;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.commons.math.stat.inference.ChiSquareTest;
import org.apache.commons.math.stat.inference.ChiSquareTestImpl;
import org.tartarus.snowball.ext.PorterStemmer;

import nu.xom.Builder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.HyphenTokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequenceSource;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.TLRHolder;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermMaps;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptc.experimental.ngramtfdf.NewNGramTfDf;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class CollocationsBySort {

	List<String> tokenList = new ArrayList<String>();
	Map<String,Integer> tokenIndex = new HashMap<String,Integer>();
	List<Long> collocs;
	Bag<Integer> tCounts;

	public static double gTest(int a, int b, int c, int d) {
		// a b
		// c d
		
		double n = a + b + c + d;
		double ea = (a+b) * (a+c) / n;
		double eb = (a+b) * (b+d) / n;
		double ec = (c+d) * (a+c) / n;
		double ed = (c+d) * (b+d) / n;
		double g = 0.0;
		if(a > 0) g += 2 * a * Math.log(a / ea);
		if(b > 0) g += 2 * b * Math.log(b / eb);
		if(c > 0) g += 2 * c * Math.log(c / ec);
		if(d > 0) g += 2 * d * Math.log(d / ed);
		return g;
	}
	
	public static long encodeCollocation(int c1, int c2) {
		if(c1 > c2) return encodeCollocation(c2, c1);
		return c1 + ((long)c2 << 32);
	}
	
	public static int[] decodeCollocation(Long l) {
		int [] results = new int[2];
		results[0] = (int)(l >> 32);
		results[1] = (int)(l - ((long)results[0] << 32));
		return results;
	}
	
	public CollocationsBySort() throws Exception {
		collocs = new ArrayList<Long>();
		tCounts = new Bag<Integer>();
	}
	
	public void randomCorpus(List<File> files, int num, int seed) throws Exception {
		List<Integer> sizes = new ArrayList<Integer>();
		List<Integer> corpus = new ArrayList<Integer>();
		long time = System.currentTimeMillis();
		for(File f : files) {
			ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(new Builder().build(f), false, false, false);
			int size = 0;
			for(TokenSequence ts : procDoc.getTokenSequences()) {
				if(ts.size() == 0) continue;
				for(Token t : ts.getTokens()) {
					size++;
					String s = t.getValue().toLowerCase().intern();
					int tn = -1;
					if(tokenIndex.containsKey(s)) {
						tn = tokenIndex.get(s);
					} else {
						tn = tokenList.size();
						tokenList.add(s);
						tokenIndex.put(s, tn);
					}
					corpus.add(tn);
				}
			}
			sizes.add(size);
		}
		//System.out.println(System.currentTimeMillis() - time);
		time = System.currentTimeMillis();
		Random r = new Random(seed);
		for(int k=0;k<num;k++) {
			int size = sizes.get(r.nextInt(sizes.size()));
			Set<Integer> tokSet = new HashSet<Integer>();
			for(int j=0;j<size;j++) {
				int tn = corpus.get(r.nextInt(corpus.size()));
				tokSet.add(tn);
			}
			List<Integer> ll = new ArrayList<Integer>(tokSet);
			for(int i=0;i<ll.size()-1;i++) {
				for(int j=i+1;j<ll.size();j++) {
					collocs.add(encodeCollocation(ll.get(i), ll.get(j)));
				}
			}
			for(Integer i : ll) tCounts.add(i);
		}
		//System.out.println(System.currentTimeMillis() - time);
	}
	
	public void run(List<File> files) throws Exception {
		
		Stemmer st = new Stemmer(new PorterStemmer());
		
		long time = System.currentTimeMillis();
		for(File f : files) {
			ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(new Builder().build(f), false, false, false);
			Set<Integer> tokSet = new HashSet<Integer>();
			for(TokenSequence ts : procDoc.getTokenSequences()) {
				if(ts.size() == 0) continue;
				for(Token t : ts.getTokens()) {
					String s = t.getValue().toLowerCase().intern();
					if(s.matches(".*[a-z].*")) s = st.getStem(s);
					if(s == null || s.length() == 0) continue;
					int tn = -1;
					if(tokenIndex.containsKey(s)) {
						tn = tokenIndex.get(s);
					} else {
						tn = tokenList.size();
						tokenList.add(s);
						tokenIndex.put(s, tn);
					}
					tokSet.add(tn);
				}
			}
			//System.out.println(tokSet.size());
			List<Integer> ll = new ArrayList<Integer>(tokSet);
			for(int i=0;i<ll.size()-1;i++) {
				for(int j=i+1;j<ll.size();j++) {
					collocs.add(encodeCollocation(ll.get(i), ll.get(j)));
					//rcBag.add(encodeCollocation(ll.get(i), ll.get(j)));
				}
			}
			for(Integer i : ll) tCounts.add(i);
			//System.out.println("cs: " + collocs.size());
		}
		//System.out.println(System.currentTimeMillis() - time);
		if(collocs.size() == 0) return;

	}
	
	public void acceptInput(List<Set<Integer>> docs, List<String> terms) {
		tokenList = terms;
		for(int i=0;i<tokenList.size();i++) {
			tokenIndex.put(tokenList.get(i), i);
		}
		for(Set<Integer> tokSet : docs) {
			List<Integer> ll = new ArrayList<Integer>(tokSet);
			for(int i=0;i<ll.size()-1;i++) {
				for(int j=i+1;j<ll.size();j++) {
					collocs.add(encodeCollocation(ll.get(i), ll.get(j)));
				}
			}
			for(Integer i : ll) tCounts.add(i);
		}
	}
	
	public void finishAnalysis(int filenum) throws Exception {
		double fdr = 0.05;
		
		long time = System.currentTimeMillis();
		List<Integer> tList = tCounts.getList();
		Map<Integer,Integer> wordRank = new HashMap<Integer,Integer>();
		for(int i=0;i<tList.size();i++) {
			wordRank.put(tList.get(i), i+1);
		}
		for(int i=tList.size()-1;i>0;i--) {
			if(tCounts.getCount(tList.get(i)) == tCounts.getCount(tList.get(i-1))) wordRank.put(tList.get(i-1), wordRank.get(tList.get(i)));
		}
		int toksAboveThresh = 0;
		for(int i=tList.size()-1;i>0;i--) {
			if(tCounts.getCount(tList.get(i)) > 2) {
				toksAboveThresh = i+1;
				break;
			}
		}
		//System.out.println("3 or more occurrences: " + toksAboveThresh);
		//for(int i=0;i<tList.size();i++) {
		//	int t = tList.get(i);
		//	System.out.println(tokenList.get(t) + "\t" + wordRank.get(t) + "\t" + tCounts.getCount(t));
		//}
		//System.out.println(collocs.size());
		
		time = System.currentTimeMillis();
		Collections.sort(collocs);
		collocs.add((long)-2);
		//System.out.println(System.currentTimeMillis() - time);
		//if(true) return;
		long colloc = -1;
		int count = 0;
		Bag<String> cBag = new Bag<String>();
		Map<String,Double> excesses = new HashMap<String,Double>();
		time = System.currentTimeMillis();
		int ccount = 0;
		int hypotheses = ((toksAboveThresh-1) * (toksAboveThresh-2)) / 2;

		ChiSquaredDistribution csd = new ChiSquaredDistributionImpl(1);
		double upperThreshold = csd.inverseCumulativeProbability(1.0 - (fdr / hypotheses));
		double lowerThreshold = csd.inverseCumulativeProbability(1.0 - fdr);
		Map<String,String> reports = new HashMap<String,String>();
		int totalViable = 0;
		for(Long c : collocs) {
			if(c == colloc) {
				count++;
			} else {
				if(colloc != -1) {
					int [] r = decodeCollocation(colloc);
					int c1 = tCounts.getCount(r[0]);
					int c2 = tCounts.getCount(r[1]);
					//if(count > 2) {
					if(c1 > 2 && c2 > 2) {
						String s = tokenList.get(r[0]) + " " + tokenList.get(r[1]);
						//cBag.add(s, count);
						double expected = tCounts.getCount(r[0]) * tCounts.getCount(r[1]) * 1.0 / filenum;
						if(count > expected) totalViable++;

						//excesses.put(s, count - expected);
						//long [][] counts = new long[][]{{count, c1-count}, {c2-count, files.size()-c1-c2+count}};
						double cs = gTest(count, c1-count, c2-count, filenum-c1-c2+count);
						if(cs > lowerThreshold) {
							//double cs = cst.chiSquare(counts);
							//double p = csd.cumulativeProbability(cs);
							//System.out.println(cs);
							double pp;
							if(cs > upperThreshold) {
								pp = 0.0;
							} else {
								try {
									pp = 1.0 - csd.cumulativeProbability(cs);								
								} catch (MaxIterationsExceededException e) {
									System.out.println(cs);
									pp = 0.0;
								}								
							}
							//double pp = 1.0 - csd.cumulativeProbability(cs);
							if(Double.isNaN(pp)) System.out.println("NaN: " + s);
							if(!Double.isNaN(pp) && count > expected) {
								int r1 = wordRank.get(r[0]);
								int r2 = wordRank.get(r[1]);
								if(r1 > r2) {
									int tmp = r1;
									r1 = r2;
									r2 = tmp;
								}
								int hypno = (r1 * r2) - ((r1 * (r1 + 1))/2);
								//double score = -((1.0 - p) * hypno);
								double score = -(pp * hypno);
								double newp = 1.0 + score;
								if(pp < fdr) {
									reports.put(s, count + "\t" + (c1-count) + "\t" + (c2-count) + "\t" + (filenum-c1-c2+count) + "\t" + hypno + "\t" + pp);
									//excesses.put(s, count-expected);
									excesses.put(s, pp);
								}
							}
						}
					}
					ccount++;
				}
				if(c != -2) {
					colloc = c;
					count = 1;
				}
			}
		}
		//System.out.println(System.currentTimeMillis() - time);
	
		//System.out.println(collocs.size() + "\t" + ccount);
		
		List<String> rawResList = StringTools.getSortedList(excesses);
		Collections.reverse(rawResList);
		//hypotheses = 1; // If these are included in the scores...
		//hypotheses = totalViable;
		int accepted = 0;
		List<String> acceptedResults = new ArrayList<String>();
		for(int i=0;i<rawResList.size();i++) {
			String s = rawResList.get(i);
			double p = excesses.get(rawResList.get(i));
			if((p/(i+1)) * hypotheses > fdr && accepted == 0) {
				System.out.println("Accepted: " + (i) + " from " + rawResList.size() + " out of " + totalViable + " out of " + hypotheses + " hypotheses");
				accepted = i;
				//System.out.println(s + "\t" + p + "\t" + ((p/(i+1)) * hypotheses) + "\t" + reports.get(s));
				break;
			}
			acceptedResults.add(s);
			String [] ss = s.split(" ");
			System.out.println(s + "\t" + p + "\t" + ((p/(i+1)) * hypotheses) + "\t" + reports.get(s));
			acceptedResults.add(ss[1] + " " + ss[0]);
		}
		Collections.sort(acceptedResults);
		for(String s : acceptedResults) {
			System.out.println(s);
		}
		
		if(true) return;
		time = System.currentTimeMillis();
		List<String> resList = StringTools.getSortedList(excesses);
		System.out.println(System.currentTimeMillis() - time);
		Bag<String> parts = new Bag<String>();
		Map<String,Double> partScores = new HashMap<String,Double>();
		for(String s : resList) {
			String [] ss = s.split(" ");
			for(int i=0;i<2;i++) {
				if(!partScores.containsKey(ss[i])) partScores.put(ss[i], 0.0);
				partScores.put(ss[i], partScores.get(ss[i]) + excesses.get(s));
				parts.add(ss[i]);
			}
			//parts.add(ss[0]);
			//parts.add(ss[1]);
			System.out.println(s + "\t" + excesses.get(s) + "\t" + reports.get(s));
		}
		
		System.out.println(excesses.size());
		//Map<String,Double> partScores = new HashMap<String,Double>();
		for(String s : parts.getList()) {
			//partScores.put(s, parts.getCount(s) * 1.0 / tCounts.getCount(tokenIndex.get(s)));
			System.out.println(s + "\t" + parts.getCount(s) + "\t" + tCounts.getCount(tokenIndex.get(s)) + "\t" + partScores.get(s));
		}
		System.out.println();
		for(String s : StringTools.getSortedList(partScores)) {
			//partScores.put(s, parts.getCount(s) * 1.0 / tCounts.getCount(tokenIndex.get(s)));
			System.out.println(s + "\t" + parts.getCount(s) + "\t" + tCounts.getCount(tokenIndex.get(s)) + "\t" + partScores.get(s));
		}
		//for(String s : cBag.getList()) {
		//	System.out.println(s + "\t" + cBag.getCount(s));
		//}
	}
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if(false) {
			ChiSquaredDistribution csd = new ChiSquaredDistributionImpl(1);
			System.out.println(csd.inverseCumulativeProbability(1.0 - (0.05 / 1000000000)));
			System.out.println(csd.inverseCumulativeProbability(0.05));
			return;
		}
		if(false) {
			System.out.println(3 << 16);
			int [] r = decodeCollocation(encodeCollocation(1000, 10000));
			System.out.println(r[0] + "\t" + r[1]);
			
			ChiSquaredDistribution csd = new ChiSquaredDistributionImpl(3);
			for(int i=0;i<50;i++) {
				double p = Math.pow(10.0, -i);
				System.out.println(p + "\t" + csd.inverseCumulativeProbability(p));
			}
			
			return;
		}
		
		List<File> files = new ArrayList<File>();
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/newEnzyme"), "source.xml"));
		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12"), "source.xml"));
		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/allGrapefruit"), "source.xml"));
		//files = files.subList(0, 50);
		//System.out.println(files.size());
		HyphenTokeniser.init();
		TermMaps.init();
		TermSets.init();
		TLRHolder.getInstance();
		System.out.println("Init...");
		
		if(true) {
			CollocationsBySort is = new CollocationsBySort();
			System.out.println(files.size());
			NewNGramTfDf nngtfdf = new NewNGramTfDf();
			nngtfdf.run(files);
			List<String> t = nngtfdf.extractedTerms;
			List<Set<Integer>> d = nngtfdf.docContents;
			nngtfdf = null;
			is.acceptInput(d, t);
			//is.run(files);
			//is.randomCorpus(files, 100, i);
			is.finishAnalysis(files.size());			
		} else {
			for(int i=0;i<100;i++) {
				CollocationsBySort is = new CollocationsBySort();
				Random r = new Random(i);
				Collections.shuffle(files, r);
				is.run(files.subList(0, 500));
				//is.randomCorpus(files, 100, i);
				is.finishAnalysis(500);			
			}
		}
		
	}

}
