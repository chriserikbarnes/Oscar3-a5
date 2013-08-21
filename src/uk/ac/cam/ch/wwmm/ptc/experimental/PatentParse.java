package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.NodeFactory;
import nu.xom.Nodes;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

public class PatentParse extends NodeFactory {

	SciXMLDocument sciDoc;
	
	public Element parsePara(String para) {
		Element e = new Element("P");
		Pattern p = Pattern.compile("<(\\S+|EMI\\s+ID=\\d+\\.\\d+)>");
		int lastEnd = 0;
		Matcher m = p.matcher(para);
		while(m.find()) {
			e.appendChild(para.substring(lastEnd, m.start()));
			String contents = m.group(1);
			if(contents.matches(".*\\s+.*")) {
				Element xref = new Element("XREF");
				xref.addAttribute(new Attribute("TYPE", "EMI-REF"));
				xref.addAttribute(new Attribute("ID", contents.split("\\s+")[1]));
				e.appendChild(xref);
			} else {
				Element sp = new Element("SP");
				sp.appendChild(contents);
				e.appendChild(sp);
			}
			lastEnd = m.end();
		}
		e.appendChild(para.substring(lastEnd));
		XOMTools.normalise(e);
		return e;
	}
	
	@Override
	public Document startMakingDocument() {
		sciDoc = new SciXMLDocument();		
		return super.startMakingDocument();
	}
	
	@Override
	public Nodes finishMakingElement(Element elem) {
		if(elem.getLocalName().equals("document")) {
			File f = new File("/home/ptc24/corpora/patents/" + elem.getAttributeValue("pn"));
			f.mkdir();
			try {
				Serializer ser = new Serializer(new FileOutputStream(new File(f, "source.xml")));
				ser.write(sciDoc);				
			} catch (Exception e) {
				e.printStackTrace();
			}
			sciDoc = new SciXMLDocument();
			System.out.println();
			System.out.println();
			//System.out.println(elem.toXML());
			return new Nodes();
		}
		if(elem.getLocalName().equals("title")) {
			sciDoc.setTitle(elem.getValue());
			return new Nodes();
		}
		if(elem.getLocalName().equals("description")) {
			String desc = elem.getValue();
			Pattern p = Pattern.compile("\\[\\d\\d\\d\\d\\](\\s+\\[\\d+\\])?");
			Matcher m = p.matcher(desc);
			int lastEnd = 0;
			while(m.find()) {
				String pContents = desc.substring(lastEnd, m.start());
				lastEnd = m.end();
				//System.out.println(m.group() + "> " + parsePara(pContents.trim()).toXML());
				sciDoc.getDiv().appendChild(parsePara(pContents.trim()));
			}
			sciDoc.getDiv().appendChild(parsePara(desc.substring(lastEnd).trim()));
			//System.out.println("NONE> " + parsePara(desc.substring(lastEnd)).toXML());
			return new Nodes();
		}
		if(elem.getLocalName().equals("claims")) {
			String claims = elem.getValue();
			Pattern p = Pattern.compile("\\.\\s+(\\d+\\.)");
			Matcher m = p.matcher(claims);
			String lastPoint = "";
			int lastEnd = 0;
			while(m.find()) {
				String pContents = claims.substring(lastEnd, m.start(1));
				lastEnd = m.end(1);
				//System.out.println(lastPoint + pContents);
				sciDoc.getDiv().appendChild(parsePara((lastPoint + pContents).trim()));
			    lastPoint = m.group(1);
			}
			sciDoc.getDiv().appendChild(parsePara((lastPoint + claims.substring(lastEnd)).trim()));
			//System.out.print("NONE> " + parsePara(lastPoint + claims.substring(lastEnd)).toXML());
			return new Nodes();
		}
		// TODO Auto-generated method stub
		return new Nodes(elem);
	}
	
	@Override
	public void finishMakingDocument(Document doc) {
		// TODO Auto-generated method stub
		super.finishMakingDocument(doc);
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		NodeFactory n = new PatentParse();
		Builder b = new Builder(n);
		b.build(new File("/home/ptc24/corpora/patents/oscarC07d-b2-wo2006a.xml"));
	}

}
