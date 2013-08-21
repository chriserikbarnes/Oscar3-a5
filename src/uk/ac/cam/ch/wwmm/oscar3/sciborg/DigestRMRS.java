package uk.ac.cam.ch.wwmm.oscar3.sciborg;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Serializer;

public class DigestRMRS {

	public static long totalTime = 0;
	
	public static Set<String> rargnames = new HashSet<String>();

	Document doc;
	List<EP> eps;
	
	public DigestRMRS(String rmrs) {
		try {
			eps = new ArrayList<EP>();
			long time = System.currentTimeMillis();
			doc = new Builder().build(new StringReader(rmrs));
			//totalTime += System.currentTimeMillis() - time;

			//Serializer ser = new Serializer(System.out);
			//ser.write(doc);
			
			//Nodes n = doc.query("/rmrs/ep");
			Elements ee;
			
			ee = doc.getRootElement().getChildElements("rarg");
			Map<String,String> cargs = new HashMap<String,String>();
			for(int i=0;i<ee.size();i++) {
				Element e = ee.get(i);
				String rargname = e.getFirstChildElement("rargname").getValue();
				if("CARG".equals(rargname)) {
					Element labelElem = e.getFirstChildElement("label");
					if(labelElem != null) {
						String s = labelElem.getAttributeValue("vid");
						if(s != null) cargs.put(s, e.getFirstChildElement("constant").getValue());
					}					
				}
			}
			//System.out.println(cargs);
			
			ee = doc.getRootElement().getChildElements("ep");
			for(int i=0;i<ee.size();i++) {
				Element e = ee.get(i);
				Element realpred = e.getFirstChildElement("realpred");
				EP ep = new EP();
				if(realpred != null) {
					ep.lemma = realpred.getAttributeValue("lemma");
					ep.pos = realpred.getAttributeValue("pos");
					ep.sense = realpred.getAttributeValue("sense");
				} else {
					Element gpredElem = e.getFirstChildElement("gpred");
					if(gpredElem != null) {
						ep.gpred = gpredElem.getValue();
					}
				}
				
				String label = null;
				Element anchor = e.getFirstChildElement("anchor");
				if(anchor != null) label = anchor.getAttributeValue("vid");
				if(label == null) label = e.getFirstChildElement("label").getAttributeValue("vid");
				if(label != null) {
					ep.carg = cargs.get(label);
				}
				
				//String label = e.query("label/@vid").get(0).getValue();
				//Nodes anodes = e.query("anchor/@vid");
				//if(anodes.size() == 1) label = anodes.get(0).getValue();
				//Nodes nn = doc.query("/rmrs/rarg[rargname/.='CARG'][label/@vid='" + label + "']/constant");
				//if(nn.size() == 1) {
				//	ep.carg = nn.get(0).getValue();
				//}
								
				/*nn = doc.query("/rmrs/rarg[label/@vid='" + label + "']");
				Map<String,String> m = new HashMap<String,String>();
				for(int j=0;j<nn.size();j++) {
					String rargname = ((Element)nn.get(j)).getFirstChildElement("rargname").getValue();
					//System.out.println(rargname);
					rargnames.add(rargname);
					if(!"CARG".equals(rargname)) {
						String val = ((Element)nn.get(j)).getFirstChildElement("var").getAttributeValue("sort") + ((Element)nn.get(j)).getFirstChildElement("var").getAttributeValue("vid");
						m.put(rargname, val);						
					}
				}*/
				
				//String var = e.query("var/@sort").get(0).getValue() + e.query("var/@vid").get(0).getValue();
				ep.cfrom = Integer.parseInt(e.getAttributeValue("cfrom"));
				ep.cto = Integer.parseInt(e.getAttributeValue("cto"));
				//System.out.println(ep + "\t" + label + "\t" + var + "\t" + m);
				//System.out.println(ep);
				eps.add(ep);
				
			}
			totalTime += System.currentTimeMillis() - time;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<EP> getEps() {
		return eps;
	}
	
}
