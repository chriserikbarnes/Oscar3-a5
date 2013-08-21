package uk.ac.cam.ch.wwmm.ptc.experimental.classifiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public class DecisionList {

	List<String> featureList;
	List<String> outcomeList;
	String defaultOutcome;
	
	public DecisionList(Collection<BagEvent> events) {
		featureList = new ArrayList<String>();
		outcomeList = new ArrayList<String>();
		
		List<BagEvent> evList = new ArrayList<BagEvent>(events);
		Set<String> usedFeatures = new HashSet<String>();
		/*usedFeatures.add("synthesi");
		usedFeatures.add("dioxygen");
		usedFeatures.add("h");
		usedFeatures.add("catalyst");*/
		
		
		Bag<String> outcomes = new Bag<String>();
		for(BagEvent be : events) {
			outcomes.add(be.getClassLabel());
		}
		defaultOutcome = outcomes.getList().get(0);
		
		while(evList.size() > 0) {
			System.out.println(evList.size());
			Map<String,Bag<String>> foo = new HashMap<String,Bag<String>>();
			for(BagEvent be : evList) {
				for(String s : be.getFeatures().getSet()) {
					if(usedFeatures.contains(s)) continue;
					if(!foo.containsKey(s)) foo.put(s, new Bag<String>());
					//foo.get(s).add(be.getClassLabel(), be.getFeatures().getCount(s));
					foo.get(s).add(be.getClassLabel());
				}
			}
			String purestFeature = null;
			String pfOutcome = null;
			int pfc = -1;
			for(String s : foo.keySet()) {
				if(foo.get(s).size() == 1) {
					Bag<String> b = foo.get(s);
					String outcome = b.getList().get(0);
					int count = b.getCount(outcome);
					if(count > pfc) {
						pfc = count;
						purestFeature = s;
						pfOutcome = outcome;
					}
				}
			}
			if(purestFeature == null) break;
			usedFeatures.add(purestFeature);
			featureList.add(purestFeature);
			outcomeList.add(pfOutcome);
			
			System.out.println(purestFeature + "\t" + pfc + "\t" + pfOutcome);
			List<BagEvent> evList2 = new ArrayList<BagEvent>();
			for(BagEvent be : evList) {
				if(be.getFeatures().getCount(purestFeature) == 0) evList2.add(be);
			}
			evList = evList2;
		}
	}
	
	public String testBag(Bag<String> features) {
		for(int i=0;i<featureList.size();i++) {
			if(features.getCount(featureList.get(i)) > 0) return outcomeList.get(i);
		}
		return defaultOutcome;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
