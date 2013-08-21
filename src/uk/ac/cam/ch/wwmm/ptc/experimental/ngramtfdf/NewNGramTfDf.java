package uk.ac.cam.ch.wwmm.ptc.experimental.ngramtfdf;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import nu.xom.Builder;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class NewNGramTfDf {

	List<Integer> corpusArray = new ArrayList<Integer>();
	List<Integer> offsetArray = new ArrayList<Integer>();
	List<String> tokenList = new ArrayList<String>();
	Map<String,Integer> tokenIndex = new HashMap<String,Integer>();
	List<Integer> lcp;
	int numdocs;
	List<NewSubstringClass> classArray;

	public List<String> extractedTerms;
	public List<Set<Integer>> docContents;

	
	public void run(List<File> files) throws Exception {
		corpusArray = new ArrayList<Integer>();
		offsetArray = new ArrayList<Integer>();
		tokenList = new ArrayList<String>();
		tokenIndex = new HashMap<String,Integer>();
		
		numdocs = 0;
		for(File f : files) {
			ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(new Builder().build(f), false, false, false);
			for(TokenSequence ts : procDoc.getTokenSequences()) {
				if(ts.size() == 0) continue;
				for(Token t : ts.getTokens()) {
					String s = t.getValue().toLowerCase().intern();
					int tn = -1;
					if(tokenIndex.containsKey(s)) {
						tn = tokenIndex.get(s);
					} else {
						tn = tokenList.size();
						tokenList.add(s);
						tokenIndex.put(s, tn);
					}
					offsetArray.add(corpusArray.size());
					corpusArray.add(tn);
				}
				corpusArray.add(-1);
			}
			corpusArray.add(-2 - numdocs);
			numdocs++;
		}
		
		Comparator<Integer> offsetComparator = new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				int i1 = o1;
				int i2 = o2;
				while(true) {
					int c1 = corpusArray.get(i1);
					int c2 = corpusArray.get(i2);
					if(c1 < 0) {
						if(c2 < 0) return 0;
						return -1;
					}
					if(c2 < 0) return 1;
					int cd = c1 - c2;
					if(cd != 0) {
						return cd;
					}
					i1++;
					i2++;
				}
			}
		};
		
		long time=System.currentTimeMillis();
		Collections.sort(offsetArray, offsetComparator);
		System.out.println(System.currentTimeMillis() - time);
		//time=System.currentTimeMillis();
		calculateNGrams();
		//System.out.println(System.currentTimeMillis() - time);
		int goodSets = 0;

		docContents = new ArrayList<Set<Integer>>();
		for(int i=0;i<numdocs;i++) docContents.add(new HashSet<Integer>());
		extractedTerms = new ArrayList<String>();
		
		Map<String,Integer> tfs = new HashMap<String,Integer>();
		
		for(NewSubstringClass nsc : classArray) {
			if(nsc.getDf() < 3) continue;
			int sil = nsc.getSil();
			int fo = nsc.getFo();
			int offset = offsetArray.get(fo);
			StringBuilder sb = new StringBuilder();
			for(int i=0;i<sil;i++) {
				if(i>0) sb.append(" ");
				sb.append(tokenList.get(corpusArray.get(i + offset)));					
			}
			String nGram = sb.toString();
			if(!NGramTfDf.checkTerm(nGram)) continue;
			nGram = nGram.replaceAll(" ", "_");
			//for(int i=0;i<sil;i++) {
			//	System.out.print(tokenList.get(corpusArray.get(i + offset)) + " ");					
			//}
			//System.out.println("\t" + nsc.getTf() + "\t" + nsc.getDf());
			System.out.println(nGram);
			Set<Integer> is = new TreeSet<Integer>();
			for(int i=fo;i<fo+nsc.getTf();i++) {
				is.add(getDocID(offsetArray.get(i)));
			}
			System.out.println(is);
			//printConcordance(fo, nsc.getTf(), sil);
			System.out.println();
			//if(is.size() > 2) {
				extractedTerms.add(nGram);
				tfs.put(nGram, nsc.getTf());
				for(Integer i : is) {
					docContents.get(i).add(goodSets);
				}
				goodSets++;
			//}
		}
		System.out.println(classArray.size() + " " + goodSets);
		System.out.println(numdocs);
		for(Set<Integer> doc : docContents) {
			System.out.println(doc.size());
			//for(Integer i : doc) {
			//	System.out.println(strings.get(i));
			//}
			//System.out.println();
		}
		
		Collections.sort(extractedTerms, new Comparator<String>() {
			public int compare(String o1, String o2) {
				int c1 = 0;
				for(int i=0;i<o1.length();i++) {
					if(o1.charAt(i) == '_') c1++;
				}
				int c2 = 0;
				for(int i=0;i<o2.length();i++) {
					if(o2.charAt(i) == '_') c2++;
				}
				return c2 - c1;
			}
		});

		Map<String,Integer> timesInLongerString = new HashMap<String,Integer>();
		Map<String,Integer> freqInLongerString = new HashMap<String,Integer>();

		Map<String,Double> cValues = new HashMap<String,Double>();
		int accepted = 0;
		int rejected = 0;
		for(String c : extractedTerms) {
			//System.out.println(c + "\t" + tfs.get(c));
			int len = c.split("_").length;
			if(len < 2) break;
			int freq = tfs.get(c);
			int freqInLong = 0;
			double cVal;
			/*if(!bags.containsKey(c)) {
				cVal = Math.log(len) / Math.log(2) * freq;
			} else {
				double entropy = bags.get(c).entropy();
				double perplexity = Math.pow(2.0, entropy);
				freqInLong = bags.get(c).totalCount();
				cVal = Math.log(len) / Math.log(2) * (freq - (freqInLong/perplexity));
			}*/
			int timesInLong = 0;
			if(timesInLongerString.containsKey(c)) {
				timesInLong = timesInLongerString.get(c);
				freqInLong = freqInLongerString.get(c);
			}
			if(timesInLong == 0) {
				cVal = Math.log(len) / Math.log(2) * freq;
			} else {
				cVal = Math.log(len) / Math.log(2) * (freq - ((double)freqInLong/timesInLong));
				//System.out.println("Hurrah!\t" + cVal + "\t" + Math.log(len) / Math.log(2) * freq);
			}				
			
			if(cVal < 3.0)  {
				System.out.println(c + "\t" + cVal);
				rejected++;
				continue; 
			} else {
				accepted++;
			}
			
			cValues.put(c, cVal);

			//if(collCounts.getCount(c) <= 1) continue;
			int effectiveCount = tfs.get(c) - freqInLong;
			if(effectiveCount < 1) continue;
			//System.out.println(c + "\t" + collCounts.getCount(c) + "\t" + effectiveCount);
			String [] subWords = c.split("_");
			for(int i=0;i<subWords.length-2;i++) {
				for(int j=i+2;j<=subWords.length;j++) {
					if(i==0 && j == subWords.length) continue;
					//System.out.print(i + " " + j + ": ");
					StringBuffer sb = new StringBuffer();
					for(int k=i;k<j;k++) {
						sb.append(subWords[k]);
						if(k+1 < j) sb.append("_");
						//System.out.print(subWords[k] + " ");
					}
					String subString = sb.toString();
					if(tfs.containsKey(subString)) {
						if(!timesInLongerString.containsKey(subString)){
							timesInLongerString.put(subString, 0);
							freqInLongerString.put(subString, 0);
						}
						timesInLongerString.put(subString, timesInLongerString.get(subString) + 1);
						freqInLongerString.put(subString, freqInLongerString.get(subString) + effectiveCount);
						//if(!bags.containsKey(subString)) {
						//	bags.put(subString, new Bag<String>());
						//}
						//bags.get(subString).add(c, effectiveCount);
						//System.out.println(subString + "\t" + timesInLongerString.get(subString) + "\t" + 
						//		freqInLongerString.get(subString));
					}
				}
			}
		}
		for(String s : StringTools.getSortedList(cValues)) {
			System.out.println(s + "\t" + cValues.get(s));
		}
		System.out.println(accepted + "\t" + rejected);
	}

	public void printConcordance(int fo, int tf, int sil) {
		for(int i=fo;i<fo+tf;i++) {
			int end = offsetArray.get(i) + sil;
			int start = offsetArray.get(i);
			int pstart = start;
			for(int j=0;j<5;j++) {
				if(start > 0 && corpusArray.get(start-1) > -1) start--;
			}
			System.out.print(i + "\t");
			for(int j=start;corpusArray.get(j) != -1 && j-end < 5;j++) {
				if(j == pstart || j == end) System.out.print("  ");
				System.out.print(tokenList.get(corpusArray.get(j)) + " ");
			}
			System.out.println();
		}
	}

	public int lcpLength(int o1, int o2) {
		for(int i=0;true;i++) {
			if(o1 + i == corpusArray.size()) return i;
			if(o2 + i == corpusArray.size()) return i;
			int o1c = corpusArray.get(o1+i);
			int o2c = corpusArray.get(o2+i);
			if(o1c < 0 || o2c < 0 || o1c != o2c) return i;
		}
	}
	
	public int getDocID(int offset) {
		for(int i=offset;i<corpusArray.size();i++) {
			if(corpusArray.get(i) < -1) {
				return (-2-corpusArray.get(i));
			}
		}
		throw new Error();
	}
	
	private void logClass(int i, int j, int k, int df) {
		int lbl = Math.max(lcp.get(i), lcp.get(j+1));
		int sil = lcp.get(k);
		if(i == j) {
			//if(verbose) System.out.println("trivial <" + i + "," + j + ">, tf = 1");
		} else if(lbl < sil) {
			//NewSubstringClass sc = new SubstringClass(sil, lbl, (j-i+1), df, suffixArray.get(i).longestCommonPrefix(suffixArray.get(j)));
			NewSubstringClass sc = new NewSubstringClass(sil, lbl, (j-i+1), df, i);
			//System.out.println(sil + "\t" + lbl + "\t" + lcpLength(offsetArray.get(i), offsetArray.get(j)));
			classArray.add(sc);
		}

	}

	public void calculateNGrams() {
		boolean verbose = true;
		long time=System.currentTimeMillis();
		
		
		lcp = new ArrayList<Integer>(offsetArray.size() + 1);
		for(int i=0;i<=offsetArray.size();i++) {
			if(i == 0 || i == offsetArray.size()) {
				lcp.add(0);
			} else {
				lcp.add(lcpLength(offsetArray.get(i), offsetArray.get(i-1)));
				//lcp.add(offsetArray.get(i).longestCommonPrefixLength(offsetArray.get(i-1)));
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
		for(int i=0;i<numdocs;i++) {
			docLink.add(-1);
		}
		
		classArray = new ArrayList<NewSubstringClass>();
		
		for(int j=0;j<offsetArray.size();j++) {
			logClass(j, j, 0, 1);
			int doc = getDocID(offsetArray.get(j));
			//System.out.println(doc);
			//int doc = offsetArray.get(j).getDocID();
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
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/newEnzyme"), "source.xml");
		List<File> files = new ArrayList<File>();
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/allGrapefruit"), "source.xml"));
		//List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/newEnzyme"), "source.xml");
		new NewNGramTfDf().run(files);
	}

}
