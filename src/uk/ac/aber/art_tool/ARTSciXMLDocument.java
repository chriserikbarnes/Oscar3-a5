package uk.ac.aber.art_tool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParentNode;
import nu.xom.ProcessingInstruction;

import org.json.JSONException;
import org.json.JSONObject;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

/** A subclass of Document, contains methods for generating SciXML and extensions.
 * 
 * @author Maria Liakata
 * @author ptc24
 *
 */
public class ARTSciXMLDocument extends Document {
	
	public static ARTSciXMLDocument makeFromDoc(Document doc) {
		Element dummy = new Element("dummy");
		Element root = doc.getRootElement();
		doc.setRootElement(dummy);
		return new ARTSciXMLDocument(root);
	}
	
	public ARTSciXMLDocument(Element arg0) {
		super(arg0);
	}
	
	public void removeProcessingInstructions() {
		Nodes n = query("processing-instruction()");
		for(int i=0;i<n.size();i++) {
			n.get(i).detach();
		}
	}
	
	//change to addMode1PI for both kinds of server, see below
	public void addServerProcessingInstructions() {
		removeProcessingInstructions();
		ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/toHTMLJS.xsl\"");
		insertChild(pi, 0);
		pi = new ProcessingInstruction("jarpath", "/");
		insertChild(pi, 1);
		pi = new ProcessingInstruction("host", Oscar3Props.getInstance().hostname + ":8181");
		insertChild(pi, 2);
		pi = new ProcessingInstruction("viewer", "picture");
		insertChild(pi, 3);
	}
	
	public void addMode2OSCARPI() {
		removeProcessingInstructions();
		ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/art_tool_web/xsl/mode2.xsl\""); //works for oscar version
		//ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/xsl/mode2.xsl\"");
		insertChild(pi, 0);
		pi = new ProcessingInstruction("jarpath", "/");
		insertChild(pi, 1);
		pi = new ProcessingInstruction("host", Oscar3Props.getInstance().hostname + ":8181");
		insertChild(pi, 2);
	}
	
	public void addMode2SapientPI() {
		removeProcessingInstructions();
		//ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/art_tool_web/xsl/mode2.xsl\""); //works for oscar version
		ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"/xsl/mode2.xsl\"");
		insertChild(pi, 0);
		pi = new ProcessingInstruction("jarpath", "/");
		insertChild(pi, 1);
		pi = new ProcessingInstruction("host", Oscar3Props.getInstance().hostname + ":8181");
		insertChild(pi, 2);
	}
	
	
	
	public void updateMode2XML(JSONObject conceptJSON,JSONObject subtypeJSON) {
		//list of Nodes which are the sentences
		//Nodes n = query("//s");
		//System.out.println(n.get(0));
		//find by attribute (sid) for all the relevant ones in the JSON objects
		Iterator<String> conceptIterator = (Iterator<String>)conceptJSON.keys(); //This may need to be updated if the JSONObject class changes
		Iterator<String> subtypeIterator = (Iterator<String>)subtypeJSON.keys();
		TreeMap<String,String> conceptMap = new TreeMap<String,String>();
		TreeMap<String,String> subtypeMap = new TreeMap<String,String>();
		try {
			while(conceptIterator.hasNext()) {
				String sid = conceptIterator.next();
				conceptMap.put(sid, conceptJSON.getString(sid));
			}
			while(subtypeIterator.hasNext()) {
				String sid = subtypeIterator.next();
				subtypeMap.put(sid, subtypeJSON.getString(sid));
			}
		} catch(JSONException je) {
			//do nothing...
		}
		Set<String> sids = conceptMap.keySet();
		String qstring = "//s";
		Nodes n = query(qstring);
		//int sentNum = 1;
		int nodesNum=n.size();
		//String sidString = "";
		for(int i = 1; i <= nodesNum; i++) {
			String sentid = ""+i;			
			String thisSentenceQuery = "//s[@sid='" + i + "']";
			Element sentenceElement = (Element)query(thisSentenceQuery).get(0);
			String annotQuery = "//s[@sid='" + i + "']//annotationART";
			// Remove any annotationART child from the sentenceElement
			
			Nodes annotNodes = query(annotQuery);
			Pattern sTaggedPattern;
			if(annotNodes.size() != 0) {
				 sTaggedPattern = Pattern.compile("^<s sid=\\\"\\d+\\\"><annotationART .+?>(.+)</annotationART></s>$");					
			} else {
				sTaggedPattern = Pattern.compile("^<s sid=\\\"\\d+\\\">(.+)</s>$");
			}
			String sTaggedSentence = sentenceElement.toXML();
			Matcher m = sTaggedPattern.matcher(sTaggedSentence);
			m.find();
			//System.out.println(m.group(0));
			String untaggedSentence = m.group(1);
			//System.out.println("untagged sentence: " + untaggedSentence);
			String unchevronned = untaggedSentence.replace("<", "sapientOpenChevron");
			unchevronned = unchevronned.replace(">", "sapientCloseChevron");
			unchevronned = unchevronned.replace("&", "sapientAmpersand");
			//System.out.println("unchevronned" + unchevronned);
			//System.out.println("sentenceElement XML: " + sentenceElement.toXML());
			if(annotNodes.size() != 0) {
				Node annotNode = annotNodes.get(0);				
				annotNode.detach();
				sentenceElement.appendChild(unchevronned);
			}
				
			if(sids.contains(sentid)) {
				//System.out.println(sentid);
				String ctype = conceptMap.get(sentid);	// ctype is here the conceptiD
				String subtype = subtypeMap.get(sentid);
				if(subtype == null) subtype = "None";
				String novelty = "None";
				String advantage = "None";
				
				if(subtype.equals("New")) {					
					novelty = "New";
				} else if (subtype.equals("Old")) {					
					novelty = "Old";
				} else if (subtype.equals("Advantage") || subtype.equals("Disadvantage")) {		
					System.out.println("The id of an advantage/disadvantage should be: " + sentid);
					if(subtype.equals("Advantage")) {					
						advantage = "Yes";
					} else {						
						advantage = "No";
					}
					// ArrayList<String> idsOfCtype = new ArrayList<String>();
			        ArrayList<Integer> idsOfCtype = new ArrayList<Integer>();
					
					//find another sentence with this conceptID
					for(String sentid2: conceptMap.keySet()) {
						if(conceptMap.get(sentid2).equals(ctype)) {
							int sentid2Int = new Integer(sentid2).intValue();
							idsOfCtype.add(sentid2Int);
						}
					}
					
					// We need to sort the ids with the same conceptID here to make sure the earliest instance
					// is picked. 
					Collections.sort(idsOfCtype);
					
					//find the novelty of that sentence
					for(int id: idsOfCtype) {	
					
						System.out.println("The id where an advantage/disadvantage refers: " + id);
						String noveltyVal = subtypeMap.get(""+ id);						
						if(noveltyVal.equals("New") || noveltyVal.equals("Old")) {							
							novelty = noveltyVal;
							break;
						}
					}					
				}
				//System.out.println("//s[@sid='" + sentid + "']");
				Elements sentenceChildren = sentenceElement.getChildElements();
				for(int j = 0; j < sentenceChildren.size(); j++) {
					//System.out.println(sentenceChildren.get(j));
				}
				ParentNode parent = sentenceElement.getParent();
				int childIndex = parent.indexOf(sentenceElement);
				sentenceElement.detach();
				
				//Elements sentenceContent = sentenceElement.getChildElements();
				//System.out.println("content of sentence: " + sentenceContent);
				ArrayList<Attribute> attArray = new ArrayList<Attribute>();
				attArray.add(new Attribute("atype", "GSC"));
				if(!ctype.equals("None")) {
					///System.out.println("ctype is not None: " + ctype);
					attArray.add(new Attribute("type", ctype.substring(0, 3)));
				} else {
					//System.out.println("ctype is None:" + ctype);
					attArray.add(new Attribute("type", ctype));
				}
				attArray.add(new Attribute("conceptID", ctype));
				attArray.add(new Attribute("novelty", novelty));
				attArray.add(new Attribute("advantage", advantage));
				Element annotationElement = new Element("annotationART");
				for (Attribute a: attArray) {
					annotationElement.addAttribute(a);
				}
				//System.out.println(annotationElement);
				//annotationElement.appendChild("<dummy tag='this'>hello</dummy>");
				//Text textNode = new Text(untaggedSentence);
				annotationElement.appendChild(unchevronned);
				
				/*for(int i = 0; i <sentenceContent.size();i++) {
					sentenceElement.removeChild(sentenceContent.get(i));
					annotationElement.appendChild(sentenceContent.get(i));
					System.out.println("child:" + sentenceContent.get(i).toString());
				}*/
				//sentenceElement.removeChildren();
				
				Element newSentence = new Element("s");
				parent.insertChild(newSentence, childIndex);
				Attribute sidAttr = new Attribute("sid", sentid);
				newSentence.addAttribute(sidAttr);				
				newSentence.appendChild(annotationElement);
				//System.out.println(newSentence.toString());
			}
		}//for all the sentence nodes			
	}//end of method updateMode2XML

}
