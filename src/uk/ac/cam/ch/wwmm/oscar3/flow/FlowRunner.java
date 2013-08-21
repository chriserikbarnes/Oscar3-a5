package uk.ac.cam.ch.wwmm.oscar3.flow;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Document;
import nu.xom.Element;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.dataparse.DataParser;
import uk.ac.cam.ch.wwmm.oscar3.misc.DiscardInDataSection;
import uk.ac.cam.ch.wwmm.oscar3.misc.GeniaProcessor;
import uk.ac.cam.ch.wwmm.oscar3.misc.NewGeniaRunner;
import uk.ac.cam.ch.wwmm.oscar3.misc.SciBorgPostProcess;
import uk.ac.cam.ch.wwmm.oscar3.misc.StructureTypes;
import uk.ac.cam.ch.wwmm.oscar3.misc.XSLOnInline;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.NameRecogniser;
import uk.ac.cam.ch.wwmm.oscar3.subtypes.NESubtypes;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SAFToInline;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/**Runs OscarFlows. The methods here are mainly invoked by OscarFlow - however,
 * the addCommand method is left visible for extensibility.
 * 
 * @author ptc24
 *
 */
public final class FlowRunner {

	private static Pattern flowPattern = Pattern.compile("([A-Za-z0-9_]+)(\\(([^\\s,]+(,\\s+[^\\s,]+)*)\\))?");
	private static Pattern argumentPattern = Pattern.compile("[^\\s,]+");

	private Map<String,FlowCommand> flowCommands;
		
	private static FlowRunner myInstance;
	
	/**Get the FlowRunner singleton instance, creating it afresh if necessary.
	 * 
	 * @return The FlowRunner singleton instance.
	 */
	public static FlowRunner getInstance() {
		if(myInstance == null) myInstance = new FlowRunner();
		return myInstance;
	}

