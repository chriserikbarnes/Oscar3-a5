package uk.ac.cam.ch.wwmm.oscar3.flow;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Document;
import nu.xom.Element;
import sun.misc.Lock;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** This runs Oscar modules on SciXML Documents. You put the SciXML in with the
 * constructor, call methods to run the modules, and then use getter methods to
 * get various Documents back.
 * @author ptc24
 *
 */
public final class OscarFlow {

	private Document sourceXML;
	private Document inlineXML;
	private Document safXML;
	private Document geniaSAF;
	private Document relationXML;
	private Document dataXML;
	private Map<String,Object> customDict;
	private Map<String,ByteArrayOutputStream> customOutput;
	private static ReentrantLock flowLock = new ReentrantLock();
	
	/**Constructs an OscarFlow object for a given SciXML document.
	 * 
	 * @param sourceXML The SciXML document to analyse.
	 */
	public OscarFlow(Document sourceXML) {
		this.sourceXML = sourceXML;
		inlineXML = new Document((Element)XOMTools.safeCopy(sourceXML.getRootElement()));
		safXML = new Document(new Element("saf"));
		geniaSAF = null;
		relationXML = null;
		dataXML = null;
		customDict = new HashMap<String,Object>();
		customOutput = new HashMap<String,ByteArrayOutputStream>();
	}
		
	/**Runs a series of OscarFlow commands.
	 * 
	 * @param flow The commands to run, separated by spaces.
	 * @throws Exception
	 */
	public void runFlow(String flow) throws Exception {
		try {
			flowLock.lock();
			FlowRunner.getInstance().runFlow(this, flow);
		} finally {
			flowLock.unlock();
		}
	}
		
	/**Runs the OscarFlow specified by <tt>processFull</tt> in the
	 * Oscar3 properties file. 
	 * 
	 * @throws Exception
	 */
	public void processFull() throws Exception {
		runFlow("data " + Oscar3Props.getInstance().oscarFlow);
	}

	/**Runs the OscarFlow specified by <tt>processLite</tt> in the
	 * Oscar3 properties file. 
	 * 
	 * @throws Exception
	 */
	public void processLite() throws Exception {
		runFlow(Oscar3Props.getInstance().oscarFlow);
	}

