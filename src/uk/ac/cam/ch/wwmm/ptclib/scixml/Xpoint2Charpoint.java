package uk.ac.cam.ch.wwmm.ptclib.scixml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;

/**Produces a mapping from XPoint to character offsets. These are offsets in
 * the serialized XML document, rather than in the value of the root element.
 * 
 * @author ptc24, C. J. Rupp
 *
 */
public class Xpoint2Charpoint extends DefaultHandler {

	// our SAX parser
	private static SAXParser xr;
	private static Locator dl;

	private int line_offset=0;
	private int offset=0;
	private Stack<Integer> nodes;
	private int current_node;
	private boolean in_text;
	private Map<String,Integer> pointMap;
	
	/**Produce a mapping from XPoint to character offset.
	 * 
	 * @param file The SciXML file to analyse.
	 * @return The mapping.
	 * @throws Exception
	 */
	public static Map<String,Integer> parseFile(File file) throws Exception {
		InputSource filein = new InputSource(new FileInputStream(file));
		filein.setEncoding("UTF-8");
		Xpoint2Charpoint xpc = new Xpoint2Charpoint();
		xpc.initOffset(file);
		xr.parse(filein, xpc);
		return xpc.pointMap;
	}
	
	/**Produce a mapping from XPoint to character offset.
	 * 
	 * @param file The SciXML file to analyse.
	 * @return The mapping.
	 * @throws Exception
	 */
	public static Map<String,Integer> parseFile(String file) throws Exception {
		return parseFile(new File(file));
	}

