package uk.ac.cam.ch.wwmm.ptc.experimental.classifiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Element;
import nu.xom.Elements;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class MultinomialNaiveBayes {

	private List<String> classes;
	private Map<String,Integer> classToClassID;
	private List<Bag<String>> featuresByClass;
	//int grandTotal;
	private List<Double> priors;
	private List<Integer> totals;
	
	public MultinomialNaiveBayes(Element elem) throws Exception {
		String classStr = elem.getFirstChildElement("classes").getValue();
		List<String> tmpClasses = StringTools.arrayToList(classStr.split(" "));
		int i=0;
		classes = new ArrayList<String>();
		classToClassID = new HashMap<String,Integer>();
		for(String c : tmpClasses) {
			String ci = c.intern(); 
			classes.add(ci);
			classToClassID.put(ci, i);
			i++;
		}
		
		String priorsStr = elem.getFirstChildElement("priors").getValue();
		List<String> tmpPriors = StringTools.arrayToList(priorsStr.split(" "));
		priors = new ArrayList<Double>();
		for(String p : tmpPriors) {
			priors.add(Double.parseDouble(p));
		}

		String totalsStr = elem.getFirstChildElement("totals").getValue();
		List<String> tmpTotals = StringTools.arrayToList(totalsStr.split(" "));
		totals = new ArrayList<Integer>();
		for(String p : tmpTotals) {
			totals.add(Integer.parseInt(p));
		}
		
		featuresByClass = new ArrayList<Bag<String>>();
		Element fbcElem = elem.getFirstChildElement("featuresByClass");
		Elements ee = fbcElem.getChildElements("features");
		for(i=0;i<ee.size();i++) {
			Bag<String> fb = new Bag<String>();
			featuresByClass.add(fb);
			String fStr = ee.get(i).getValue();
			for(String line : StringTools.arrayToList(fStr.split("\n"))) {
				if(line.matches("\\S+\t\\S+")) {
					String [] sa = line.split("\t");
					fb.add(sa[0].intern(), Integer.parseInt(sa[1]));
				}
			}
		}
		
	}
	
	public MultinomialNaiveBayes(Collection<BagEvent> events) {
		Set<String> classSet = new LinkedHashSet<String>();
		for(BagEvent event : events) classSet.add(event.getClassLabel());
		classes = new ArrayList<String>();
		classToClassID = new HashMap<String,Integer>();
		featuresByClass = new ArrayList<Bag<String>>();
		int i=0;
		priors = new ArrayList<Double>();
		for(String c : classSet) {
			String ci = c.intern(); 
			classes.add(ci);
			classToClassID.put(ci, i);
			featuresByClass.add(new Bag<String>());
			priors.add(0.0);
			i++;
		}
		double itemSize = 1.0 / events.size();
		//grandTotal = 0;
		for(BagEvent event : events) {
			String c = event.getClassLabel();
			int cid = classToClassID.get(c);
			featuresByClass.get(cid).addAll(event.getFeatures());
			//grandTotal += event.getFeatures().totalCount();
			priors.set(cid, priors.get(cid) + itemSize);
		}
		totals = new ArrayList<Integer>();
		for(i=0;i<classes.size();i++) {
			totals.add(featuresByClass.get(i).totalCount());
		}
	}

	public Map<String,Double> testBag(Bag<String> features) {
		return testBag(null, features);
	}
	
	public Map<String,Double> testBag(String leaveOneOutClass, Bag<String> features) {
		int loocid = -1;
		if(classToClassID.containsKey(leaveOneOutClass)) loocid = classToClassID.get(leaveOneOutClass);
		Map<String,Double> results = new HashMap<String,Double>();
		List<Double> logProbList = new ArrayList<Double>();
		for(int i=0;i<classes.size();i++) {
			logProbList.add(Math.log(priors.get(i)));
		}
		int fTotal = features.totalCount();
		for(String feature : features.getSet()) {
			int multiples = features.getCount(feature);
			for(int i=0;i<classes.size();i++) {
				/*if(i == loocid) {
					int count = featuresByClass.get(i).getCount(feature);
					int total = totals.get(i);
					double lp = multiples * Math.log(count * 1.0 / total);
					logProbList.set(i, logProbList.get(i) + lp);
				} else {
					int count = featuresByClass.get(i).getCount(feature) + multiples;
					int total = totals.get(i) + fTotal;
					double lp = multiples * Math.log(count * 1.0 / total);
					logProbList.set(i, logProbList.get(i) + lp);					
				}*/
				if(i == loocid) {
					int count = featuresByClass.get(i).getCount(feature) - multiples + 1;
					int total = totals.get(i) - fTotal + 1;
					double lp = multiples * Math.log(count * 1.0 / total);
					logProbList.set(i, logProbList.get(i) + lp);
				} else {
					int count = featuresByClass.get(i).getCount(feature) + 1;
					int total = totals.get(i) + 1;
					double lp = multiples * Math.log(count * 1.0 / total);
					logProbList.set(i, logProbList.get(i) + lp);					
				}
			}
		}
		double maxResult = Double.NEGATIVE_INFINITY;
		for(int i=0;i<classes.size();i++) {
			maxResult = Math.max(maxResult, logProbList.get(i));
		}
		double totalProb = 0.0;
		for(int i=0;i<classes.size();i++) {
			totalProb += Math.exp(logProbList.get(i) - maxResult);
		}
		for(int i=0;i<classes.size();i++) {
			results.put(classes.get(i), Math.exp(logProbList.get(i) - maxResult) / totalProb);
		}
		return results;
	}

	public String bestResult(Map<String,Double> results) {
		String result = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for(String s : results.keySet()) {
			if(results.get(s) > bestScore) {
				result = s;
				bestScore = results.get(s);
			}
		}
		return result;
	}
	
	public Element toXML() {
		Element mnbElem = new Element("multinomialNaiveBayes");
		
		Element classesElem = new Element("classes");
		classesElem.appendChild(StringTools.objectListToString(classes, " "));
		mnbElem.appendChild(classesElem);
		
		Element priorsElem = new Element("priors");
		priorsElem.appendChild(StringTools.objectListToString(priors, " "));
		mnbElem.appendChild(priorsElem);

		Element totalsElem = new Element("totals");
		totalsElem.appendChild(StringTools.objectListToString(totals, " "));
		mnbElem.appendChild(totalsElem);
		
		Element fbcElem = new Element("featuresByClass");
		for(Bag<String> bag : featuresByClass) {
			Element features = new Element("features");
			fbcElem.appendChild(features);
			List<String> bagStrs = new ArrayList<String>();
			for(String s : bag.getSet()) {
				bagStrs.add(s + "\t" + bag.getCount(s));
			}
			features.appendChild(StringTools.objectListToString(bagStrs, "\n"));
		}
		mnbElem.appendChild(fbcElem);
		
		return mnbElem;
	}
	
	public static void main(String[] args) {

	}

}
