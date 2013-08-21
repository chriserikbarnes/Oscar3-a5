package uk.ac.cam.ch.wwmm.ptclib.misc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**A class for collecting and analysing the results of classification
 * experiments.
 * 
 * @author ptc24
 *
 */
public final class ClassificationEvaluator {

	private List<String> standardEvents;
	private List<String> classifierEvents;
	private List<String> classes;
	private Map<String,Integer> classMap;
	
	private int [][] confusionMatrix;
	private int [] standardTotals;
	private int [] classifierTotals;
	private int grandTotal;
	
	private double logloss;
	private double quadloss;
	
	/**Sets up a new ClassificationEvaluator.
	 * 
	 */
	public ClassificationEvaluator() {
		standardEvents = new ArrayList<String>();
		classifierEvents = new ArrayList<String>();
		logloss = 0.0;
		quadloss = 0.0;
	}
	
	/**Records a prediction, and what the prediction should have been.
	 * 
	 * @param standard What the prediction should have been.
	 * @param classifier What the prediction was.
	 */
	public void logEvent(String standard, String classifier) {
		if(standard == null || classifier == null) return;
		standardEvents.add(standard);
		classifierEvents.add(classifier);
	}

	/**Records a prediction, with probabilities. Required for the calculation
	 * of log-loss and square-loss scores.
	 * 
	 * @param standard What the prediction should have been.
	 * @param classifier The highest-probability prediction given.
	 * @param correctProb The probability given for the highest-probability
	 *  prediction.
	 * @param allProb An array containing all of the prediction probabilities.
	 */
	public void logEvent(String standard, String classifier, double correctProb, double [] allProb) {
		logEvent(standard, classifier);
		logloss -= Math.log(correctProb)/Math.log(2);
		quadloss += 1;
		quadloss -= 2 * correctProb;
		for(int i=0;i<allProb.length;i++) {
			quadloss += allProb[i] * allProb[i];
		}
	}
	
	/**Gets the accuracy of the predictions seen so far. The accuracy is defined
	 * as the proportion of predictions that were correct.
	 * 
	 * @return The accuracy.
	 */
	public double getAccuracy() {
		//System.out.println(standardEvents.size() + "\t" + classifierEvents.size());
		int correct = 0;
		int total = 0;
		for(int i=0;i<standardEvents.size();i++) {
			total++;
			if(standardEvents.get(i).equals(classifierEvents.get(i))) correct++;
		}
		return (double)correct/total;
	}
	
	/**Gets the total log-loss of the predictions seen so far.
	 * 
	 * @return The total log-loss.
	 */
	public double getLogloss() {
		return logloss;
	}
	
	/**Gets the total square loss of the predictions seen so far.
	 * 
	 * @return The total square loss.
	 */
	public double getQuadloss() {
		return quadloss;
	}
	
	/**Gets a list of all of the types of predictions that have been made,
	 * or should have been made.
	 * 
	 * @return The list of possible or actual predictions.
	 */
	public List<String> getClasses() {
		if(classes != null) return classes;
		LinkedHashSet<String> tmpClasses = new LinkedHashSet<String>();
		for(String e : standardEvents) tmpClasses.add(e);
		for(String e : classifierEvents) tmpClasses.add(e);
		classes = new ArrayList<String>(tmpClasses);
		return classes;
	}
	
	private Map<String,Integer> getClassMap() {
		if(classMap != null) return classMap;
		classMap = new HashMap<String,Integer>();
		for(int i=0;i<getClasses().size();i++) {
			classMap.put(getClasses().get(i), i);
		}
		return getClassMap();
	}
	
	/**Gets a confusion matrix. Use getClasses to interpret the row and
	 * column indices. The first index represents the prediction that should
	 * have been made, the second represents the one that was made.
	 * 
	 * @return The confusion matrix.
	 */
	public int [][] getConfusionMatrix() {
		if(confusionMatrix != null) return confusionMatrix;
		int classCount = getClasses().size();
		confusionMatrix = new int[classCount][classCount];
		standardTotals = new int[classCount];
		classifierTotals = new int[classCount];
		grandTotal = 0;
		for(int i=0;i<classCount;i++) {
			standardTotals[i] = 0;
			classifierTotals[i] = 0;
			for(int j=0;j<classCount;j++) {
				confusionMatrix[i][j] = 0;
			}
		}
		for(int i=0;i<standardEvents.size();i++) {
			int standard = getClassMap().get(standardEvents.get(i));
			int classifier = getClassMap().get(classifierEvents.get(i));
			confusionMatrix[standard][classifier]++;
			standardTotals[standard]++;
			classifierTotals[classifier]++;
			grandTotal++;
		}
		return confusionMatrix;
	}
	
	/**Prints the confusion matrix, and some label headings, to STDOUT.
	 * 
	 */
	public void pprintConfusionMatrix() {
		System.out.print(" \t");
		for(String clss : getClasses()) System.out.print(StringTools.shorten(clss, 7) + "\t");
		System.out.print("TOTAL");
		System.out.println();
		for(int i=0;i<getClasses().size();i++) {
			System.out.print(StringTools.shorten(getClasses().get(i), 7) + "\t");
			for(int j=0;j<getClasses().size();j++) {
				System.out.print(getConfusionMatrix()[i][j] + "\t");
			}
			System.out.print(standardTotals[i]);
			System.out.println();
		}
		System.out.print("TOTAL\t");
		for(int i=0;i<getClasses().size();i++) {
			System.out.print(classifierTotals[i]+"\t");
		}
		System.out.print(grandTotal);
		System.out.println();
	}
	
	/**Prints precision and recall statistics for individual classes to STDOUT.
	 * 
	 */
	public void pprintPrecisionRecallEval() {
		System.out.println(" \tRef\t\tResp\t\tPrec\tRec\tF");
		for(int i=0;i<getClasses().size();i++) {
			int agree = getConfusionMatrix()[i][i];
			int s = standardTotals[i];
			double spc = s * 100.0 / grandTotal;
			int c = classifierTotals[i];
			double cpc = c * 100.0 / grandTotal;
			double precision = (double)agree / c;
			double recall = (double)agree / s;
			double f = (2 * precision * recall / (precision + recall));
			System.out.printf("%s\t%d\t%.2f%%\t%d\t%.2f%%\t%.2f%%\t%.2f%%\t%.2f%%\n", StringTools.shorten(getClasses().get(i), 7), s, spc, c, cpc, precision*100, recall*100, f*100);
		}
	}
	
	/**Calculates Cohen's kappa for the confusion matrix.
	 * 
	 * @return Cohen's kappa for the confusion matrix.
	 */
	public double getKappa() {
		int agree = 0;
		double expectAgree = 0.0;
		for(int i=0;i<getClasses().size();i++) {
			agree += getConfusionMatrix()[i][i];
			expectAgree += (double)classifierTotals[i]*standardTotals[i]/grandTotal;
		}
		//System.out.printf("agree\t%d\texpect\t%f\n", agree, expectAgree);
		return (agree - expectAgree) / (grandTotal - expectAgree);
	}
	
	/**Gets the total number of predictions recorded.
	 * 
	 * @return The total number of predictions recorded.
	 */
	public int getSize() {
		getConfusionMatrix();
		return grandTotal;
	}
	
	
}
