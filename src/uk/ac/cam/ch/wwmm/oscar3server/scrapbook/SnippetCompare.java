package uk.ac.cam.ch.wwmm.oscar3server.scrapbook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.xslt.XSLTransform;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;

/** Compares two scrapbook snippets to see if they're close-enough the same.
 * 
 * @author ptc24
 *
 */
final class SnippetCompare {

	private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3server/scrapbook/resources/");

	private static SnippetCompare myInstance;

	private XSLTransform xslt;
	
	public static void reinitialise() throws Exception {
		myInstance = null;
		getInstance();
	}
	
	private static SnippetCompare getInstance() throws Exception {
		if(myInstance == null) myInstance = new SnippetCompare();
		return myInstance;
	}

	private SnippetCompare() throws Exception {
		xslt = new XSLTransform(rg.getXMLDocument("canonicaliser.xsl"));
	}
	
	public static Element canonicalise(Element e) throws Exception {
		return getInstance().canonicaliseInternal(e);
	}
	
	private Element canonicaliseInternal(Element s) throws Exception {
		Document d = new Document(new Element(s));
		return new Element(XSLTransform.toDocument(xslt.transform(d)).getRootElement());
	}
	
	public static boolean compare(Element e0, Element e1) throws Exception {
		e0 = canonicalise(e0);
		e1 = canonicalise(e1);
		return e0.toXML().equals(e1.toXML());
	}
	
	public static Set<String> extractCanonicalNEs(Element snippetElem) throws Exception {
		snippetElem = canonicalise(snippetElem);
		XMLSpanTagger.tagUpDocument(snippetElem, "t");
		Set<String> nes = new LinkedHashSet<String>();
		Nodes neNodes = snippetElem.query(".//ne");
		for(int i=0;i<neNodes.size();i++) {
			Element e = (Element)neNodes.get(i);
			e.removeAttribute(e.getAttribute("xtid"));
			//String ev = e.getValue();
			nes.add(e.toXML().replaceAll("xtid=\".*?\"", ""));			
		}
		return nes;
	}
	public static ScoreStats getPrecisionAndRecall(Element testElem, Element refElem) throws Exception {
		return getPrecisionAndRecall(testElem, refElem, false);
	}
	
