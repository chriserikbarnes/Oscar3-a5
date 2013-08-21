package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.Text;
import nu.xom.xslt.XSLTransform;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

public class PatentAnaly {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File f = new File("/home/ptc24/corpora/newpatents/gs39_20080722_single_refs.xml");
		Document doc = new Builder().build(f);
		Serializer ser;
		if(false) {
			ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.write(doc);			
		}
		Nodes n = doc.query("//LINE/*");
		Set<String> nodeNames = new HashSet<String>();
		for(int i=0;i<n.size();i++) {
			Node node = n.get(i);
			if(node instanceof Element) {
				Element e = (Element)node;
				nodeNames.add(e.getLocalName());
			}
		}
		System.out.println(nodeNames);
		
		ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/ptc/experimental/resources/");
		
		XSLTransform xslt = new XSLTransform(rg.getXMLDocument("patents2.xsl"));
		
		Nodes paperNodes = doc.query("/PAPERS/PAPER");
		
		File outFolder = new File("/home/ptc24/newows/corpora/NewSciXMLPatents2");
		if(!outFolder.exists()) outFolder.mkdir();
		
		for(int i=0;i<paperNodes.size();i++) {			
			Node node = paperNodes.get(i);
			node.detach();
			assert(node instanceof Element);
			Element e = (Element)node;
			Document newDoc = new Document(e);
			Document sciXML = XSLTransform.toDocument(xslt.transform(newDoc));
			
			String title = sciXML.query("/PAPER/CURRENT_TITLE").get(0).getValue().trim();
			assert(title.length() > 0);
			title = title.replaceAll(" ", "_");
			System.out.println(title);
			File paperDir = new File(outFolder, title);
			if(!paperDir.exists()) paperDir.mkdir();
			
			Nodes neNodes = sciXML.query("//ne");
			Pattern p = Pattern.compile("(.*\\S+)(\\s+)");
			for(int j=0;j<neNodes.size();j++) {
				assert(neNodes.get(j) instanceof Element);
				String neTxt = neNodes.get(j).getValue();
				System.out.println(neNodes.get(j).toXML());
				Matcher m = p.matcher(neTxt);
				if(m.matches()) {
					Element neElem = (Element)neNodes.get(j);
					Element parent = (Element)neElem.getParent();
					int index = parent.indexOf(neElem);
					neElem.removeChildren();
					neElem.appendChild(m.group(1));
					if(index == parent.getChildCount() - 1) {
						Text textNode = new Text(m.group(2));
						parent.appendChild(textNode);
					} else if(parent.getChild(index+1) instanceof Text) {
						Text nextText = (Text)parent.getChild(index+1);
						nextText.setValue(m.group(2) + nextText.getValue());
					} else {
						parent.insertChild(new Text(m.group(2)), index+1);
						//neElem.removeChildren();
						//neElem.appendChild(m.group(1));
					}
				}
				System.out.println(neNodes.get(j).toXML());				
			}

			ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.write(sciXML);

			File markedupFile = new File(paperDir, "markedup.xml");
			FileOutputStream fos = new FileOutputStream(markedupFile);
			ser = new Serializer(fos);
			ser.write(sciXML);
			fos.close();
			
			Nodes nodes = sciXML.query("//ne");
			for(int j=0;j<nodes.size();j++) {
				XOMTools.removeElementPreservingText((Element)nodes.get(j));
			}
			
			File sourceFile = new File(paperDir, "source.xml");
			fos = new FileOutputStream(sourceFile);
			ser = new Serializer(fos);
			ser.write(sciXML);
			fos.close();
		}
		
	}

}