	private static String getSAFFlowCommands(String flow) {
		Pattern p = Pattern.compile("\\binline\\b", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(flow);
		return m.replaceAll("");
	}
	
	/**Runs the OscarFlow specified by <tt>processSAF</tt> in the
	 * Oscar3 properties file. 
	 * 
	 * @throws Exception
	 */
	public void processToSAF() throws Exception {
		String flow = Oscar3Props.getInstance().oscarFlow;
		flow = getSAFFlowCommands(flow);
		runFlow(flow);
	}
	
	/**Runs a short OscarFlow with only the <tt>data</tt> command.
	 * 
	 * @throws Exception
	 */
	public void parseData() throws Exception {
		runFlow("data");
	}		
	
	/**Gets the source XML from the OscarFlow object.
	 * 
	 * @return The source XML from the OscarFlow object.
	 */
	public Document getSourceXML() {
		return sourceXML;
	}
	
	/**Gets the inline XML from the OscarFlow object.
	 * 
	 * @return The inline XML, if present, otherwise null.
	 */
	public Document getInlineXML() {
		return inlineXML;
	}
	
	/**Gets the Genia SAF XML from the OscarFlow object.
	 * 
	 * @return The Genia SAF XML, if present, otherwise null.
	 */
	public Document getGeniaSAF() {
		return geniaSAF;
	}
	
	/**Gets the SAF XML from the OscarFlow object.
	 * 
	 * @return The SAF XML, if present, otherwise null.
	 */
	public Document getSafXML() {
		return safXML;
	}
	
	/**Gets the relations XML (experimental) from the OscarFlow object.
	 * 
	 * @return The relations XML, if present, otherwise null.
	 */
	public Document getRelationXML() {
		return relationXML;
	}
	
	/**Gets the inline data XML from the OscarFlow object.
	 * 
	 * @return The inline data XML, if present, otherwise null.
	 */
	public Document getDataXML() {
		return dataXML;
	}
	
	/**Sets the source XML for the OscarFlow object.
	 * 
	 * @param sourceXML The source XML document.
	 */
	public void setSourceXML(Document sourceXML) {
		this.sourceXML = sourceXML;
	}
	
	/**Sets the inline XML for the OscarFlow object.
	 * 
	 * @param inlineXML The inline XML document.
	 */
	public void setInlineXML(Document inlineXML) {
		this.inlineXML = inlineXML;
	}
	
	/**Sets the Genia SAF XML for the OscarFlow object.
	 * 
	 * @param geniaSAF The Genia SAF XML.
	 */
	public void setGeniaSAF(Document geniaSAF) {
		this.geniaSAF = geniaSAF;
	}
	
	/**Sets the SAF XML for the OscarFlow object.
	 * 
	 * @param safXML The SAF XML.
	 */
	public void setSafXML(Document safXML) {
		this.safXML = safXML;
	}
	
	/**Experimental: sets the relations XML for the OscarFlow object.
	 * 
	 * @param relationXML The relations XML.
	 */
	public void setRelationXML(Document relationXML) {
		this.relationXML = relationXML;
	}
	
	/**Sets the experimental data XML for the OscarFlow object.
	 * 
	 * @param dataXML The data XML.
	 */
	public void setDataXML(Document dataXML) {
		this.dataXML = dataXML;
	}
	
	/**Gets a Map<String, Object>, to which data can be written and from which
	 * it can be read.
	 * 
	 * @return The custom dictionary.
	 */
	public Map<String, Object> getCustomDict() {
		return customDict;
	}
	
	/**Gets the names of all of the custom output streams used.
	 * 
	 * @return The names of the custom output streams.
	 */
	public Set<String> getCustomOutputNames() {
		return customOutput.keySet();
	}
	
	/**Make an OutputStream, to which data can be written. The data written
	 * to these streams will be written to disk if Oscar3 is run over a
	 * directory, and these outputs can also be accessed from the web server.
	 * 
	 * @param name The custom output stream name.
	 * @return The custom output stream.
	 * @throws Exception
	 */
	public OutputStream customOutputStream(String name) throws Exception {
		if(name == null) throw new Exception("Null name for custom output stream");
		if(customOutput.containsKey(name)) throw new Exception("Custom output " + name + " already exists");
		if(!name.matches("[A-Za-z0-9_.]+")) throw new Exception("Custom output names may only contain letters, numbers, underscore and dot");
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		customOutput.put(name, baos);
		return baos;
	}
	
	/**As customOutputStream, but as a Writer, writing UTF-8.
	 * 
	 * @param name The custom writer name.
	 * @return The custom writer.
	 * @throws Exception
	 */
	public Writer customWriter(String name) throws Exception {
		OutputStream os = customOutputStream(name);
		Writer w = new OutputStreamWriter(os, "UTF-8");
		return w;
	}
	
	/**As customWriter, but as a PrintWriter.
	 * 
	 * @param name The custom PrintWriter name.
	 * @return The custom PrintWriter.
	 * @throws Exception
	 */
	public PrintWriter customPrintWriter(String name) throws Exception {
		return new PrintWriter(customWriter(name));
	}
	
	/**Gets an InputStream from which data written to the OutputStream of the
	 * same name can be read.
	 * 
	 * @param name The stream name.
	 * @return The InputStream.
	 * @throws Exception
	 */
	public InputStream customInputStream(String name) throws Exception {
		if(!customOutput.containsKey(name)) throw new Exception("No output by the name " + name + " in the OscarFlow");
		ByteArrayInputStream bais = new ByteArrayInputStream(customOutput.get(name).toByteArray());
		return bais;
	}
	
	/**As customInputStream, but gets a Reader, reading UTF-8.
	 * 
	 * @param name The stream name.
	 * @return The Reader.
	 * @throws Exception
	 */
	public Reader customReader(String name) throws Exception {
		return new InputStreamReader(customInputStream(name), "UTF-8");
	}
	
	/**As customReader, but gets a BufferedReader.
	 * 
	 * @param name The stream name.
	 * @return The BufferedReader.
	 * @throws Exception
	 */
	public BufferedReader customBufferedReader(String name) throws Exception {
		return new BufferedReader(customReader(name));
	}
	
	/**Takes the data written to a customOutputStream (or (Print)Writer), and
	 * writes that data to another output stream. The output stream that gets
	 * written to will not be closed; you should close it yourself, unless
	 * you want to write further data to it.
	 * 
	 * @param name The custom stream name.
	 * @param os The output stream to write to.
	 * @throws Exception
	 */
	public void writeCustomeOutputToStream(String name, OutputStream os) throws Exception {
		InputStream is = customInputStream(name);
		byte[] buffer = new byte[1024];
		int i = 0;
		while ((i = is.read(buffer)) != -1) {
			os.write(buffer, 0, i);
		}
		is.close();
	}
	
}