	private FlowRunner() {
		flowCommands = new HashMap<String,FlowCommand>();
		flowCommands.put("recognise", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				NameRecogniser.processDocument(flow.getSourceXML(), flow.getSafXML());
				//NameRecogniser.processDocument(flow.getInlineXML(), flow.getSafXML());		
			}
		});
		flowCommands.put("resolve", new FlowCommand(){
			@SuppressWarnings("unchecked")
			public void call(OscarFlow flow, List<String> args) throws Exception {
				try {
					Class c = Class.forName("uk.ac.cam.ch.wwmm.oscar3.resolver.NameResolver");
					Method m = c.getMethod("parseDoc", new Class[]{Document.class, List.class});
					m.invoke(null, new Object[]{flow.getSafXML(), args});
				} catch (ClassNotFoundException e) {
					System.err.println("The name resolver is not present in this version.");
				} 
			}
		});
		flowCommands.put("genia", new FlowCommand() {
			public void call(OscarFlow flow, List<String> args) throws Exception {
				if(NewGeniaRunner.canRunGenia()) {
					try {
						flow.setGeniaSAF(NewGeniaRunner.getInstance().runGenia(flow.getSourceXML()));
					} catch (Exception e) {
						e.printStackTrace();
						System.err.println("Warning: genia tagger runner failed");
						return;
					}
					if(flow.getGeniaSAF() != null) {
						GeniaProcessor.processGeniaAndOscarSafs(flow.getSourceXML(), flow.getGeniaSAF(), flow.getSafXML());
					}
				}
			}
		});
		flowCommands.put("tidyelements", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				if(flow.getGeniaSAF() != null) {
					GeniaProcessor.removeElementsAtSentenceStart(flow.getSafXML(), flow.getGeniaSAF());
				}
			}
		});
		flowCommands.put("adjusttopos", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				if(flow.getGeniaSAF() != null) {
					GeniaProcessor.adjustNETypeUsingPartOfSpeech(flow.getSafXML(), flow.getGeniaSAF());
				}
			}
		});
		flowCommands.put("importne", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				if(flow.getGeniaSAF() != null) {
					GeniaProcessor.importGeniaNEs(flow.getSafXML(), flow.getGeniaSAF());
				}
			}
		});
		flowCommands.put("inline", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				flow.setInlineXML((Document)new Document((Element)XOMTools.safeCopy(flow.getInlineXML().getRootElement())));
				flow.setInlineXML(SAFToInline.safToInline(flow.getSafXML(), flow.getSourceXML(), flow.getInlineXML(), false));
			}
		});
		flowCommands.put("sciborgpp", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				SciBorgPostProcess.postProcess(flow.getSafXML());
			}
		});
		flowCommands.put("structuretypes", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				StructureTypes.doStructureTypes(flow.getSafXML());
			}
		});
		flowCommands.put("subtypes", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				NESubtypes.doSubTypes(flow.getSourceXML(), flow.getSafXML());
			}
		});
		flowCommands.put("numbersaf", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				SafTools.numberSaf(flow.getSafXML(), "oscar", "o");
			}
		});
		flowCommands.put("xsl", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				flow.setInlineXML(XSLOnInline.runXSLOnInline(flow.getInlineXML(), args.get(0)));
			}
		});
		flowCommands.put("dataxsl", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				flow.setDataXML(XSLOnInline.runXSLOnInline(flow.getDataXML(), args.get(0)));
			}
		});
		flowCommands.put("relations", new FlowCommand(){
			@SuppressWarnings("unchecked")
			public void call(OscarFlow flow, List<String> args) throws Exception {
				Class c = Class.forName("uk.ac.cam.ch.wwmm.ptc.experimental.relations.Relations");
				Method m = c.getMethod("analysePaper", new Class[]{Document.class, Document.class});
				flow.setRelationXML((Document)m.invoke(null, new Object[]{flow.getSourceXML(), flow.getSafXML()}));
				//flow.setRelationXML(Relations.analysePaper(flow.getSourceXML(), flow.getSafXML()));
			}
		});
		flowCommands.put("data", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				flow.setDataXML(new Document((Element)XOMTools.safeCopy(flow.getSourceXML().getRootElement())));
				DataParser.dataParse(flow.getDataXML());
			}
		});
		flowCommands.put("discardindata", new FlowCommand(){
			public void call(OscarFlow flow, List<String> args) throws Exception {
				DiscardInDataSection.discard(flow.getSafXML());
			}
		});
	
	}
	
	void runFlow(OscarFlow flow, String flowString) throws Exception {
		List<String> commands = new ArrayList<String>();
		List<List<String>> args = new ArrayList<List<String>>();
		parseFlow(flowString, commands, args);
		for(int i=0;i<commands.size();i++) {
			String command = commands.get(i).toLowerCase();
			List<String> arg = args.get(i);
			if(Oscar3Props.getInstance().verbose) System.out.println(command);
			FlowCommand fc = flowCommands.get(command);
			if(fc == null) {
				System.err.println("WARNING: Did not recognised OscarFlow command: " + command);
			} else {
				fc.call(flow, arg);
			}
		}
	}
	
	private static void parseFlow(String flow, List<String> commands, List<List<String>> args) {
		flow = flow.trim();
		Matcher m = flowPattern.matcher(flow);
		while(m.find()) {
			String command = m.group(1);
			List<String> arguments = new ArrayList<String>();
			if(m.group(3) != null) {
				Matcher mm = argumentPattern.matcher(m.group(3));
				while(mm.find()) arguments.add(mm.group());
			}
			commands.add(command);
			args.add(arguments);
		}
	}
	
	/**Adds a command to the OscarFlow language.
	 * 
	 * @param commandName The name of the command. Case insensitive.
	 * @param command The class whose 
	 * call(OscarFlow flow, List&lt;String&gt; args) method will be called
	 * when the command is invoked.
	 * @throws Exception
	 */
	public void addCommand(String commandName, FlowCommand command) throws Exception {
		if(commandName == null || commandName.trim().length() == 0) {
			throw new Exception("Command name must not be null, and must contain non-whitespace characters");
		}
		flowCommands.put(commandName, command);
	}

}
