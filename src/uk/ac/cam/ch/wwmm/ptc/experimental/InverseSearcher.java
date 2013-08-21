package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.HyphenTokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequenceSource;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.TLRHolder;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermMaps;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class InverseSearcher {

	List<Integer> corpusArray = new ArrayList<Integer>();
	List<Integer> offsetArray = new ArrayList<Integer>();
	List<String> tokenList = new ArrayList<String>();
	Map<String,Integer> tokenIndex = new HashMap<String,Integer>();

	
	public int compareToCorpus(List<Integer> tsl, int corpusOffset) {
		int tslOffset=0;
		while(true) {
			int c1 = tsl.get(tslOffset);
			int c2 = corpusArray.get(corpusOffset);
			//System.out.print(c1 + ":" + c2 + " ");
			if(c1 == -1) return 0;
			if(c2 == -1) return 1;
			int cd = c1 - c2;
			if(cd != 0) return cd;
			tslOffset++;
			corpusOffset++;
		}
	}
	
	public InverseSearcher(List<File> files) throws Exception {
		TokenSequenceSource tss = new TokenSequenceSource(files);

		corpusArray = new ArrayList<Integer>();
		offsetArray = new ArrayList<Integer>();
		tokenList = new ArrayList<String>();
		tokenIndex = new HashMap<String,Integer>();
		long time = System.currentTimeMillis();
		for(TokenSequence ts : tss) {
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
		System.out.println(System.currentTimeMillis() - time);
		Comparator<Integer> offsetComparator = new Comparator<Integer>() {
			public int compare(Integer o1, Integer o2) {
				int i1 = o1;
				int i2 = o2;
				while(true) {
					int c1 = corpusArray.get(i1);
					int c2 = corpusArray.get(i2);
					if(c1 == -1) {
						if(c2 == -1) return 0;
						return -1;
					}
					if(c2 == -1) return 1;
					int cd = c1 - c2;
					if(cd != 0) {
						return cd;
					}
					i1++;
					i2++;
				}
			}
		};
		
		Collections.sort(offsetArray, offsetComparator);
		System.out.println(System.currentTimeMillis() - time);	
	}
	
	public int [] searchForString(String searchString) {
		TokenSequence ts = Tokeniser.getInstance().tokenise(searchString);
		List<Integer> tsl = new ArrayList<Integer>(ts.size() + 1);
		for(Token t : ts.getTokens()) {
			String s = t.getValue().toLowerCase();
			if(tokenIndex.containsKey(s)) {
				tsl.add(tokenIndex.get(s));
			} else {
				return null;
			}
		}
		tsl.add(-1);
		//System.out.println(tsl);

		//long time = System.currentTimeMillis();
		int lowerOffsetPtr = 0;
		int upperOffsetPtr = offsetArray.size();
		int midOffsetPtr = (lowerOffsetPtr + upperOffsetPtr) / 2;
		while(true) {
			//System.out.println(lowerOffsetPtr + "\t" + midOffsetPtr + "\t" + upperOffsetPtr);
			//for(int i=offsetArray.get(midOffsetPtr);corpusArray.get(i) != -1;i++) {
			//System.out.print(tokenList.get(corpusArray.get(i)) + " ");
			//	System.out.print(corpusArray.get(i) + " ");
			//}
			//System.out.println();

			int midSearchOffset=offsetArray.get(midOffsetPtr);
			int cmp = compareToCorpus(tsl, midSearchOffset);
			//System.out.println(cmp);
			if(cmp == 0) {
				break;
			} else if(lowerOffsetPtr == upperOffsetPtr) {
				return null;
			} else if(lowerOffsetPtr == midOffsetPtr && midOffsetPtr == upperOffsetPtr - 1) {
				midOffsetPtr = upperOffsetPtr;
				lowerOffsetPtr = upperOffsetPtr;
			} else if(cmp > 0) {
				lowerOffsetPtr = midOffsetPtr;
				midOffsetPtr = (lowerOffsetPtr + upperOffsetPtr) / 2;
			} else {
				upperOffsetPtr = midOffsetPtr;
				midOffsetPtr = (lowerOffsetPtr + upperOffsetPtr) / 2;
			}
		}
		//System.out.println("Chopped: " + lowerOffsetPtr + "\t" + midOffsetPtr + "\t" + upperOffsetPtr);
		//System.out.println(System.currentTimeMillis() - time);

		//Sort out lower
		int cmp = compareToCorpus(tsl, offsetArray.get(lowerOffsetPtr));
		if(compareToCorpus(tsl, offsetArray.get(lowerOffsetPtr)) != 0) {
			int llPtr = lowerOffsetPtr;
			int luPtr = midOffsetPtr;
			int lmPtr = (llPtr + luPtr) / 2;
			while(true) {
				//System.out.println(llPtr + "\t" + lmPtr + "\t" + luPtr);
				cmp = compareToCorpus(tsl, offsetArray.get(lmPtr));
				if(cmp == 0) {
					if(lmPtr == llPtr + 1) {
						lowerOffsetPtr = lmPtr;
						break;
					}
					luPtr = lmPtr;
					lmPtr = (llPtr + luPtr) / 2;
				} else {
					if(lmPtr == llPtr) {
						lowerOffsetPtr = lmPtr + 1;
						break;
					}
					llPtr = lmPtr;
					lmPtr = (llPtr + luPtr) / 2;							
				}
			}
		}
		//System.out.println("Found lower: " + lowerOffsetPtr);
		if(upperOffsetPtr == offsetArray.size()) {
			upperOffsetPtr -= 1;
		} else if(compareToCorpus(tsl, offsetArray.get(upperOffsetPtr)) != 0) {
			int ulPtr = midOffsetPtr;
			int uuPtr = upperOffsetPtr;
			int umPtr = (ulPtr + uuPtr) / 2;
			while(true) {
				//System.out.println(ulPtr + "\t" + umPtr + "\t" + uuPtr);
				cmp = compareToCorpus(tsl, offsetArray.get(umPtr));
				if(cmp == 0) {
					if(umPtr == uuPtr - 1) {
						upperOffsetPtr = umPtr;
						break;
					}
					ulPtr = umPtr;
					umPtr = (ulPtr + uuPtr) / 2;
				} else {
					uuPtr = umPtr;
					umPtr = (ulPtr + uuPtr) / 2;							
				}
			}
		}
		//System.out.println("Found upper: " + upperOffsetPtr);
		return new int[]{lowerOffsetPtr, upperOffsetPtr, tsl.size()-1};
	}
	
	public void printConcordance(int [] results, PrintStream out) {
		for(int i=results[0];i<=results[1];i++) {
			int end = offsetArray.get(i) + results[2];
			int start = offsetArray.get(i);
			int pstart = start;
			for(int j=0;j<5;j++) {
				if(start > 0 && corpusArray.get(start-1) != -1) start--;
			}
			out.print(i + "\t");
			for(int j=start;corpusArray.get(j) != -1 && j-end < 5;j++) {
				if(j == pstart || j == end) out.print("  ");
				out.print(tokenList.get(corpusArray.get(j)) + " ");
			}
			out.println();
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		if(true) {
			ArrayList<String> ll = new ArrayList<String>(10000);
			BufferedReader br = new BufferedReader(new FileReader(new File(Oscar3Props.getInstance().workspace, "npc/names.txt")));
			long time = System.currentTimeMillis();
			int i = 0;
			for(String line = br.readLine();line!=null;line=br.readLine()) {
				ll.add(line);
				if(++i % 10000 == 0) {
					ll.clear();
					System.out.println(i);
				}
			}
			System.out.println(System.currentTimeMillis() - time);
			return;
		}
		
		
		List<File> files = new ArrayList<File>();
		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/newEnzyme"), "source.xml"));
//		files.addAll(FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005/12"), "source.xml"));
		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/allGrapefruit"), "source.xml"));
		System.out.println(files.size());
		HyphenTokeniser.init();
		TermMaps.init();
		TermSets.init();
		TLRHolder.getInstance();
		System.out.println("Init...");
		
		long time = System.currentTimeMillis();
		InverseSearcher is = new InverseSearcher(files);
		
		String searchString = "was found to be a minor";
		
		int [] results = is.searchForString(searchString);
		if(results == null) {
			System.out.println("No results for: " + searchString);
		} else {
			is.printConcordance(results, System.out);
		}
		System.out.println();
		
		time = System.currentTimeMillis();
		PrintStream ps = new PrintStream(new FileOutputStream(new File("/tmp/pcconcord.txt")));
		BufferedReader br = new BufferedReader(new FileReader(new File(Oscar3Props.getInstance().workspace, "npc/names.txt")));
		int i = 0;
		for(String line = br.readLine();line!=null;line=br.readLine()) {
			if(++i % 10000 == 0) System.out.println(i);
		//for(int i=0;i<10000;i++) {
			//String line = br.readLine();
			if(line == null) break;
			if(!line.contains("\t")) continue;
			searchString = line.split("\t")[0];
			if(searchString.matches("[A-Z]{1,3}")) continue;
			results = is.searchForString(searchString);
			if(results == null) {
				//System.out.println("No results for: " + searchString);
			} else {
				ps.println(searchString);
				is.printConcordance(results, ps);
				ps.println();
			}
		}
		ps.close();
		System.out.println(System.currentTimeMillis() - time);
		
		/*TokenSequence ts = Tokeniser.getInstance().tokenise(searchString);
		List<Integer> tsl = new ArrayList<Integer>(ts.size() + 1);
		for(Token t : ts.getTokens()) {
			String s = t.getValue().toLowerCase();
			if(tokenIndex.containsKey(s)) {
				tsl.add(tokenIndex.get(s));
			} else {
				tsl = null;
				break;
			}
		}
		if(tsl != null) {
			tsl.add(-1);
			//System.out.println(tsl);
			
			int lowerOffsetPtr = 0;
			int upperOffsetPtr = offsetArray.size();
			int midOffsetPtr = (lowerOffsetPtr + upperOffsetPtr) / 2;
			int result = -1;
			while(true) {
				//System.out.println(lowerOffsetPtr + "\t" + midOffsetPtr + "\t" + upperOffsetPtr);
				//for(int i=offsetArray.get(midOffsetPtr);corpusArray.get(i) != -1;i++) {
					//System.out.print(tokenList.get(corpusArray.get(i)) + " ");
				//	System.out.print(corpusArray.get(i) + " ");
				//}
				//System.out.println();

				int midSearchOffset=offsetArray.get(midOffsetPtr);
				int cmp = compareToFoo(tsl, corpusArray, midSearchOffset);
				//System.out.println(cmp);
				if(cmp == 0) {
					result = midOffsetPtr;
					break;
				} else if(lowerOffsetPtr == upperOffsetPtr) {
					result = -1;
					break;
				} else if(lowerOffsetPtr == midOffsetPtr && midOffsetPtr == upperOffsetPtr - 1) {
					midOffsetPtr = upperOffsetPtr;
					lowerOffsetPtr = upperOffsetPtr;
				} else if(cmp > 0) {
					lowerOffsetPtr = midOffsetPtr;
					midOffsetPtr = (lowerOffsetPtr + upperOffsetPtr) / 2;
				} else {
					upperOffsetPtr = midOffsetPtr;
					midOffsetPtr = (lowerOffsetPtr + upperOffsetPtr) / 2;
				}
			}
			System.out.println(result);
			System.out.println(System.currentTimeMillis() - time);

			if(result != -1) {
				for(int rresult=Math.max(0, result-10);rresult<=Math.min(offsetArray.size(), result+10);rresult++) {
					int end = offsetArray.get(rresult) + tsl.size();
					int start = offsetArray.get(rresult);
					int pstart = start;
					for(int i=0;i<5;i++) {
						if(start > 0 && corpusArray.get(start-1) != -1) start--;
					}
					System.out.print(rresult + "\t");
					for(int i=start;corpusArray.get(i) != -1 && i-end < 5;i++) {
						if(i == pstart || i == end - 1) System.out.print("  ");
						System.out.print(tokenList.get(corpusArray.get(i)) + " ");
						//System.out.print(corpusArray.get(i) + " ");
					}
					System.out.println();
				}
				
				//Sort out lower
				int cmp = compareToFoo(tsl, corpusArray, offsetArray.get(lowerOffsetPtr));
				if(cmp == 0) {
					// Lower pointer is good
				} else {
					int llPtr = lowerOffsetPtr;
					int luPtr = midOffsetPtr;
					int lmPtr = (llPtr + luPtr) / 2;
					while(true) {
						System.out.println(llPtr + "\t" + lmPtr + "\t" + luPtr);
						cmp = compareToFoo(tsl, corpusArray, offsetArray.get(lmPtr));
						if(cmp == 0) {
							if(lmPtr == llPtr + 1) {
								lowerOffsetPtr = lmPtr;
								break;
							}
							luPtr = lmPtr;
							lmPtr = (llPtr + luPtr) / 2;
						} else {
							llPtr = lmPtr;
							lmPtr = (llPtr + luPtr) / 2;							
						}
					}
				}
				System.out.println(lowerOffsetPtr);
				if(upperOffsetPtr < offsetArray.size() && compareToFoo(tsl, corpusArray, offsetArray.get(upperOffsetPtr)) == 0) {
					
				} else {
					int ulPtr = midOffsetPtr;
					int uuPtr = upperOffsetPtr;
					int umPtr = (ulPtr + uuPtr) / 2;
					while(true) {
						System.out.println(ulPtr + "\t" + umPtr + "\t" + uuPtr);
						cmp = compareToFoo(tsl, corpusArray, offsetArray.get(umPtr));
						if(cmp == 0) {
							if(umPtr == uuPtr - 1) {
								upperOffsetPtr = umPtr;
								break;
							}
							ulPtr = umPtr;
							umPtr = (ulPtr + uuPtr) / 2;
						} else {
							uuPtr = umPtr;
							umPtr = (ulPtr + uuPtr) / 2;							
						}
					}
				}
				System.out.println(upperOffsetPtr);
				for(int rresult=lowerOffsetPtr;rresult<=upperOffsetPtr;rresult++) {
					int end = offsetArray.get(rresult) + tsl.size();
					int start = offsetArray.get(rresult);
					int pstart = start;
					for(int i=0;i<5;i++) {
						if(start > 0 && corpusArray.get(start-1) != -1) start--;
					}
					System.out.print(rresult + "\t");
					for(int i=start;corpusArray.get(i) != -1 && i-end < 5;i++) {
						if(i == pstart || i == end - 1) System.out.print("  ");
						System.out.print(tokenList.get(corpusArray.get(i)) + " ");
						//System.out.print(corpusArray.get(i) + " ");
					}
					System.out.println();
				}

				
			}
			System.out.println();

		}*/
		
		
		/*for(Integer start : offsetArray.subList(0, 1000)) {
			for(int i=start;corpusArray.get(i) != -1 && i-start < 20;i++) {
				//System.out.print(tokenList.get(corpusArray.get(i)) + " ");
				System.out.print(corpusArray.get(i) + " ");
			}
			System.out.println();
		}*/
		
	}

}
