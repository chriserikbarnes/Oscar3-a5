package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import uk.ac.cam.ch.wwmm.oscar3.models.ExtractTrainingData;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class SingleLogit {

	
	public static double loss(double m, double c, List<Double> positiveExamples, List<Double> negativeExamples) {
		double loss = 0.0;
		for(Double d : positiveExamples) {
			loss += -Math.log(1+Math.exp(-m*d-c));
		}
		for(Double d : negativeExamples) {
			loss += -Math.log(1+Math.exp(m*d+c));
		}
		return loss;		
	}
	
	/**
	 * @param args
	 */
	// I'm sure there's a nice analytic way of doing this. Ah well...
	public static void main(String[] args) {
		List<Double> positiveExamples = new ArrayList<Double>();
		List<Double> negativeExamples = new ArrayList<Double>();
		
		ExtractTrainingData etd1 = ExtractTrainingData.getInstance();
		List<File> sbFiles = new ArrayList<File>();
		sbFiles.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/goodrsc"), "scrapbook.xml"));
		ExtractTrainingData etd2 = new ExtractTrainingData(sbFiles);
		Set<String> chem = new HashSet<String>(etd2.chemicalWords);
		//chem.removeAll(etd1.chemicalWords);
		for(String w : chem) {
			if(!NGram.getInstance().chemSet.contains(NGram.parseWord(w))) {
				double score = NGram.getInstance().testWord(w);
				positiveExamples.add(score);
			}
		}
		System.out.println();
		Set<String> nonchem = new HashSet<String>(etd2.nonChemicalWords);
		chem.removeAll(etd1.nonChemicalWords);
		for(String w : nonchem) {
			if(!NGram.getInstance().engSet.contains(NGram.parseWord(w))) {
				double score = NGram.getInstance().testWord(w);
				negativeExamples.add(score);
			}
		}

		
		//Random r = new Random(0);
		//for(int i=0;i<10000;i++) {
		//	positiveExamples.add(r.nextGaussian() * 1.0 + 0.1);
		//	negativeExamples.add(r.nextGaussian() * 0.5 + 0.0);			
		//}
		//positiveExamples.add(negativeExamples.get(0));
		
		/*positiveExamples.add(1.0);
		positiveExamples.add(2.0);
		positiveExamples.add(4.0);
		negativeExamples.add(-1.5);
		negativeExamples.add(-0.5);
		negativeExamples.add(1.7);*/
		
		double m = 0.0;
		double c = 0.0;
		double step = 1.0;
		double bestLoss = loss(m, c, positiveExamples, negativeExamples);
		//double impfactor = Double.POSITIVE_INFINITY;
		long time = System.currentTimeMillis();
		while(step > 0 && Math.max(step / Math.abs(m), step / Math.abs(c)) > 0.0001) {
		//while(bestLoss < -0.00001 && step > 0 && impfactor > 0.00001) {
			//System.out.println(impfactor);
			//System.out.println(Math.max(step / Math.abs(m), step / Math.abs(c)));
			System.out.println(m + "\t" + c + "\t" + step + "\t" + "\t"  + bestLoss);
			double incmloss = loss(m+step, c, positiveExamples, negativeExamples);
			if(incmloss > bestLoss) {
				while(incmloss > bestLoss) {
					m = m+step;
					//impfactor = - (incmloss - bestLoss) / bestLoss;
					bestLoss = incmloss;
					incmloss = loss(m+step, c, positiveExamples, negativeExamples);
				}
				continue;
			} 
			double decmloss = loss(m-step, c, positiveExamples, negativeExamples);
			if(decmloss > bestLoss) {
				while(decmloss > bestLoss) {
					m = m-step;
					//impfactor = - (decmloss - bestLoss) / bestLoss;
					bestLoss = decmloss;
					decmloss = loss(m-step, c, positiveExamples, negativeExamples);
				}
				continue;
			} 
			double inccloss = loss(m, c+step, positiveExamples, negativeExamples);
			if(inccloss > bestLoss) {
				while(inccloss > bestLoss) {
					c = c+step;
					//impfactor = - (inccloss - bestLoss) / bestLoss;
					bestLoss = inccloss;
					inccloss = loss(m, c+step, positiveExamples, negativeExamples);
				}
				continue;
			} 
			double deccloss = loss(m, c-step, positiveExamples, negativeExamples);
			if(deccloss > bestLoss) {
				while(deccloss > bestLoss) {
					c = c-step;
					//impfactor = - (deccloss - bestLoss) / bestLoss;
					bestLoss = deccloss;
					deccloss = loss(m, c-step, positiveExamples, negativeExamples);
				}
				continue;
			} 
			step /= 2;
		}
		System.out.println(System.currentTimeMillis() - time);
		//if(true) return;
		System.out.println();
		double minp = 1.0;
		double dminp = Double.NaN;
		int tp = 0;
		int fp = 0;
		int tn = 0;
		int fn = 0;
		for(Double d : positiveExamples) {
			double l = m*d + c;
			double p = 1.0 / (1.0 + Math.exp(-l));
			System.out.println(d + "\t" + p);
			if(p < minp) {
				minp = p;
				dminp = d;
			}
			if(p > 0.5) {
				tp++;
			} else {
				fn++;
			}
		}
		System.out.println();
		System.out.println(dminp + "\t" + minp);
		System.out.println();
		for(Double d : negativeExamples) {
			double l = m*d + c;
			double p = 1.0 / (1.0 + Math.exp(-l));
			System.out.println(d + "\t" + p);
			if(p > 0.5) {
				fp++;
			} else {
				tn++;
			}
		}
		System.out.println(tp + "\t" + fp + "\t" + fn + "\t" + tn);
		
		
		// TODO Auto-generated method stub

	}

}
