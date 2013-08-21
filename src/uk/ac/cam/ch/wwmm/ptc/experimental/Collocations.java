package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.StringSource;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class Collocations {

	public static void main(String [] args) {
		List<File> files = new ArrayList<File>();
		//files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/paperset1"), "source.xml");
		//files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/paperset1"), "source.xml");

		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/BioIE"), "source.xml"));
		/*files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/bigBactMet"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/bigBioOrg"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/bigCancer"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/bigGrapefruit"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/bigSmith"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/largePorcine"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/largeVet"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/smallBactMet"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/smallCancer"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/smallGrapefruit"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/smallSmith"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/smallPorcine"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/smallVet"), "source.xml"));*/

		/*files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12/31"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12/29"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12/28"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12/27"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12/24"), "source.xml"));
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12/22"), "source.xml"));*/

		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12/31"), "source.xml"));

		
		StringSource ss = new StringSource(files, false);
		
		Bag<String> wordCounts = new Bag<String>();
		Bag<String> collCounts = new Bag<String>();
		
		
		ss.reset();
		int col = 0;
		for(String s : ss) {
			/*System.out.print(".");
			col++;
			if(col >= 80) {
				col = 0;
				System.out.println();
			}*/
			//System.out.println(s);
			TokenSequence t = Tokeniser.getInstance().tokenise(s);
			LinkedList<StringBuffer> prevBuffer = new LinkedList<StringBuffer>();
			for(String word : t.getTokenStringList()) {
				if(!word.matches(".*[a-zA-Z0-9].*|-|/")) {
					if(prevBuffer.size() > 0) prevBuffer.removeLast();
					for(StringBuffer sb : prevBuffer) {
						collCounts.add(sb.toString());
					}
					prevBuffer.clear();
					continue;
				}
				word = StringTools.normaliseName(word);
				
				word = word.intern();
				
				wordCounts.add(word);
				if(prevBuffer.size() > 0) {
					for(StringBuffer sb : prevBuffer) {
						sb.append(" " + word);
					}					
				}
				/*if(prevBuffer.size() > 0) {
					for(int i=0;i<prevBuffer.size();i++) {
						StringBuffer sb = new StringBuffer();
						int size = 1; // For the end token
						for(int j=i;j<prevBuffer.size();j++) {
							if(!"-".equals(prevBuffer.get(j))) {
								sb.append(prevBuffer.get(j) + " ");
								size++;
							}
						}
						sb.append(StringTools.normaliseName(word));
						collCounts.add(sb.toString());
						collLengths.put(sb.toString(), size);
					}
				}*/
				prevBuffer.add(new StringBuffer(word));
				if(prevBuffer.size() > 6) {
					collCounts.add(prevBuffer.removeFirst().toString().intern());
				}
			}
			if(prevBuffer.size() > 0) prevBuffer.removeLast();
			for(StringBuffer sb : prevBuffer) {
				collCounts.add(sb.toString().intern());
			}
			prevBuffer.clear();
		}

		Map<String,Integer> collLengths = new HashMap<String,Integer>();
		for(String s : collCounts.getSet()) {
			collLengths.put(s, s.split(" ").length);
		}
		
		System.out.println("Digested texts");
				
		double wTotal = wordCounts.totalCount();
		double cTotal = collCounts.totalCount();
		
		Map<String,Double> cScores = new HashMap<String,Double>();
		
		Set<String> stops = TermSets.getClosedClass();

		Set<String> candidateSet = new HashSet<String>();
		
		for(String c : collCounts.getSet()) {
			String [] subWords = c.split(" ");
			boolean hasStop = false;
			for(int i=0;i<subWords.length;i++) {
				if(stops.contains(subWords[i])) {
					hasStop = true;
					break;
				}
			}
			if(hasStop) continue;
			if(subWords[0].equals("/") || subWords[0].equals("-") || 
					subWords[subWords.length-1].equals("-") ||
					subWords[subWords.length-1].equals("/")) continue;
			if(collCounts.getCount(c) > 1) candidateSet.add(c);
			double cProb = collCounts.getCount(c) / cTotal;
			double wProb = 1.0;
			for(int i=0;i<subWords.length;i++) {
				wProb *= wordCounts.getCount(subWords[i])/wTotal;
			}
			double mi = Math.log(cProb / wProb) / Math.log(2);
			if(mi < 0.0) continue;
			int countVal = collCounts.getCount(c);
			countVal = Math.max(0, countVal - 1);
			if(countVal == 0) continue;
			cScores.put(c, countVal * mi);
		}
		
		/*List<String> results = StringTools.getSortedList(cScores);
		for(String c : results) {
			System.out.println(c + "\t" + cScores.get(c) + "\t" + collLengths.get(c));
			System.out.print(collCounts.getCount(c));
			String [] subWords = c.split(" ");
			for(int i=0;i<subWords.length;i++) {
				System.out.print("\t" + wordCounts.getCount(subWords[i]));
			}
			System.out.println();
		}*/
		
		Map<String,Integer> timesInLongerString = new HashMap<String,Integer>();
		Map<String,Integer> freqInLongerString = new HashMap<String,Integer>();
		
		Map<String,Bag<String>> bags = new HashMap<String,Bag<String>>();
		
		List<String> candidates = new ArrayList<String>(candidateSet);
		StringTools.sortStringList(candidates, collLengths);
		int maxLen = collLengths.get(candidates.get(0));
		for(String c : candidates) {
			String [] subWords = c.split(" ");
			//if(subWords.length < maxLen) {
				timesInLongerString.put(c, 0);
				freqInLongerString.put(c, 0);
			//}
		}

		Map<String,Double> cValues = new HashMap<String,Double>();
		for(String c : candidates) {
			int len = c.split(" ").length;
			int freq = collCounts.getCount(c);
			int freqInLong = 0;
			double cVal;
			if(!bags.containsKey(c)) {
				cVal = Math.log(len) / Math.log(2) * freq;
			} else {
				double entropy = bags.get(c).entropy();
				double perplexity = Math.pow(2.0, entropy);
				freqInLong = bags.get(c).totalCount();
				cVal = Math.log(len) / Math.log(2) * (freq - (freqInLong/perplexity));
			}
			//int timesInLong = timesInLongerString.get(c);
			//int freqInLong = freqInLongerString.get(c);
			//if(timesInLong == 0) {
			//	cVal = Math.log(len) / Math.log(2) * freq;
			//} else {
			//	cVal = Math.log(len) / Math.log(2) * (freq - ((double)freqInLong/timesInLong));
			//}
			
			if(cVal < 5.0) continue; 
			
			cValues.put(c, cVal);

			//if(collCounts.getCount(c) <= 1) continue;
			int effectiveCount = collCounts.getCount(c) - freqInLong;
			if(effectiveCount < 1) continue;
			//System.out.println(c + "\t" + collCounts.getCount(c) + "\t" + effectiveCount);
			String [] subWords = c.split(" ");
			for(int i=0;i<subWords.length-2;i++) {
				for(int j=i+2;j<=subWords.length;j++) {
					if(i==0 && j == subWords.length) continue;
					//System.out.print(i + " " + j + ": ");
					StringBuffer sb = new StringBuffer();
					for(int k=i;k<j;k++) {
						sb.append(subWords[k]);
						if(k+1 < j) sb.append(" ");
						//System.out.print(subWords[k] + " ");
					}
					String subString = sb.toString();
					if(candidateSet.contains(subString)) {
						//timesInLongerString.put(subString, timesInLongerString.get(subString) + 1);
						//freqInLongerString.put(subString, freqInLongerString.get(subString) + effectiveCount);
						if(!bags.containsKey(subString)) {
							bags.put(subString, new Bag<String>());
						}
						bags.get(subString).add(c, effectiveCount);
						//System.out.println(subString + "\t" + timesInLongerString.get(subString) + "\t" + 
						//		freqInLongerString.get(subString));
					}
				}
			}
		}
		
		System.out.println();
		System.out.println();
		
		List<String> terms = StringTools.getSortedList(cValues);
		for(String term : terms) {
			System.out.println(term + "\t\t\t" + cValues.get(term) + "\t" + collCounts.getCount(term));
			Bag<String> b = bags.get(term);
			if(b != null) {
				//System.out.println(Math.pow(2.0, b.entropy()) + "\t" + b.totalCount());
				//System.out.println(b);
			} else {
				//System.out.println("0\t0");
			}
			//System.out.println(timesInLongerString.get(term) + "\t" + freqInLongerString.get(term));
		}
		
		

		
	}
	
}