	public static ScoreStats getPrecisionAndRecall(Element testElem, Element refElem, boolean charScore) throws Exception {
		testElem = canonicalise(testElem);
		refElem = canonicalise(refElem);
		XMLSpanTagger.tagUpDocument(testElem, "t");
		XMLSpanTagger.tagUpDocument(refElem, "t");
		
		List<String> testChars = new ArrayList<String>();
		List<String> refChars = new ArrayList<String>();
		String str = testElem.getValue();
		for(int i=0;i<str.length();i++) {
			testChars.add("O");
			refChars.add("O");
		}
		
		ScoreStats ss = new ScoreStats();
		Set<String> refStrs = new HashSet<String>();
		Set<String> refStrsTypeless = new HashSet<String>();
		
		Set<Integer> refFrom = new HashSet<Integer>();
		Set<Integer> refTo = new HashSet<Integer>();
		Nodes refNodes = refElem.query(".//ne");
		for(int i=0;i<refNodes.size();i++) {

			//uncomment to analyse only a single annotation type
			
//			System.err.println(refNodes.get(i).toXML());
//			if (!((Element)refNodes.get(i)).getAttribute("type").getValue().equals("PM")) {
//
//				System.err.println("skipping");
//				continue;
//			}
			
			Element e = (Element)refNodes.get(i);
			e.removeAttribute(e.getAttribute("xtid"));
			String ev = e.getValue();
			refStrs.add(e.toXML().replaceAll("xtid=\".*?\"", ""));
			refStrsTypeless.add(e.toXML().replaceAll("xtid=\".*?\"", "").replaceAll("type=\".*?\"", ""));
			if(ev.contains("-") || ev.contains(",") || ev.contains("(") || ev.contains(")")) {
				ss.incFalsenegWithPunctuation();
			}
			int start = Integer.parseInt(e.getAttributeValue("xtspanstart"));
			refFrom.add(start);
			int end = Integer.parseInt(e.getAttributeValue("xtspanend"));
			refTo.add(end);
			String type = e.getAttributeValue("type");
			refChars.set(start, "B-" + type);
			for(int j=start+1;j<end;j++) refChars.set(j, "I-" + type);
		}
				
		int rsSize = refStrs.size();
		
		Nodes testNodes = testElem.query(".//ne");
		Set<Integer> testFrom = new HashSet<Integer>();
		Set<Integer> testTo = new HashSet<Integer>();
		for(int i=0;i<testNodes.size();i++) {
			

			//uncomment to analyse only a single annotation type
			
//			System.err.println(testNodes.get(i).toXML());
//			if (!((Element)testNodes.get(i)).getAttribute("type").getValue().equals("PM")) {
//
//				System.err.println("skipping");
//				continue;
//			}
			
			Element e = (Element)testNodes.get(i);
			String type = e.getAttributeValue("type");

			int start = Integer.parseInt(e.getAttributeValue("xtspanstart"));
			testFrom.add(start);
			int end = Integer.parseInt(e.getAttributeValue("xtspanend"));
			testTo.add(end);
			testChars.set(start, "B-" + type);
			for(int j=start+1;j<end;j++) testChars.set(j, "I-" + type);

			e.removeAttribute(e.getAttribute("xtid"));
			String ev = e.getValue();
			if("ONT".equals(type)) {
//			if(!"PM".equals(type)) {
				//System.out.ln("ONT: " + ev);
				continue;
			}
			String testStr = e.toXML().replaceAll("xtid=\".*?\"", "");
			String testStrTypeless = testStr.replaceAll("type=\".*?\"", "");
			if(refStrs.contains(testStr)) {
				//System.out.println("True positive " + type + ": " + ev);
				ss.incMatches();
				if(ev.contains("-") || ev.contains(",") || ev.contains("(") || ev.contains(")")) {
					ss.incMatchesWithPunctuation();
					ss.decFalsenegWithPunctuation();
				}
				refStrs.remove(testStr);
			} else {
				if(refStrsTypeless.contains(testStrTypeless)) {
					if(Oscar3Props.getInstance().verbose) System.out.println(testStr + "\t" + refStrs);
				}
				ss.incFalsepos();
				if(refFrom.contains(start) && refTo.contains(end)) {
					if(Oscar3Props.getInstance().verbose) System.out.println("F+T: " + e.getValue());
				}
				//if(ev.matches(".*\\s.*")) System.out.println("Multiword false positive: " + ev);
				//System.out.println("False positive " + type + ": " + ev);
			}
		}
		
		for(String s : refStrs) {
			Document d = new Builder().build(s, "/foo");
			Element e = d.getRootElement();
			//String ev = d.getValue();
			//String type = d.getRootElement().getAttributeValue("type");
			int start = Integer.parseInt(e.getAttributeValue("xtspanstart"));
			int end = Integer.parseInt(e.getAttributeValue("xtspanend"));
			if(testFrom.contains(start) && testTo.contains(end)) {
				if(Oscar3Props.getInstance().verbose) System.out.println("F+Tr: " + e.getValue());
			}
			//if(!sss.matches(".*\\s.*") && !sss.matches(".*[a-z][a-z].*") && sss.matches(".*[A-Z].*")) {
			//	System.out.println("NonWord missing: " + sss);
			//	if(ExtractTrainingData.getIntstance().nonChemicalNonWords.contains(sss)) System.out.println("Argh!");
			//}
			//if(sss.matches(".*\\s.*")) {
			//	System.out.println("Multiword missing: " + sss);
			//}
			//System.out.println("False negative " + type + ": " + ev);
		}
		
		/*for(int i=0;i<str.length();i++) {
			String s = str.substring(i, i+1);
			if(s.matches("\\s")) s = " ";
			System.out.println(s + "\t" + refChars.get(i) + "\t" + testChars.get(i));
		}*/

		int simpleFn = 0;
		
		for(int i=0;i<refNodes.size();i++) {
			Element e = (Element)refNodes.get(i);
			int start = Integer.parseInt(e.getAttributeValue("xtspanstart"));
			int end = Integer.parseInt(e.getAttributeValue("xtspanend"));
			boolean isSimpleFn = true;
			for(int j=start;j<end;j++) {
				if(!testChars.get(j).equals("O")) {
					isSimpleFn = false;
					break;
				}
			}
			if(isSimpleFn) {
				//System.out.println("SimpleFN: " + e.getValue());
				simpleFn++;
			}
		}

		int simpleFp = 0;
		
		for(int i=0;i<testNodes.size();i++) {
			Element e = (Element)testNodes.get(i);
			int start = Integer.parseInt(e.getAttributeValue("xtspanstart"));
			int end = Integer.parseInt(e.getAttributeValue("xtspanend"));
			boolean isSimpleFp = true;
			for(int j=start;j<end;j++) {
				if(!refChars.get(j).equals("O")) {
					isSimpleFp = false;
					break;
				}
			}
			if(isSimpleFp) {
				//System.out.println("SimpleFP: " + e.getValue());
				simpleFp++;
			}
		}

		ss.simpleFn = simpleFn;
		ss.simpleFp = simpleFp;
		
		//System.out.println(refStrs);
		ss.setFalsenegViaPossibleMatches(rsSize);

		if(charScore) {
			int tp = 0;
			int fp = 0;
			int fn = 0;
			for(int i=0;i<str.length();i++) {
				//String s = str.substring(i, i+1);
				//if(s.matches("\\s")) s = " ";
				if(refChars.get(i).equals("O")) {
					if(!testChars.get(i).equals("O")) {
						fp++;
					}
				} else if(testChars.get(i).equals("O")) {
					fn++;
				} else if(refChars.get(i).equals(testChars.get(i))) {
					tp++;
				} else {
					fp++; fn++;
				}
				//System.out.println(s + "\t" + refChars.get(i) + "\t" + testChars.get(i) + "\t" + tp + "\t" + fp + "\t" + fn);
			}
			ss.falseneg = fn;
			ss.falsepos = fp;
			ss.matches = tp;
		}

		
		return ss;
	}
}
