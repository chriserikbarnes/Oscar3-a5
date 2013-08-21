package uk.ac.cam.ch.wwmm.ptc.experimental.ngramtfdf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;

import org.tartarus.snowball.ext.EnglishStemmer;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class NGramTfDf {

	/* Mikio Yamamoto, Kenneth W. Church
	 * Using suffix arrays to compute term frequency and document frequency for all substrings in a corpus
	 * Computational Linguistics 2001, 1-30
	 */
	
	List<List<String>> documents;
	List<Suffix> suffixArray;
	List<Integer> lcp;
	List<SubstringClass> classArray;
	int totalTokens;
	Bag<String> tokens;
	
	private static boolean verbose = true;

	public static NGramTfDf analyseFiles(List<File> files) throws Exception {
		List<List<String>> docSet = new ArrayList<List<String>>();

		for(File f : files) {
			if(verbose) System.out.println(f);
			docSet.add(fileToDoc(f));
		}
		return new NGramTfDf(docSet);
	}
	
	public NGramTfDf(List<List<String>> documents) {
		this.documents = documents;
		tokens = new Bag<String>();
		for(List<String> document : documents) {
			for(String token : document) {
				tokens.add(token);
			}
		}
		
	}
	
	private void logClass(int i, int j, int k, int df) {
		int lbl = Math.max(lcp.get(i), lcp.get(j+1));
		int sil = lcp.get(k);
		if(i == j) {
			//if(verbose) System.out.println("trivial <" + i + "," + j + ">, tf = 1");
		} else if(lbl < sil) {
			SubstringClass sc = new SubstringClass(sil, lbl, (j-i+1), df, suffixArray.get(i).longestCommonPrefix(suffixArray.get(j)));
			classArray.add(sc);
		}

	}
	
	public void sortSuffixArray() {
		List<String> starts = new ArrayList<String>(tokens.getSet());
		Collections.sort(starts);
		Map<String,List<Suffix>> suffixesByStart = new HashMap<String,List<Suffix>>();
		for(String start : starts) {
			suffixesByStart.put(start, new ArrayList<Suffix>());
		}
		for(Suffix suffix : suffixArray) {
			suffixesByStart.get(suffix.getSuffix().get(0)).add(suffix);
		}
		suffixArray.clear();
		for(String start : starts) {
			List<Suffix> suffixes = suffixesByStart.get(start);
			Collections.sort(suffixes);
			suffixArray.addAll(suffixes);
		}
	}
	
	public void calculateNGrams() {
		totalTokens = 0;
		long time = System.currentTimeMillis();
		if(verbose) System.out.println("Building suffix array");
		suffixArray = new ArrayList<Suffix>();
		for(int i=0;i<documents.size();i++) {
			List<String> document = documents.get(i);
			int size = document.size();
			totalTokens += size;
			for(int j=0;j<size;j++) {
				suffixArray.add(new Suffix(i, document.subList(j, size)));
			}
		}
		if(verbose) System.out.println("Built suffix array " + (System.currentTimeMillis() - time));
		long ttime = System.currentTimeMillis();
		sortSuffixArray();
		//Collections.sort(suffixArray);
		//System.out.println("Sort takes " + (System.currentTimeMillis() - ttime));
		if(verbose) System.out.println("Sorted suffix array " + (System.currentTimeMillis() - time));
		lcp = new ArrayList<Integer>(suffixArray.size() + 1);
		for(int i=0;i<=suffixArray.size();i++) {
			if(i == 0 || i == suffixArray.size()) {
				lcp.add(0);
			} else {
				lcp.add(suffixArray.get(i).longestCommonPrefixLength(suffixArray.get(i-1)));
			}
		}
		if(verbose) System.out.println("Built LCP " + (System.currentTimeMillis() - time));
		
		Stack<Integer> stackI = new Stack<Integer>();
		Stack<Integer> stackK = new Stack<Integer>();
		Stack<Integer> stackDF = new Stack<Integer>();
		
		stackI.push(0);
		stackK.push(0);
		stackDF.push(1);
		
		List<Integer> docLink = new ArrayList<Integer>();
		for(int i=0;i<documents.size();i++) {
			docLink.add(-1);
		}
		
		classArray = new ArrayList<SubstringClass>();
		
		for(int j=0;j<suffixArray.size();j++) {
			logClass(j, j, 0, 1);
			int doc = suffixArray.get(j).getDocID();
			if(docLink.get(doc) != -1) {
				int dld = docLink.get(doc);
				int beg = 0;
				int end = stackI.size();
				int mid = end / 2;
				while(beg != mid) {
					if(dld >= stackI.get(mid)) {
						beg = mid;
					} else {
						end = mid;
					}
					mid = (beg + end) / 2;
				}
				stackDF.set(mid, stackDF.get(mid)-1);
			}
			docLink.set(doc, j);
			int df = 1;
			while(lcp.get(j+1) < lcp.get(stackK.peek())) {
				df = stackDF.peek() + df;
				logClass(stackI.peek(), j, stackK.peek(), df);
				stackI.pop();
				stackK.pop();
				stackDF.pop();
			}
			stackI.push(stackK.peek());
			stackK.push(j+1);
			stackDF.push(df);
		}

		if(verbose) System.out.println("Analysed " + (System.currentTimeMillis() - time));
		
		//Collections.sort(classArray);	
	}
	
	public void printClassArray() {
		for(SubstringClass sc : classArray) {
			System.out.println(sc);
		}
	}
	
	public void printRIDFs() {
		Map<String,Double> ridfs = new HashMap<String,Double>();
		for(SubstringClass sc : classArray) {
			String lss = sc.getLongestSuffixString();
			ridfs.put(lss, sc.ridf(documents.size()));
		}
		for(String s : StringTools.getSortedList(ridfs)) {
			//if(s.matches(".*cyp.*")) System.out.println(s + "\t" + ridfs.get(s));
			System.out.println(s + "\t" + ridfs.get(s));
		}
	}
		
	public void printMIs() {
		Bag<String> freqs = new Bag<String>();
		for(SubstringClass sc : classArray) {
			String s = sc.getLongestSuffixString();
			freqs.add(s, sc.getTf());
		}
		Map<String,Double> mis = new HashMap<String,Double>();
		for(String s : freqs.getSet()) {
			if(!checkTerm(s)) continue;
			List<String> words = StringTools.arrayToList(s.split("\\s+"));
			if(words.size() > 2) {
				//System.out.println(s + "\t" + words.size());
				int coreFreq;
				int totalFreq = freqs.getCount(s);
				int leftFreq;
				int rightFreq;
				if(words.size() == 2) {
					leftFreq = freqs.getCount(words.get(0));
					if(leftFreq == 0) leftFreq = totalFreq;
					rightFreq = freqs.getCount(words.get(1));
					coreFreq = totalTokens;
				} else if(words.size() == 3) {
					rightFreq = freqs.getCount(StringTools.collectionToString(words.subList(1, 3), " "));
					coreFreq = freqs.getCount(words.get(1));
					if(coreFreq == 0) coreFreq = rightFreq;
					leftFreq = freqs.getCount(StringTools.collectionToString(words.subList(0, 2), " "));
					if(leftFreq == 0) leftFreq = totalFreq;
				} else {
					rightFreq = freqs.getCount(StringTools.collectionToString(words.subList(1, words.size()), " "));
					coreFreq = freqs.getCount(StringTools.collectionToString(words.subList(1, words.size()-1), " "));
					if(coreFreq == 0) coreFreq = rightFreq;
					leftFreq = freqs.getCount(StringTools.collectionToString(words.subList(0, words.size()-1), " "));
					if(leftFreq == 0) leftFreq = totalFreq;					
				}
				double mi = Math.log((totalFreq * totalFreq * 1.0) / (leftFreq * rightFreq));
				//double mi = Math.log((totalFreq * coreFreq * 1.0) / (leftFreq * rightFreq));
				mis.put(s, mi);
				/*int freq = freqs.getCount(s);
				int prevFrontFreq = freq;
				double bestScore = Double.MAX_VALUE;
				for(int i=words.size()-1;i>0;i--) {
					String front = StringTools.collectionToString(words.subList(0,i), " ");
					String back = StringTools.collectionToString(words.subList(i,words.size()), " ");
					int frontFreq = freqs.getCount(front);
					if(frontFreq == 0) frontFreq = prevFrontFreq;
					int backFreq = freqs.getCount(back);
					double score = words.size() * Math.log(freq) * ((freq-1) * (freq-1) * 1.0) / ((frontFreq - 1.0) * (backFreq - 1.0));
					if(score < bestScore) bestScore = score;
				}
				mis.put(s, bestScore);*/
				/*double expected = totalTokens;
				for(int i=0;i<words.size();i++) {
					int wordFreq = freqs.getCount(words.get(i));
					int segLen = 1;
					while(wordFreq == 0) {
						segLen++;
						wordFreq = freqs.getCount(StringTools.collectionToString(words.subList(i, i+segLen), " "));
					}
					expected *= wordFreq;
					expected /= totalTokens;
				}
				int observed = freqs.getCount(s);
				mis.put(s, 2 * observed * Math.log(observed/expected));*/
			}
		}
		for(String s : StringTools.getSortedList(mis)) {
			System.out.println(s + "\t" + mis.get(s));
		}
	}
	
	public void printUnigrams() {
		Map<String,Integer> ug = new HashMap<String,Integer>();
		for(SubstringClass sc : classArray) {
			String lss = sc.getLongestSuffixString();
			if(lss.contains(" ")) continue;
			ug.put(lss, sc.getTf());
		}
		List<String> results = StringTools.getSortedList(ug); 
		for(String s : results) {
			System.out.println(s + "\t" + ug.get(s));
		}

	
	}
	
	public void printCVals() {
		long time = System.currentTimeMillis();
		Map<String,Double> cvals = new HashMap<String,Double>();
		for(SubstringClass sc : classArray) {
			String lss = sc.getLongestSuffixString();
			//if(!lss.matches(".* \\( \\S+ \\)")) continue;
			double rcv = sc.rawCVal();
			if(rcv > 0.0) cvals.put(lss, sc.rawCVal());
		}
		List<String> results = StringTools.getSortedList(cvals); 
		System.out.println(System.currentTimeMillis() - time);
		int totLen = 0;
		int count = 0;
		for(String s : results) {
			//if(!s.matches("(.* (drugs?|agents?|compounds?)|\\S+s)")) continue;
			//if(!s.matches(".* \\( \\S+ \\)")) continue;
			/*if(s.matches(".* \\. .*")) continue;
			if(s.matches(".* \\, .*")) continue;
			if(s.matches("\\. .*")) continue;
			if(s.matches(".* \\.")) continue;
			if(s.matches("\\, .*")) continue;
			if(s.matches(".* \\,")) continue;
			if(s.matches("(.* )?(has|have|had|may|be|are|am|were|the|was|than|which|and|or|in a|" +
					"|suggests?|that|this|these|those|is|by|did|do|not|also|indicates?|" +
					"been|shown?|we|found to|of|in|to|with|as|from|at|other|may|" +
					"can|no|it|its|only|their|about|various|both|" +
					"\\(|\\))( .*)?")) continue;
			if(s.matches("(-|:|;|/|=|<|>|in|an?|of|for|no|to|from|upon|with|at|" +
					"as|on|its|via|but|\\d+(\\.\\d+)?%?|" +
					"(metabolism|inhibition|production|formation|levels?) of) .*")) continue;
			if(s.matches(".* (after|an?|of|for|no|to|in|;|:|/|=|<|>|-|from|upon|" +
					"with|as|at|on|its|via|but|levels?)")) continue;
			if(!StringTools.bracketsAreBalanced(s)) continue;*/
			if(!checkTerm(s)) continue;
			totLen += s.length();
			System.out.println(s + "\t" + cvals.get(s));
			count++;
		}
		System.out.println("Total 2+Gram length: " + totLen);
		System.out.println("Total terms: " + count);
	}
	
	public void printPMs() {
		double perplexity = tokens.perplexity();
		double pscore = perplexity / tokens.totalCount();
		
		long time = System.currentTimeMillis();
		Map<String,Double> cvals = new HashMap<String,Double>();
		for(SubstringClass sc : classArray) {
			if(sc.getLongestSuffix().getSuffix().size() == 1) continue;
			double score = sc.getTf();
			for(String s : sc.getLongestSuffix().getSuffix()) {
				int wcount = tokens.getCount(s);
				//score /= wcount;
				score /= wcount * pscore;
			}
			String lss = sc.getLongestSuffixString();
			//if(!lss.matches(".* \\( \\S+ \\)")) continue;
			cvals.put(lss, Math.log(score) / sc.getTf());
		}
		List<String> results = StringTools.getSortedList(cvals); 
		System.out.println(System.currentTimeMillis() - time);
		int totLen = 0;
		int count = 0;
		for(String s : results) {

			totLen += s.length();
			System.out.println(s + "\t" + cvals.get(s));
			count++;
		}
		System.out.println("Total 2+Gram length: " + totLen);
		System.out.println("Total terms: " + count);
	}

	public static boolean checkTerm(String s) {
		//if(true) return s.matches("\\S+ \\.");
		//if(true) return s.matches(".* \\.");
		
		s = s.toLowerCase();
		if(s.matches(".* \\. .*") && !s.matches("[Ss]t \\. .*")) return false;
		if(s.matches(".* \\, .*")) return false;
		if(s.matches("\\. .*")) return false;
		if(s.matches(".* \\.")) return false;
		if(s.matches("\\, .*")) return false;
		if(s.matches(".* \\,")) return false;
		if(s.matches("(.* )?(has|have|had|may|be|are|am|were|was|than|which|and|or|in a|" +
				"|suggests?|that|this|these|those|is|by|did|do|not|also|indicates?|" +
				"been|shown?|we|found to|in|to|with|as|from|at|other|may|" +
				"can|no|it|its|only|their|about|various|both|into|during|whereas" +
				"\\(|\\))( .*)?")) return false;
		if(s.matches("(\\.|-|:|;|/|=|<|>|in|an?|of|for|no|to|from|upon|with|at|the" +
				"|as|on|its|via|but|\\d+(\\.\\d+)?%?" +
				//"|(metabolism|inhibition|production|formation|levels?) of" +
				") .*")) return false;
		if(s.matches(".* (the|after|an?|of|for|no|to|in|;|:|/|=|<|>|-|\\.|from|upon" +
				"|with|as|at|on|its|via|but|levels?)")) return false;
		if(!StringTools.bracketsAreBalanced(s)) return false;
		return true;
	}
	
	public Bag<String> getTfBag() {
		Bag<String> results = new Bag<String>();
		for(SubstringClass sc : classArray) {
			String s = sc.getLongestSuffixString();
			if(!checkTerm(s)) continue;
			results.add(s.intern(), sc.getTf());
		}
		return results;
	}
	
	public Bag<String> getDfBag(int minLen) {
		Bag<String> results = new Bag<String>();
		for(SubstringClass sc : classArray) {
			int df = sc.getDf();
			for(String s : sc.getSuffixStrings(minLen)) {
				if(!checkTerm(s)) continue;
				if(results.getCount(s) < df) {
					results.set(s.intern(), df);					
				}
			}
		}
		return results;
	}
	
	public static List<String> stringToDoc(String s) {
		List<String> results = new ArrayList<String>();
		for(String word : StringTools.arrayToList(s.split("\\s+"))) {
			results.add(word.toLowerCase().intern());
		}
		return results;
	}

	public static List<String> fileToDoc(File f) throws Exception {
		List<String> results = new ArrayList<String>();
		
		Document doc = new Builder().build(f);
		
		Nodes n = XMLStrings.getInstance().getChemicalPlaces(doc);
		for(int i=0;i<n.size();i++) {
			String s = n.get(i).getValue();
			if(s != null) {
				TokenSequence t = Tokeniser.getInstance().tokenise(s);
				for(String word : t.getTokenStringList()) {
					results.add(StringTools.normaliseName2(word).intern());
				}				
			}
		}
		
		return results;
	}
	
	public Map<String,List<String>> ngramsByStem() {
		Stemmer st = new Stemmer(new EnglishStemmer());
		Set<String> terms = new HashSet<String>();
		for(SubstringClass sc : classArray) {
			for(String s : sc.getSuffixStrings(2)) {
				if(!checkTerm(s)) continue;
				terms.add(s);
			}
		}
		Map<String,List<String>> stems = new HashMap<String,List<String>>();
		for(String term : terms) {
			String stem = st.getStem(term.replaceAll(" - ", " "));
			if(!stems.containsKey(stem)) stems.put(stem, new ArrayList<String>());
			stems.get(stem).add(term);
		}
		return stems;
	}
		
	public static void main(String[] args) throws Exception {
		
		//if(true) return;
		List<File> files = new ArrayList<File>();
		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/BioIE"), "source.xml"));
		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/paperset1"), "source.xml"));
		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/allGrapefruit"), "source.xml"));
		
		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/newEnzyme"), "source.xml"));
		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/enzyme"), "source.xml"));

		NGramTfDf ngtd = analyseFiles(files);
		ngtd.calculateNGrams();		
		ngtd.printCVals();
		/*Bag<String> bag = ngtd.getTfBag();
		for(String s : bag.getList()) {
			System.out.println(s);
		}*/

	}

}
