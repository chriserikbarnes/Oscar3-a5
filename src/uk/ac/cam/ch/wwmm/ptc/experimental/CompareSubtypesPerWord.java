package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;

import org.tartarus.snowball.ext.PorterStemmer;

import uk.ac.cam.ch.wwmm.ptclib.misc.ClassificationEvaluator;
import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;

public class CompareSubtypesPerWord {

	public static String convertSubtype(String subtype) {
		return subtype;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {		
		Map<String,ClassificationEvaluator> evals = new HashMap<String,ClassificationEvaluator>();
		
		Stemmer st = new Stemmer(new PorterStemmer());
		
		//String docNo = "b600383d";

		//File acDir = new File("/home/ptc24/annot_challenges/subtypes_for_lrec_crb/");
		//File acDir = new File("/home/ptc24/annot_challenges/reacttypes_crb_28082008_easy/");
		File acDir = new File("/home/ptc24/annot_challenges/reacttypes_crb_12092008/");
		
		String [] fn = acDir.list();
		List<Double> cmAccs = new ArrayList<Double>();
		for(int fi=0;fi<fn.length;fi++) {
			System.out.println(fn[fi]);
			Pattern p = Pattern.compile("scrapbook-(b\\d+[a-z])\\.xml");
			Matcher m = p.matcher(fn[fi]);
			Map<String,ClassificationEvaluator> pevals = new HashMap<String,ClassificationEvaluator>();

			if(m.matches()) {
				String docNo = m.group(1);
				System.out.println(docNo);
				//if(!analytical.contains(docNo)) continue;
				
				//if(!"b316218d".equals(docNo)) continue;
				
				//Document doc1 = new Builder().build(new File("/home/ptc24/annot_challenges/reacttypes_crb_28082008_easy/scrapbook-" + docNo + ".xml"));
				//Document doc2 = new Builder().build(new File("/home/ptc24/newows/reactmiscrsceasy/" + docNo + "/scrapbook.xml"));
				Document doc1 = new Builder().build(new File("/home/ptc24/annot_challenges/reacttypes_crb_12092008/scrapbook-" + docNo + ".xml"));
				Document doc2 = new Builder().build(new File("/home/ptc24/newows/reactnewrsc/" + docNo + "/scrapbook.xml"));
				//Document doc1 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_ptc/" + docNo + "/scrapbook.xml"));
				//Document doc2 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_ptc/" + docNo + "/scrapbook.xml"));
				//Document doc2 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_crb/scrapbook_" + docNo + ".xml"));
				//Document doc2 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_crb_auto/" + docNo + ".xml"));
				
				XMLSpanTagger.tagUpDocument(doc1.getRootElement(), "a");
				

				String docStr = doc1.getValue();
				
				Nodes n1 = doc1.query("//ne");
				Nodes n2 = doc2.query("//ne");
				for(int i=0;i<n1.size();i++) {
					Element e1 = (Element)n1.get(i);
					Element e2 = (Element)n2.get(i);
					//String t1 = st.getStem(e1.getValue().toLowerCase());
					//String t2 = st.getStem(e2.getValue().toLowerCase());
					String t1 = e1.getValue().toLowerCase();
					String t2 = e2.getValue().toLowerCase();
					String st1 = e1.getAttributeValue("subtype");
					String st2 = e2.getAttributeValue("subtype");
					st1 = convertSubtype(st1);
					st2 = convertSubtype(st2);
				
					if(st1 == null) st1 = "";
					if(st2 == null) st2 = "";
					
					//if(t2.equals("CM")) st2 = "EXACT";
					
					if(!evals.containsKey(t1)) {
						evals.put(t1, new ClassificationEvaluator());
					}
					evals.get(t1).logEvent(st1, st2);
					
					if(!pevals.containsKey(t1)) {
						pevals.put(t1, new ClassificationEvaluator());
					}
					pevals.get(t1).logEvent(st1, st2);
					
					if(!st1.equals(st2)) {
						int start = Integer.parseInt(e1.getAttributeValue("xtspanstart"));
						int end = Integer.parseInt(e1.getAttributeValue("xtspanend"));
						int preStart = Math.max(0, start-20);
						int postEnd = Math.min(end+20, docStr.length());
						System.out.println(docStr.substring(preStart, start).replaceAll("\\s+", " ") + "\t" + docStr.substring(start,end) + "\t" + docStr.substring(end, postEnd).replaceAll("\\s+", " "));
						
						System.out.println(e1.getValue() + "\t" + t1 + ":" + st1 + "\t" + t2 + ":" + st2);
					}
					
				}
				if(false) {
					for(String type : pevals.keySet()) {
						//if(!"CM".equals(type)) continue;
						System.out.println(type);
						System.out.println("accuracy: " + pevals.get(type).getAccuracy());
						System.out.println("kappa: " + pevals.get(type).getKappa());
						pevals.get(type).pprintConfusionMatrix();
						pevals.get(type).pprintPrecisionRecallEval();
						System.out.println();
						cmAccs.add(pevals.get(type).getAccuracy());
					}
				}

			}
		}
		
		/*if(cmAccs.size() != 42) throw new Error();
		Collections.sort(cmAccs);
		double median = (cmAccs.get(20) + cmAccs.get(21)) / 2.0;
		System.out.println("median: " + median);
		System.out.println(cmAccs.get(0));
		System.out.println(cmAccs.get(41));*/
		
		//if(true) return;
		
		Map<String,Integer> typesBySize = new HashMap<String,Integer>();
		for(String type : evals.keySet()) {
			typesBySize.put(type, evals.get(type).getSize());
		}
		
		for(String type : StringTools.getSortedList(typesBySize)) {
			/*System.out.println(type);
			System.out.println("accuracy: " + evals.get(type).getAccuracy());
			System.out.println("kappa: " + evals.get(type).getKappa());
			evals.get(type).pprintConfusionMatrix();
			evals.get(type).pprintPrecisionRecallEval();
			System.out.println();*/
			System.out.println(type + "\t" + evals.get(type).getSize() + "\t" + evals.get(type).getAccuracy() + "\t" + evals.get(type).getKappa());
			//evals.get(type).pprintConfusionMatrix();

		}

	}

}
