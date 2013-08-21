package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.util.List;

public class SimilarityMatrix {

	//Map<String,Integer> termDict;
	List<String> termList;
	
	//int termDictSize;
	float [] similarities;

	private int coordsTo1D(int x, int y) {
		if(x == y) return 0;
		if(y > x) {
			int tmp = x;
			x = y;
			y = tmp;
		}
		return ((x*(x+1))/2) + y + 1;
	}
	
	public SimilarityMatrix(SimilarityExtractor se, List<String> termList) {
		this.termList = termList;
		//termDict = new HashMap<String,Integer>();
		//termDictSize = 0;
		//for(String term : termList) {
		//	termDict.put(term, termDictSize++);
		//}
		
		similarities = new float[(termList.size()*termList.size())-termList.size()+1];
		// similarities[0] stands in for self-similarity
		similarities[0] = (float)1.0;
		for(int i=0;i<termList.size();i++) {
			//System.out.println(termList.get(i));
			for(int j=0;j<termList.size();j++) {
				//System.out.println(termList.get(i) + "\t" + termList.get(j));
				if(j <= i) {
					//similarities[coordsTo1D(i,j)] = similarities[j][i];
				} else if(j == i) {
					similarities[coordsTo1D(i,j)] = 1;
				} else {
					double sim = se.getSimilarity(termList.get(i), termList.get(j));
					similarities[coordsTo1D(i,j)] = (float)sim;
				}
			}
		}
//		/System.out.println("Similarity matrix computed");
	}
	
	public SimilarityMatrix(List<String> termList) {
		this(termList, true);
	}
	
	public SimilarityMatrix(List<String> termList, boolean makeArray) {
		this.termList = termList;
		if(makeArray) {
			similarities = new float[(termList.size()*termList.size())-termList.size()+1];
			for(int i=0;i<termList.size();i++) {
				similarities[coordsTo1D(i,i)] = 1;
			}			
		}
	}
	
	public void setSimilarity(int i, int j, float f) {
		similarities[coordsTo1D(i, j)] = f;
	}
	
	public float getSimilarity(int i, int j) {
		return similarities[coordsTo1D(i, j)];
	}
	
	public int getTermListSize() {
		return termList.size();
	}
	
	public String getNameForNumber(int i) {
		return termList.get(i);
	}
	
}
