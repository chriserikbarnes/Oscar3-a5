package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Nodes;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SAFToInline;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class CombinedCorpus {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Set<String> noCopySlots = new HashSet<String>(StringTools.arrayToList(new String[] {
			"surface", "type", "confidence", "blocked"	
		}));
		
		
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/tmp/combined2"), "source.xml");
		for(File f : files) {
			Document source = new Builder().build(f);
			Document saf = new Builder().build(new File(f.getParent(), "saf.xml"));
			Document autoSaf = new Builder().build(new File(new File("/home/ptc24/tmp/autoannot_for_combine", f.getParentFile().getName()), "saf.xml"));
			Nodes n;
			
			n = saf.query("/saf/annot");
			for(int i=0;i<n.size();i++) {
				Element e = (Element)n.get(i);
				String from = e.getAttributeValue("from");
				String to = e.getAttributeValue("to");
				String type = e.query("slot[@name='type']").get(0).getValue();
				//System.out.println(e.toXML());
				Elements ee = e.getChildElements("slot");
				for(int j=0;j<ee.size();j++) {
					if(ee.get(j).getAttributeValue("name").equals("tag")) ee.get(j).detach();
				}
				//System.out.println(e.toXML());
				
				//System.out.println(from + " " + to + " " + type);
				Nodes nn = autoSaf.query("/saf/annot[@from='" + from + "'][@to='" + to + "'][slot[@name='type']/.='" + type + "']");
				if(nn.size() == 1) {
					//System.out.println(nn.get(0).toXML());
					ee = ((Element)nn.get(0)).getChildElements("slot");
					for(int j=0;j<ee.size();j++) {
						Element eee = ee.get(j);
						String name = eee.getAttributeValue("name");
						if(!noCopySlots.contains(name)) {
							eee.detach();
							e.appendChild(eee);
						}
					}
					//System.out.println(e.toXML());
				} else if(nn.size() > 1) {
					throw new Exception();
				}
				//System.out.println();
			}
			
			n = autoSaf.query("/saf/annot[slot[@name='type']/.='ONT']");
			for(int i=0;i<n.size();i++) {
				Element e = (Element)n.get(i);
				e.detach();
				saf.getRootElement().appendChild(e);
			}
			SafTools.numberSaf(saf, "oscar", "o");
			
			Serializer ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.write(saf);
			
			Document markedup = new Document(source);
			markedup = SAFToInline.safToInline(saf, markedup, source, false);
			new Serializer(System.out).write(markedup);
			
			ser = new Serializer(new FileOutputStream(new File(f.getParentFile(), "saf.xml")));
			ser.write(saf);
			ser = new Serializer(new FileOutputStream(new File(f.getParentFile(), "markedup.xml")));
			ser.write(markedup);
			
			System.out.println();
		}
	}

}
