package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ProcessingInstruction;
import nu.xom.Serializer;
import nu.xom.Text;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.ScrapBook;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;

public class XMLTreeFP {

	public static String getNodePrint(Node n) {
		if(n instanceof Text) {
			return ("T");
		} else if (n instanceof ProcessingInstruction) {
			return ("P");
		} else if (n instanceof Element) {
			Element e = (Element)n;
			if(e.getLocalName().equals("REF") || e.getLocalName().equals("PUBREF")) {
				return "R";
			} else if(e.getLocalName().equals("REFERENCELIST")) {
				return "L";
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append("E(");
				for(int i=0;i<e.getChildCount();i++) {
					sb.append(getNodePrint(e.getChild(i)));
				}
				sb.append(")");
				return sb.toString();				
			}
		} else {
			return "O";
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/tmp/handRetargeted/"), "scrapbook.xml");
		/*for(File f : files) {
			System.out.println(f);
			Document doc = new Builder().build(f);
			String xs1 = getNodePrint(doc.getRootElement());
			f = new File(f.getParentFile(), "newsource.xml");
			doc = new Builder().build(f);
			String xs2 = getNodePrint(doc.getRootElement());
			System.out.println(xs1.equals(xs2));
		}*/
		
		for(File f : files) {
			if(f.toString().contains("default")) continue;
			Document doc = new Builder().build(f);
			Nodes refNodes = doc.query("//REF");
			for(int i=0;i<refNodes.size();i++) {
				Element e = (Element)refNodes.get(i);
				e.setLocalName("PUBREF");
				e.addAttribute(new Attribute("TEXT", e.getValue()));
				while(e.getChildCount() > 0) {
					e.getChild(0).detach();
				}
			}
			/*Serializer ser = new Serializer(System.out);
			ser.setIndent(2);
			ser.write(doc);*/
			Serializer ser = new Serializer(new FileOutputStream(f));
			ser.write(doc);
			ScrapBook sb = new ScrapBook(f.getParentFile());
			sb.makePaper();
		}
		
	}

}
