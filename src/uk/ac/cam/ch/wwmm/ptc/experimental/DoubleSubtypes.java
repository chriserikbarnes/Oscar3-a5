package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.ptclib.misc.ClassificationEvaluator;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;

public class DoubleSubtypes {

	public static String convertSubtype(String subtype) {
		//if(subtype.equals("SURFACE")) return "EXACT";
		//if(subtype.equals("POLYMER")) return "PART";
		//if(subtype.equals("PART")) return "EXACT";
		
		if(true) return subtype;
		if(subtype.equals("ION")) return "EXACT";
		if(subtype.equals("PROTEIN")) return "EXACT";
		if(subtype.equals("STUFF")) return "EXACT";
		if(subtype.equals("LIKE")) return "EXACT";
		if(subtype.equals("SURFACE")) return "EXACT";
		if(subtype.equals("TECH")) return "EXACT";
		//if(subtype.equals("STRUCTURE")) return "CLASS";
		if(subtype.equals("STRUCTURE")) return "EXACT";
		if(subtype.equals("OTHER")) return "EXACT";
		if(subtype.equals("CLASS")) return "EXACT";
		if(subtype.equals("SPECIES")) return "PART";
		//if(subtype.equals("BOND")) return "PART";
		return subtype;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Set<String> analytical = new HashSet<String>(StringTools.arrayToList(new String[]{
				"b304177h",
				"b306947h", 
				"b307474a", 
				"b307981c", 
				"b308052h", 
				"b308914b", 
				"b309237b", 
				"b309380h", 
				"b311998j", 
				"b312285a", 
				"b313239k"}));
		Set<String> materials = new HashSet<String>(StringTools.arrayToList(new String[]{
				"b302845c", 
				"b303205a", 
				"b303244b", 
				"b308044g", 
				"b310260b", 
				"b310709d", 
				"b310932a", 
				"b310952f", 
				"b310991g", 
				"b311655g"}));
		Set<String> organic = new HashSet<String>(StringTools.arrayToList(new String[]{
				"b304959k", 
				"b305986c", 
				"b306799h", 
				"b307426a", 
				"b308195h", 
				"b309235f", 
				"b311151b", 
				"b312552a", 
				"b312761c",
				"b313655h",
				"b313956e",
				"b315181f"}));
		Set<String> inorganic = new HashSet<String>(StringTools.arrayToList(new String[]{
				"b309317d",
				"b309733a",
				"b310238f",
				"b311063j",
				"b311113j",
				"b313341a",
				"b314686c"}));
		Set<String> theoretical = new HashSet<String>(StringTools.arrayToList(new String[]{
				"b310723j"}));
		Set<String> education = new HashSet<String>(StringTools.arrayToList(new String[]{
				"b309878h"}));
	
		
		Map<String,ClassificationEvaluator> evals = new HashMap<String,ClassificationEvaluator>();
		
		//String docNo = "b600383d";

		File acDir = new File("/home/ptc24/annot_challenges/subtypes_for_lrec_crb/");
		
		String [] fn = acDir.list();
		List<Double> cmAccs = new ArrayList<Double>();
		for(int fi=0;fi<fn.length;fi++) {
			System.out.println(fn[fi]);
			Pattern p = Pattern.compile("scrapbook_(b\\d+[a-z])\\.xml");
			Matcher m = p.matcher(fn[fi]);
			Map<String,ClassificationEvaluator> pevals = new HashMap<String,ClassificationEvaluator>();

			if(m.matches()) {
				String docNo = m.group(1);
				System.out.println(docNo);
				//if(!analytical.contains(docNo)) continue;
				
				//if(!"b316218d".equals(docNo)) continue;
				
				//Document doc1 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_crb/scrapbook_" + docNo + ".xml"));
				//Document doc1 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_ptc/" + docNo + "/scrapbook.xml"));
				//Document doc2 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_ptc/" + docNo + "/scrapbook.xml"));
				//Document doc2 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_crb/scrapbook_" + docNo + ".xml"));
				//Document doc2 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_crb_auto/" + docNo + ".xml"));
				
				Document doc1 = new Builder().build(new File("/home/ptc24/corpora/doubleSubtypes/" + docNo + "/scrapbook.xml"));
				Document doc2 = new Builder().build(new File("/home/ptc24/annot_challenges/subtypes_for_lrec_crb/scrapbook_" + docNo + ".xml"));
				
				
				
				//XMLSpanTagger.tagUpDocument(doc1.getRootElement(), "a");
				

				String docStr = doc1.getValue();
				
				Nodes n1 = doc1.query("//ne");
				Nodes n2 = doc2.query("//ne");
				for(int i=0;i<n1.size();i++) {
					Element e1 = (Element)n1.get(i);
					Element e2 = (Element)n2.get(i);
					String t1 = e1.getAttributeValue("type");
					String t2 = e2.getAttributeValue("type");
					String st1 = e1.getAttributeValue("subtype");
					String st2 = e2.getAttributeValue("subtype");
					st1 = convertSubtype(st1);
					st2 = convertSubtype(st2);
				
					e1.addAttribute(new Attribute("altSubtype", st2));
					
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
					
					/*if(!st1.equals(st2)) {
						int start = Integer.parseInt(e1.getAttributeValue("xtspanstart"));
						int end = Integer.parseInt(e1.getAttributeValue("xtspanend"));
						int preStart = Math.max(0, start-20);
						int postEnd = Math.min(end+20, docStr.length());
						System.out.println(docStr.substring(preStart, start) + "\t" + docStr.substring(start,end) + "\t" + docStr.substring(end, postEnd));
						
						System.out.println(e1.getValue() + "\t" + t1 + ":" + st1 + "\t" + t2 + ":" + st2);
					}*/
					
				}
				for(String type : pevals.keySet()) {
					if(!"CM".equals(type)) continue;
					System.out.println(type);
					System.out.println("accuracy: " + pevals.get(type).getAccuracy());
					System.out.println("kappa: " + pevals.get(type).getKappa());
					pevals.get(type).pprintConfusionMatrix();
					pevals.get(type).pprintPrecisionRecallEval();
					System.out.println();
					cmAccs.add(pevals.get(type).getAccuracy());
				}
				//XMLSpanTagger.deTagElement(doc1.getRootElement());
				File outFile = new File("/home/ptc24/corpora/doubleSubtypes/" + docNo + "/scrapbook.xml");
				FileOutputStream fos = new FileOutputStream(outFile);
				new Serializer(fos).write(doc1);
				fos.close();
			}
		}
		
		if(cmAccs.size() != 42) throw new Error();
		Collections.sort(cmAccs);
		double median = (cmAccs.get(20) + cmAccs.get(21)) / 2.0;
		System.out.println("median: " + median);
		System.out.println(cmAccs.get(0));
		System.out.println(cmAccs.get(41));
		
		//if(true) return;
		
		
		
		for(String type : evals.keySet()) {
			System.out.println(type);
			System.out.println("accuracy: " + evals.get(type).getAccuracy());
			System.out.println("kappa: " + evals.get(type).getKappa());
			evals.get(type).pprintConfusionMatrix();
			evals.get(type).pprintPrecisionRecallEval();
			System.out.println();
		}

	}

}
