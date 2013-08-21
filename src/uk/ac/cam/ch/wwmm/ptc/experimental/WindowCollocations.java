package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequenceSource;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class WindowCollocations {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		int windowSize = 10;
		List<Bag<String>> collocsBags = new ArrayList<Bag<String>>();
		for(int i=0;i<windowSize;i++) {
			collocsBags.add(new Bag<String>());
		}
		Bag<String> allTerms = new Bag<String>();
		String word = "reduction";
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/BioIE"), "source.xml");
		//List<File> files = FileTools.getFilesFromDirectoryByName(new File("/scratch/pubmed/2005"), "source.xml");
		TokenSequenceSource ts = new TokenSequenceSource(files);
		for(TokenSequence t : ts) {
			List<String> tokens = t.getTokenStringList();
			for(int i=0;i<tokens.size();i++) {
				if(tokens.get(i).equals(word)) {
					for(int j=Math.max(0, i-windowSize);j<=Math.min(i+windowSize,tokens.size()-1);j++) {
						if(i == j) continue;
						allTerms.add(tokens.get(j));
						int dist = Math.abs(i-j);
						/*for(int k=dist-1;k<windowSize;k++) {
							collocsBags.get(k).add(tokens.get(j));
						}*/
						collocsBags.get(dist-1).add(tokens.get(j));
						//collocs.add(tokens.get(j));
					}
				}
			}
		}
		/*for(String s : collocsBags.get(windowSize-1).getList()) {
			System.out.print(s);
			for(int i=0;i<windowSize;i++) {
				System.out.print("\t" + (collocsBags.get(i).getCount(s) / (i+1)));
			}
			System.out.println();
		}*/
		for(String s : allTerms.getList()) {
			System.out.print(s);
			for(int i=0;i<windowSize;i++) {
				//System.out.print("\t" + ((100 * collocsBags.get(i).getCount(s)) / ((allTerms.getCount(s)) / windowSize)));
				System.out.print("\t" + collocsBags.get(i).getCount(s));
			}
			System.out.println();
		}
	}

}
