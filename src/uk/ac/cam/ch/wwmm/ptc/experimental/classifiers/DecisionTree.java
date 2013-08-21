package uk.ac.cam.ch.wwmm.ptc.experimental.classifiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class DecisionTree {

	private Set<String> usedFeatures;
	String feature;
	String outcome;
	int outcomeCount;
	DecisionTree withFeature;
	DecisionTree withoutFeature;
	
	public DecisionTree(Collection<BagEvent> events) {
		this(events, new HashSet<String>());
	}
	
	public DecisionTree(Collection<BagEvent> events, Set<String> uf) {
		//System.out.println(uf);
		usedFeatures = uf;
		feature = null;
		outcome = null;
		withFeature = null;
		withoutFeature = null;
		
		List<BagEvent> evList = new ArrayList<BagEvent>(events);
		Set<String> usedFeatures = new HashSet<String>();
		
		Set<String> features = new HashSet<String>();
		Bag<String> outcomes = new Bag<String>();
		for(BagEvent be : events) {
			outcomes.add(be.getClassLabel());
			features.addAll(be.getFeatures().getSet());
		}
		
		if(outcomes.getSet().size() == 1) {
			outcome = outcomes.getList().get(0);
			outcomeCount = outcomes.totalCount();
			return;
		} else {
			int totalOther = 0;
			List<String> ol = outcomes.getList();
			for(int i=1;i<ol.size();i++) {
				totalOther += outcomes.getCount(ol.get(i));
			}
			/*if(totalOther < 20) {
				outcome = outcomes.getList().get(0);
				outcomeCount = outcomes.totalCount();
				return;
			}*/
		}
		
		int os = outcomes.totalCount();
		double entropy = outcomes.entropy();		

		String bestFeature = null;
		double bestGain = 0.0;
		for(String feature : features) {
			if(usedFeatures.contains(feature)) continue;
			Bag<String> outcomesWith = new Bag<String>();
			Bag<String> outcomesWithout = new Bag<String>();
			for(BagEvent be : events) {
				if(be.getFeatures().getCount(feature) > 0) {
					outcomesWith.add(be.getClassLabel());					
				} else {
					outcomesWithout.add(be.getClassLabel());
				}
			}
			double newEntropy = 0.0;
			int ows = outcomesWith.totalCount();
			if(ows > 0) newEntropy += outcomesWith.entropy() * ows / os;
			int owos = outcomesWithout.totalCount();
			if(owos > 0) newEntropy += outcomesWithout.entropy() * owos / os;
			double entropyGain = entropy - newEntropy;
			if(entropyGain > bestGain) {
				bestFeature = feature;
				bestGain = entropyGain;
			}
		}
		//System.out.println(bestFeature + "\t" + bestGain);
		if(bestFeature != null) {
			this.feature = bestFeature;
			List<BagEvent> evsWith = new ArrayList<BagEvent>();
			List<BagEvent> evsWithout = new ArrayList<BagEvent>();
			for(BagEvent be : events) {
				if(be.getFeatures().getCount(bestFeature) > 0) {
					evsWith.add(be);
				} else {
					evsWithout.add(be);
				}
			}
			Set<String> newUsedFeatures = new HashSet<String>(usedFeatures);
			newUsedFeatures.add(bestFeature);
			//System.out.println("With: " + bestFeature);
			withFeature = new DecisionTree(evsWith, newUsedFeatures);
			//System.out.println("Without: " + bestFeature);
			withoutFeature = new DecisionTree(evsWithout, newUsedFeatures);
		} else {
			outcome = outcomes.getList().get(0);
			outcomeCount = outcomes.getCount(outcome);
			//System.out.println("terminate");
			//for(String outcome : outcomes.getList()) {
			//	System.out.println(outcome + "\t" + outcomes.getCount(outcome));
			//}
		}
	}
	
	public void printTree() {
		printTree("");
	}

	public void printTree(String past) {
		if(outcome == null) {
			System.out.println(past + feature);
			withFeature.printTree(past + feature + " ");
			withoutFeature.printTree(past + "-" + feature + " ");
		} else {
			System.out.println(past + outcome + " " + outcomeCount);
		}
	}

	
	public void printTree(int indent) {
		String iStr = StringTools.multiplyString(" " , indent);
		if(outcome == null) {
			System.out.println(iStr + feature);
			withFeature.printTree(indent + 1);
			withoutFeature.printTree(indent + 1);
		} else {
			System.out.println(iStr + outcome + " " + outcomeCount);
		}
	}
	
	public String testBag(Bag<String> features) {
		if(outcome != null) return outcome;
		if(features.getCount(feature) > 0) {
			return withFeature.testBag(features);
		} else {
			return withoutFeature.testBag(features);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
