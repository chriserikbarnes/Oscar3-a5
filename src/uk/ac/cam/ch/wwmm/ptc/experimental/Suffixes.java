package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math.stat.StatUtils;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class Suffixes {

	static class SuffixNode {
		Map<Character,SuffixNode> continuations;
		SuffixNode previous;
		int chemPaths;
		int nonChemPaths;
		
		public SuffixNode() {
			continuations = new HashMap<Character,SuffixNode>();
			chemPaths = 0;
			nonChemPaths = 0;
			previous = null;
		}

		public SuffixNode(SuffixNode previous) {
			continuations = new HashMap<Character,SuffixNode>();
			chemPaths = 0;
			nonChemPaths = 0;
			this.previous = previous;
		}
		
		public void addString(String s, boolean chemical) {
			if(chemical) {
				chemPaths++;
			} else {
				nonChemPaths++;
			}
			if(s.length() > 0) {
				char c = s.charAt(s.length()-1);
				String prefix = s.substring(0, s.length()-1);
				if(!continuations.containsKey(c)) {
					continuations.put(c, new SuffixNode(this));
				}
				continuations.get(c).addString(prefix, chemical);
			}
		}
		
		public void print(String history) {
			for(Character c : continuations.keySet()) {
				//if(!continuations.get(c).interesting()) {
				//	System.out.println("Yeep!");
				//}
				System.out.println(c + history + "\t" + continuations.get(c).chemPaths + "\t" + continuations.get(c).nonChemPaths + "\t" + continuations.get(c).chemScore());					
				continuations.get(c).print(c + history);
			}
		}
		
		public double chemScore() {
			double smoothFactor = 1.0;
			return (chemPaths + smoothFactor) / (chemPaths + nonChemPaths + smoothFactor + smoothFactor);
		}
		
		public boolean interesting() {
			if(previous == null) return true;
			double cs = chemScore();
			double pcs = previous.chemScore();
			if(cs == 0.5 ^ pcs == 0.5) return true;
			if((cs - 0.5) * (pcs - 0.5) < 0) return true;
			if((cs > 0.5) && (cs > pcs)) return true;
			if((cs < 0.5) && (cs < pcs)) return true;
			return false;
		}
		
		public void prune() {
			Set<Character> cs = new HashSet<Character>(continuations.keySet());
			for(Character c : cs) {
				continuations.get(c).prune();
				if(!continuations.get(c).interesting() && continuations.get(c).continuations.size() == 0) {
					continuations.remove(c);
				}
			}
		}
		
		public double lookup(String s) {
			if(s.length() > 0) {
				char c = s.charAt(s.length()-1);
				String prefix = s.substring(0, s.length()-1);
				if(continuations.containsKey(c)) {
					return continuations.get(c).lookup(prefix);
				}
				if(true) {
					return chemScore();
				} else {
					if(continuations.keySet().size() < 2) return chemScore();
					if(chemPaths * nonChemPaths == 0) return chemScore();
					int chemBranch = 0;
					int nonChemBranch = 0;
					for(Character cc : continuations.keySet()) {
						if(continuations.get(cc).chemScore() > 0.5) {
							chemBranch++;
						} else {
							nonChemBranch++;
						}
					}
					return chemBranch / (0.0 + chemBranch + nonChemBranch);	
				}				
			} 
			return chemScore();
		}
		
		public double knowledgeScale(String s) {
			if(s.length() > 0) {
				char c = s.charAt(s.length()-1);
				String prefix = s.substring(0, s.length()-1);
				if(continuations.containsKey(c)) {
					return continuations.get(c).lookup(prefix);
				}
				return 1.0 / (chemPaths + nonChemPaths);
			} 
			return 1.0 / (chemPaths + nonChemPaths);
		}
		
		public String getSuffix(String s, String suffix) {
			if(s.length() > 0) {
				char c = s.charAt(s.length()-1);
				String prefix = s.substring(0, s.length()-1);
				if(continuations.containsKey(c)) {
					return continuations.get(c).getSuffix(prefix, c + suffix);
				} else {
					return suffix;
				}
			} else{
				return suffix;
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Set<String> chemSet = NGram.getInstance().chemSet;
		Set<String> engSet = NGram.getInstance().engSet;	
		Set<String> wordSet = new HashSet<String>();
		wordSet.addAll(chemSet);
		wordSet.addAll(engSet);
		
		List<String> wordList = new ArrayList<String>(wordSet);

		Collections.shuffle(wordList, new Random(4));
		List<String> trainList = new ArrayList<String>(wordList.subList(0, 1*wordList.size()/10));
		List<String> activeList = wordList.subList(1*wordList.size()/10, 9*wordList.size()/10);
		List<String> testList = wordList.subList(9*wordList.size()/10, wordList.size());
		
		//long time = System.currentTimeMillis();
		SuffixNode sn = new SuffixNode();
		for(String word : trainList) {
			sn.addString(word, chemSet.contains(word));			
		}
		sn.prune();
		//System.out.println(System.currentTimeMillis() - time);
		//sn.print("");
	
		
		int increment = activeList.size() / 8;
		increment *= 4;
		System.out.println(increment);
		
		for(int i=0;i<1;i++) {
			System.out.println("Iteration " + i);
			Map<String,Double> activeMap = new HashMap<String,Double>();
			Random r = new Random(5);
			for(String word : activeList) {
				double score = r.nextDouble();
				//double score = sn.lookup(word);
				//score = 1.0 - Math.abs(0.8 - score);
				//score = score * (1.0 - score);
				//score = 1.0 - score;
				//double score = sn.knowledgeScale(word);
				activeMap.put(word,score);
			}
			
			activeList = StringTools.getSortedList(activeMap);
			trainList.addAll(activeList.subList(0, increment));
			activeList = activeList.subList(increment, activeList.size());
			sn = new SuffixNode();
			for(String word : trainList) {
				sn.addString(word, chemSet.contains(word));			
			}
			sn.prune();
		}
		
		
		
		int tp=0;
		int fp=0;
		int tn=0;
		int fn=0;

		List<Double> goodList = new ArrayList<Double>();
		List<Double> badList = new ArrayList<Double>();
		
		List<Double> chemList = new ArrayList<Double>();
		List<Double> engList = new ArrayList<Double>();
		
		Map<String,Double> badWordScores = new HashMap<String,Double>();
		
		for(String word : testList) {
			double cs = sn.lookup(word);
			boolean good = true;
			//if(cs < 0.9 && cs > 0.1) continue;
			if(chemSet.contains(word)) {
				chemList.add(cs);
			} else {
				engList.add(cs);
			}
			if(cs > 0.5) {
				if(chemSet.contains(word)) {
					tp++;
				} else {
					fp++;
					good = false;
				}
			} else {
				if(chemSet.contains(word)) {
					fn++;
					good = false;
				} else {
					tn++;
				}				
			}
			
			if(!good) {
				badWordScores.put(word, cs);
				//System.out.println(word + "\t" + cs + "\t" + sn.getSuffix(word, ""));
			}
			if(good) {
				goodList.add(cs);
			} else {
				badList.add(cs);
			}
		}
		
		for(String word : StringTools.getSortedList(badWordScores)) {
			System.out.println(word + "\t" + badWordScores.get(word) + "\t" + sn.getSuffix(word, ""));
		}
		
		double [] goodArray = new double[goodList.size()];
		for(int i=0;i<goodList.size();i++) goodArray[i] = goodList.get(i);
		double [] badArray = new double[badList.size()];
		for(int i=0;i<badList.size();i++) badArray[i] = badList.get(i);
		
		System.out.println(StatUtils.variance(goodArray));
		System.out.println(StatUtils.variance(badArray));
		
		System.out.println(tp + "\t" + fp + "\t" + fn + "\t" + tn);
		
		Collections.sort(chemList, Collections.reverseOrder());
		Collections.sort(engList, Collections.reverseOrder());
		int chemCount = 0;
		int engCount = 0;
		double map = 0.0;
		while(chemCount < chemList.size() && engCount < engList.size()) {
			if(engList.get(engCount) >= chemList.get(chemCount)) {
				engCount++;
			} else {
				chemCount++;
				double precision = chemCount * 1.0 / (chemCount + engCount);
				map += precision / chemList.size();
				if(chemCount % 10 == 0) {
					System.out.println((chemCount * 1.0 / chemList.size()) + "\t" + precision);
				}
			}
		}
		System.out.println(map);
		
	}

}