	private Xpoint2Charpoint() {
		super();
		try {
			if(xr == null) xr = (SAXParserFactory.newInstance()).newSAXParser();    		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		pointMap = new HashMap<String,Integer>();
	}

	/**Overloaded, should be protected.
	 * 
	 */
	public void setDocumentLocator(Locator locator) {
		dl = locator;
	}

	private void initOffset(File file) {
		offset = 0;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			offset = 0;
			while(true) {
				char c = (char)br.read();
				if(c == '\n') break;
				offset++;
			}
			br.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		System.out.println(offset);
	}
	
	/**Overloaded, should be protected.
	 * 
	 */
	public void startDocument() {			
		// initialise variables
		//offset += 38; // magic

		/* more makumba */
		/*String fname = dl.getSystemId();
	fname = fname.replaceFirst("file:\\/\\/", "");
	String fcmd = "head -1 "+fname;
	String fresult = Para2Sent.exec2string(fcmd);
	if(fresult.indexOf("\r")>=0){
		offset++;
	}*/

		line_offset += 0;
		nodes = new Stack<Integer>();
		current_node = 0;
		in_text = false;
	}

	/**Overloaded, should be protected.
	 * 
	 */
	public void endDocument() {
		// close batch insert (and so update DB)
		try {

		} catch (Exception e) {
			// FIXME it would be nice to abort here
			throw new RuntimeException(e);
		}

		// ?
		offset+=line_offset;
		line_offset = 0;
	}

	/**Overloaded, should be protected.
	 * 
	 */
	public void startElement(String uri, String name, String qname, Attributes atts ) {
		//		int old_offset = offset+line_offset;
		//line_offset = dl.getColumnNumber();

		// if we were in a text node it is now at an end
		if(in_text){
			current_node = nodes.pop();
			in_text = false;

			assertXpoint(xpt2string(nodes)+"."+current_node,offset+line_offset);
		}

		line_offset = dl.getColumnNumber();


		// new node
		nodes.push(++current_node);
		current_node = 0;
		//		System.err.println("<"+qname+">\t"+xpt2string(nodes)+"."+current_node+"\t"+(offset+line_offset));


		// store xpoint/charpoint pair
		//assertXpoint("START"+xpt2string(nodes)+"."+current_node,offset+line_offset);
		assertXpoint(xpt2string(nodes)+"."+current_node,offset+line_offset);
		/*		{
			String xpoint=xpt2string(nodes)+"."+current_node;
			int charpoint=offset+line_offset;
			batchInsert.insertPointmap(xpoint,charpoint); // call batchInsert
			System.err.println("<"+qname+">\t"+xpoint+"\t"+(charpoint));
			}*/
	}

	/**Overloaded, should be protected.
	 * 
	 */
	public void endElement(String uri, String name, String qname) {
		// int old_offset = offset + line_offset;	
		// store xpoint/charpoint pair

		// if we were in a text node it is now at an end
		if(in_text){
			current_node = nodes.pop();
			in_text = false;
			//BMW
			assertXpoint(xpt2string(nodes)+"."+current_node,offset+line_offset);
			//assertXpoint("ENDTEXT"+xpt2string(nodes)+"."+current_node,offset+line_offset);
			line_offset = dl.getColumnNumber();	
		}


		current_node = nodes.pop();
		//BMW
		//	assertXpoint(xpt2string(nodes)+"."+current_node,offset+line_offset);
		//assertXpoint("END"+xpt2string(nodes)+"."+current_node,offset+line_offset);
		line_offset = dl.getColumnNumber();	
		//assertXpoint("END"+xpt2string(nodes)+"."+current_node,offset+line_offset);
		assertXpoint(xpt2string(nodes)+"."+current_node,offset+line_offset);

	}

	/**Overloaded, should be protected.
	 * 
	 */
	public void characters(char ch[], int start, int length) {

		String node_prefix;

		// if we weren't in a text node, had better start one
		if(!in_text){
			in_text = true;	
			nodes.push(++current_node);
			current_node = 0;
			// System.err.println("<Text Node>\t"+xpt2string(nodes)+"."+current_node+"\t"+(offset+line_offset));
			node_prefix = xpt2string(nodes);
			assertXpoint(node_prefix+"."+current_node,offset+line_offset);
		}		
		else
			node_prefix = xpt2string(nodes);
		// store xpoint/charpoint pair for zeroth point in text node

		//assertXpoint("CHAR0"+node_prefix+"."+current_node,offset+line_offset);

		String char_esc;
		// System.err.print("Chars:\t"+xpt2string(nodes)+"."+current_node+"\t"+(offset+line_offset));

		//String foo;
		// iterate through characters
		for (int i=start; i<start+length; i++) {
			current_node++;
			if(ch[i]=='\n'){
				if(line_offset==1){
					//foo="A1";
					offset++;
				}
				else {
					//foo="A2";
					offset += line_offset;
				}
				line_offset = 1;				
			}
			else {
				//foo="B";
				// BMW this looks unsafe: we don't know whether characters were escaped or not in the raw file
				char_esc = xml_char_escape(ch[i]);
				line_offset+=char_esc.length();
				// line_offset++
			}

			// store xpoint/charpoint pair (point immediately AFTER character)
			assertXpoint(node_prefix+"."+current_node,offset+line_offset);
			//assertXpoint("CHARi"+node_prefix+"."+current_node,offset+line_offset);
		}

		// consitency check
		if(line_offset!=dl.getColumnNumber()){
			// FIXME would be nice to throw exception
			System.err.println("Inconsistent character count:\t"+(offset+line_offset)+"/"+(offset+dl.getColumnNumber()));
		}

		//...
		line_offset = dl.getColumnNumber();
		// System.err.println("\t"+xpt2string(nodes)+"."+current_node+"\t"+(offset+line_offset));
	}

	// serialize xpoint as string
	private String xpt2string(Stack xpt){
		String result = "";
		for(int i=0; i<xpt.size();i++){
			result += "/"+xpt.get(i);
		}
		return result;
	}

	// place <xpoint,charpoint> in SAF database
	private void assertXpoint(String xpoint, int charpoint) {
		pointMap.put(xpoint, charpoint);
	}

	private static String xml_char_escape(char raw){
		switch(raw){
		case '<' : { 
			return "&lt;";
		}
		case '>' : { 
			return "&gt;";
		}
		/*              case '\'' : { 
                return "&apos;";
                }*/
		case '&' : { 
			return "&amp;";
		}
		case '\"' : { 
			return "&quot";
		}
		default: {
			return ""+raw;
		}
		}
	}

	/*public static void main(String[] args) throws Exception {
		long time = System.currentTimeMillis();
		//parseFile("/home/ptc24/sciborg/big_scixml/b200198e.xml");
		Map<String,Integer> m = parseFile("/home/ptc24/sciborg/big_scixml/b200198e.xml");
		System.out.println(System.currentTimeMillis() - time);
		for(String s : m.keySet()) {
			System.out.println(s + "\t" + m.get(s));
		}
		//parseFile("/home/ptc24/tmp/b203484k.xml");
	}*/

}
