package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.cam.ch.wwmm.oscar3.sciborg.ResultDbReader;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;


public class RMRSwithNE {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		ResultDbReader	rdbr = ResultDbReader.getInstance();
		Map<Integer,String> paperIdToPath = new HashMap<Integer, String>();
		Map<String,Document> pathToSaf = new HashMap<String, Document>();
		Map<String,Document> pathToNewSaf = new HashMap<String, Document>();
		
		for(int i=0;i<30000;i++) {
			Document rmrsDoc = rdbr.getRmrs(i);
			if(!rmrsDoc.getRootElement().getLocalName().equals("error")) {
				Serializer ser;
				ser = new Serializer(System.out);
				//ser.setIndent(2);
				ser.write(rmrsDoc);				
				int sentenceId = Integer.parseInt(rmrsDoc.getRootElement().getAttributeValue("sentenceid"));
				Document sentDoc = rdbr.getSentence(sentenceId);
				ser = new Serializer(System.out);
				ser.setIndent(2);
				ser.write(sentDoc);
				String sentContent = sentDoc.query("/sentence/@content").get(0).getValue();
				Nodes neNodes = sentDoc.query("/sentence/ne");
				Map<String,List<String>> neMap = new HashMap<String,List<String>>();
				for (int j=0;j<neNodes.size();j++) {
					Element e = (Element)neNodes.get(j);
					String fromto = e.getAttributeValue("cfrom") + ":" + e.getAttributeValue("cto");
					String safid = e.getAttributeValue("safid").substring(1);
					if(!neMap.containsKey(fromto)) neMap.put(fromto, new ArrayList<String>());
					neMap.get(fromto).add(safid);
				}
				System.out.println(neMap);
				int docId = Integer.parseInt(sentDoc.query("/sentence/@docid").get(0).getValue());
				if(!paperIdToPath.containsKey(docId)) {
					Document paperDoc = rdbr.getPaperInfo(docId);
					paperIdToPath.put(docId, paperDoc.query("/paper/@oscarpath").get(0).getValue());
				}
				if(!pathToSaf.containsKey(paperIdToPath.get(docId))) {
					pathToSaf.put(paperIdToPath.get(docId), new Builder().build(new File(paperIdToPath.get(docId), "saf.xml")));
					File newSaf = new File(new File("/usr/groups/sciborg/texts/ne_annotated_210409/", new File(paperIdToPath.get(docId)).getName()), "saf.xml");
					pathToNewSaf.put(paperIdToPath.get(docId), new Builder().build(newSaf));
				}
				Document saf = pathToSaf.get(paperIdToPath.get(docId));
				Document newSaf = pathToNewSaf.get(paperIdToPath.get(docId));
				System.out.println(paperIdToPath.get(docId));
				//for(String fromto : neMap.keySet()) {
				//	for(String id : neMap.get(fromto)) {
				//		System.out.println(saf.query("/saf/annot[@id='" + id + "']").get(0).toXML());
				//	}
				//}
				//new File(new File("/usr/groups/sciborg/texts/ne_annotated_210409/", new File(paperIdToPath.get(docId)).getName()), "saf.xml");
				Nodes epNodes = rmrsDoc.query("/rmrs/rmrs/ep");
				for(int j=0;j<epNodes.size();j++) {
					Element e = (Element)epNodes.get(j);
					String fromto = e.getAttributeValue("cfrom") + ":" + e.getAttributeValue("cto");
					if(neMap.containsKey(fromto)) {
						String id = neMap.get(fromto).get(0);
						Element annot = (Element)saf.query("/saf/annot[@id='" + id + "']").get(0);
						System.out.println(annot.toXML());
						String xpfrom = annot.getAttributeValue("from");
						String xpto = annot.getAttributeValue("to");
						Nodes newAnnotNodes = newSaf.query("/saf/annot[@from='" + xpfrom + "'][@to='" + xpto + "']");
						System.out.println(xpfrom + "\t" + xpto + "\t" + newAnnotNodes.size());
						if(newAnnotNodes.size() > 0) {
							String oid = null;
							String ooid = null;
							for(int k=0;k<newAnnotNodes.size();k++) {
								String s = ((Element)newAnnotNodes.get(k)).getAttributeValue("id");
								System.out.println(newAnnotNodes.get(k).toXML());
								System.out.println(newAnnotNodes.get(k).query("slot[@name='type'][text()='ONT']").size());
								if(newAnnotNodes.get(k).query("slot[@name='type'][text()='ONT']").size() > 0) {
									ooid = s;
								} else {
									oid = s;
								}
							}
							if(oid != null) {
								e.addAttribute(new Attribute("oscarid", oid));
							} else if(ooid != null) {
								e.addAttribute(new Attribute("oscarid", ooid));
							}
						}
						//for(String id : neMap.get(fromto)) {
						//	e.appendChild(saf.query("/saf/annot[@id='" + id + "']").get(0).copy());
						//}
					}
				}
				rmrsDoc.getRootElement().addAttribute(new Attribute("paper", new File(paperIdToPath.get(docId)).getName()));
				rmrsDoc.getRootElement().addAttribute(new Attribute("sentenceContent", sentContent));
				
				//int rmrsId = Integer.parseInt(rmrsDoc.getRootElement().getAttributeValue("id"));
				
				File pf = new File("/local/scratch/ptc24/tmp/rmrs" + (i / 1000));
				if(!pf.exists()) pf.mkdir();
				File f = new File(pf, "rmrs" + i + ".xml");
				ser = new Serializer(new FileOutputStream(f));
				//ser.setIndent(2);
				ser.write(rmrsDoc);				

				System.out.println();
			}
		}

	}

}
