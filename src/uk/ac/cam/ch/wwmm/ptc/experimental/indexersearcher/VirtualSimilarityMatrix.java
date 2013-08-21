package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.List;
import java.util.Map;

import uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity.SimilarityMatrix;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.CacheMap;
import Jama.Matrix;

public class VirtualSimilarityMatrix extends SimilarityMatrix {

	Matrix dataMatrix;
	Map<Long,Float> cache;
	int count;
	double [] magnitudes;
	int nVects;
	
	private long coordsTo1DLong(int x, int y) {
		if(x == y) return 0;
		if(y > x) {
			int tmp = x;
			x = y;
			y = tmp;
		}
		return (((long)x*(x+1))/2) + y + 1;
	}

	
	public VirtualSimilarityMatrix(Matrix dataMatrix, List<String> termList, int nVects) {
		super(termList, false);
		this.dataMatrix = dataMatrix;
		this.nVects = nVects;
		cache = new CacheMap<Long,Float>(100);
		count = 0;
		magnitudes = new double[termList.size()];
		for(int i=0;i<magnitudes.length;i++) {
			double magSq = 0.0;
			for(int k=0;k<nVects;k++) magSq += dataMatrix.get(i, k) * dataMatrix.get(i, k);
			magnitudes[i] = Math.sqrt(magSq);
		}
	}
	
	public float getSimilarity(int i, int j) {
		//count++;
		//if(count % 100000 == 0) System.out.print("*");
		if(i == j) return 1.0f;
		//long c = coordsTo1DLong(i, j);
		//if(false && cache.containsKey(c)) {
			//if(count % 1000 == 0) System.out.print("!");
		//	return cache.get(c);
		//}
		//double r11 = 0.0;
		double r12 = 0.0;
		//double r22 = 0.0;
		for(int k=0;k<nVects;k++) {
			double v1 = dataMatrix.get(i, k);
			double v2 = dataMatrix.get(j, k);
			//System.out.println(v1 + "\t" + v2);
			//r11 += v1 * v1;
			r12 += v1 * v2;
			//r22 += v2 * v2;
		}
		float f = (float)(r12 / (magnitudes[i] * magnitudes[j]));
		//float f = (float)(r12 / (Math.sqrt(r11) * Math.sqrt(r22)));
		//cache.put(c, f);
		return f;
	}	
}
