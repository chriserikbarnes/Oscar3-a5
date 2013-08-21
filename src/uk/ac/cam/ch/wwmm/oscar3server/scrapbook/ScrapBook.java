package uk.ac.cam.ch.wwmm.oscar3server.scrapbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;
import nu.xom.Text;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.flow.OscarFlow;
import uk.ac.cam.ch.wwmm.oscar3.misc.NETypes;
import uk.ac.cam.ch.wwmm.oscar3.misc.SciBorgPostProcess;
import uk.ac.cam.ch.wwmm.ptclib.io.ResourceGetter;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.InlineToSAF;
import uk.ac.cam.ch.wwmm.ptclib.scixml.PubXMLToSciXML;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SAFToInline;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.CharacterMarkup;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLInserter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Contains the core logic for ScrapBook.
 * 
 * @author ptc24
 *
 */
public final class ScrapBook {

	private SciXMLDocument doc;
	private Element div;
	private int snippetId;
	private File scrapBookFile;
	private File fileDir;
	private String name;
	private boolean hasDoc;
	private boolean hasPubXMLDoc;
		
	private static ResourceGetter rg = new ResourceGetter("uk/ac/cam/ch/wwmm/oscar3server/scrapbook/resources/");

	private static int EDIT_SIZE = 1;
	
	public static File getScrapBookFile() {
		return new File(Oscar3Props.getInstance().workspace, "scrapbook");
	}
	
	public ScrapBook(File sbDir) throws Exception {
		this(sbDir.getName(), false, sbDir);
	}
	
	public ScrapBook(String name) throws Exception {
		this(name, false);
	}

	public ScrapBook(String name, boolean forceNew) throws Exception {
		this(name, forceNew, null);
	}
	
	public ScrapBook(String name, boolean forceNew, File file) throws Exception {
		this.name = name;
		
		hasDoc = false;
		hasPubXMLDoc = false;
		
		if(file == null) {
			File fileRoot = getScrapBookFile();
			fileDir = new File(fileRoot, name);			
		} else {
			fileDir = file;
		}
		
		if(forceNew && fileDir.exists()) {
			File [] subFiles = fileDir.listFiles();
			for(int i=0;i<subFiles.length;i++) subFiles[i].delete();
			fileDir.delete();
		}
		if(!fileDir.exists()) fileDir.mkdir();			
		
		hasDoc = new File(fileDir, "source.xml").exists();
		hasPubXMLDoc = new File(fileDir, "pubxml-source.xml").exists();
		
		scrapBookFile = new File(fileDir, "scrapbook.xml");
		
		if(scrapBookFile.exists()) {
			doc = SciXMLDocument.makeFromDoc(new Builder().build(scrapBookFile));
			snippetId = Integer.parseInt(doc.getRootElement().getAttributeValue("currentID").substring(1));
			Nodes divNodes = doc.query("//DIV");
			div = (Element)divNodes.get(0);
		} else {
			doc = makeScrapBook(); // FIXME
			snippetId = 0;
			doc.getRootElement().addAttribute(new Attribute("currentID", "s" + Integer.toString(snippetId)));
			Nodes divNodes = doc.query("//DIV");
			div = (Element)divNodes.get(0);
			writeScrapBook();
		}
		
		refreshNavElements();
					
	}
	
	public void delete() {
		File [] subFiles = fileDir.listFiles();
		for(int i=0;i<subFiles.length;i++) {
			subFiles[i].delete();
		}
		fileDir.delete();
	}
	
	public String getName() {
		return name;
	}
	
	private SciXMLDocument makeScrapBook() {
		Element paper = new Element("PAPER");
		Element title = new Element("TITLE");
		title.appendChild("ScrapBook");
		paper.appendChild(title);
	
		paper.appendChild(makeNavElement("show"));
		
		Element body = new Element("BODY");
		Element div = new Element("DIV");
		SciXMLDocument doc = SciXMLDocument.makeFromDoc(new Document(paper));
		paper.appendChild(body);
		body.appendChild(div);
		div.appendChild("\n\n");
		
		return doc;
	}
	
	private void writeScrapBook() throws Exception {
		Nodes titleNodes = doc.query("/PAPER/TITLE");
		Text titleText = (Text)titleNodes.get(0).getChild(0);
		titleText.setValue("ScrapBook: " + name);
		doc.removeProcessingInstructions();
		new Serializer(new FileOutputStream(scrapBookFile)).write(doc);
	}

	public SciXMLDocument getDoc() {
		Nodes titleNodes = doc.query("/PAPER/TITLE");
		Text titleText = (Text)titleNodes.get(0).getChild(0);
		titleText.setValue("ScrapBook: " + name);
		return doc;
	}

	public void addScrap(String html, String fileno) throws Exception {
		Element snippet = HTMLToSciXML.makeSciXML(html);
		addScrap(snippet, fileno);
	}
	
	public void addScrap(Element snippet, String fileno) throws Exception {
		snippet.setLocalName("snippet");
		Element p = new Element("P");
		p.appendChild(snippet);
		//Document tmpDoc = new Document(p);
		//NameRecogniser.getInstance().processDocument(tmpDoc, Config.getXMLLiteConfig());
		//NameResolverInXML.purgeCache();
		//NameResolverInXML.parseDoc(tmpDoc);
		//tmpDoc.setRootElement(new Element("dummy"));
		//p.detach();
		snippetId++;
		doc.getRootElement().addAttribute(new Attribute("currentID", "s" + Integer.toString(snippetId)));
		snippet.addAttribute(new Attribute("id", "s" + Integer.toString(snippetId)));
		if(fileno != null && !fileno.equals("unknown")) snippet.addAttribute(new Attribute("fileno", fileno));
		div.appendChild(p);
		div.appendChild("\n\n");
		writeScrapBook();		
	}
	
	public void deleteScrap(String sid) throws Exception {
		Nodes n = doc.query("//snippet[@id='" + sid +"']");
		if(n.size() == 1) {
			Element p = (Element)n.get(0).getParent();
			p.detach();
			writeScrapBook();
		}
	}

	public void setSnippetProperty(String sid, String property, String propertyName) throws Exception {
		Nodes n = doc.query("//snippet[@id='" + sid +"']");
		if(n.size() == 1) {
			Element e = (Element)n.get(0);
			e.addAttribute(new Attribute(propertyName, property));
			if(Oscar3Props.getInstance().verbose) System.out.println(e);
			writeScrapBook();
		}
	}
	
	public void clearAnnotations() throws Exception {
		Nodes nodes = doc.query("//ne");
		for(int i=0;i<nodes.size();i++) {
			XOMTools.removeElementPreservingText((Element)nodes.get(i));
		}
		nodes = doc.query("//cmlPile");
		for(int i=0;i<nodes.size();i++) {
			nodes.get(i).detach();
		}
		writeScrapBook();
	}
	
	public void autoAnnotate() throws Exception {
		clearAnnotations();
		OscarFlow oscarFlow = new OscarFlow(doc);
		oscarFlow.processLite();
		doc = SciXMLDocument.makeFromDoc(oscarFlow.getInlineXML());
		writeScrapBook();
	}

	public void autoAnnotateReactions() throws Exception {
		clearAnnotations();
		doc = SciXMLDocument.makeFromDoc(ReactWords.getInstance().annotateDoc(doc));
		writeScrapBook();
	}
	
	public RegTestResults regtest() throws Exception {
		
		Document parseDoc = (Document)XOMTools.safeCopy(doc);
		Nodes nodes = parseDoc.query("//ne");
		for(int i=0;i<nodes.size();i++) {
			XOMTools.removeElementPreservingText((Element)nodes.get(i));
		}
		nodes = parseDoc.query("//cmlPile");
		for(int i=0;i<nodes.size();i++) {
			nodes.get(i).detach();
		}		
		OscarFlow oscarFlow = new OscarFlow(parseDoc);
		oscarFlow.processLite();
		parseDoc = oscarFlow.getInlineXML();
		
		Nodes parsedIds = parseDoc.query("//snippet/@id");
		Nodes goodIds = doc.query("//snippet/@id");
		Set<String> parsedIdStrs = new LinkedHashSet<String>();
		Set<String> goodIdStrs = new LinkedHashSet<String>();
		for(int i=0;i<parsedIds.size();i++) parsedIdStrs.add(parsedIds.get(i).getValue());
		for(int i=0;i<goodIds.size();i++) goodIdStrs.add(goodIds.get(i).getValue());
		goodIdStrs.retainAll(parsedIdStrs);

		Element resultsPaper = new Element("PAPER");
		Element resultsBody = new Element("BODY");
		Element goodDiv = new Element("DIV");
		Element failedDiv = new Element("DIV");
		Element resultsTitle = new Element("TITLE");
		resultsTitle.appendChild("ScrapBook results: " + name);
		resultsPaper.appendChild(resultsTitle);

		resultsPaper.appendChild(makeNavElement("regtest"));
		
		resultsPaper.appendChild(resultsBody);
		resultsBody.appendChild(goodDiv);
		resultsBody.appendChild(failedDiv);
		SciXMLDocument resultsDoc = SciXMLDocument.makeFromDoc(new Document(resultsPaper));
		
		Element failedDivHeader = new Element("HEADER");
		failedDivHeader.appendChild("Regression");
		failedDiv.appendChild(failedDivHeader);
		
		Element goodDivHeader = new Element("HEADER");
		goodDivHeader.appendChild("No regression");
		goodDiv.appendChild(goodDivHeader);

		ScoreStats grandScore = new ScoreStats();
		
		for(String sid : goodIdStrs) {
			Element goodSnippet = (Element)doc.query("//snippet[@id='" + sid + "']").get(0);
			Element parsedSnippet = (Element)parseDoc.query("//snippet[@id='" + sid + "']").get(0);
			Element c = SnippetCompare.canonicalise(parsedSnippet);
			Element p = new Element("P");
			p.appendChild(c);
			ScoreStats ss = SnippetCompare.getPrecisionAndRecall(parsedSnippet, goodSnippet);
			grandScore.addScoreStats(ss);
			if(SnippetCompare.compare(goodSnippet, parsedSnippet)) {
				goodDiv.appendChild(p);
			} else {
				Element fdd = new Element("DIV");
				Element fddh = new Element("HEADER");
				fddh.appendChild("Oscar3 Parse");
				fdd.appendChild(fddh);
				fdd.appendChild(p);
				failedDiv.appendChild(fdd);
				
				fdd = new Element("DIV");
				fddh = new Element("HEADER");
				fddh.appendChild("Hand Annotation");
				fdd.appendChild(fddh);
				p = new Element("P");
				p.appendChild(XOMTools.safeCopy(goodSnippet));
				fdd.appendChild(p);				
				failedDiv.appendChild(fdd);
				
				p = new Element("P");
				p.appendChild(ss.getPrecAndRecallString());
				fdd.appendChild(p);
			}
		}
		
		Element grandTotalDiv = new Element("DIV");
		resultsBody.appendChild(grandTotalDiv);
		Element grandTotalHeader = new Element("HEADER");
		grandTotalHeader.appendChild("Grand Total");
		grandTotalDiv.appendChild(grandTotalHeader);
		Element p = new Element("P");
		p.appendChild(grandScore.getPrecAndRecallString());
		grandTotalDiv.appendChild(p);

		return new RegTestResults(resultsDoc, grandScore);
	}
	
	public SciXMLDocument getEditor(String sid) {
		Element editorPaper = new Element("PAPER");
		Element editorBody = new Element("BODY");
		Element editorDiv = new Element("DIV");
		Element editorTitle = new Element("TITLE");
		editorTitle.appendChild("ScrapBook editor: " + name);
		editorPaper.appendChild(editorTitle);

		Element nav = makeNavElement("editor");
		
		editorPaper.appendChild(nav);
		
		editorPaper.appendChild(editorBody);
		editorBody.appendChild(editorDiv);
		SciXMLDocument editorDoc = SciXMLDocument.makeFromDoc(new Document(editorPaper));

		Element goodPara = (Element)doc.query("//snippet[@id='" + sid + "']").get(0).getParent();
		
		Nodes nn = goodPara.query(".|following-sibling::P");
		
		String nextSid = null;
		String prevSid = null;
		
		if(nn.size() > EDIT_SIZE) nextSid = nn.get(EDIT_SIZE).query("snippet/@id").get(0).getValue();
		Nodes previous = goodPara.query("preceding-sibling::P");
		if(previous.size() > 0) prevSid = previous.get(Math.max(0, previous.size()-EDIT_SIZE)).query("snippet/@id").get(0).getValue();
				
		if(prevSid != null) {
			Element prevElem = new Element("a");
			prevElem.appendChild("previous");
			prevElem.addAttribute(new Attribute("href", "ScrapBook?name=" + name + "&action=edit&sid=" + prevSid));
			Element prevP = new Element("P");
			prevP.appendChild(prevElem);
			editorDiv.appendChild(prevP);
		}
		
		for(int i=0;i<nn.size() && i<EDIT_SIZE;i++) {
			Element p = new Element("P");
			for(String typeName : NETypes.getTypeNames()) p.appendChild(neButton(typeName));
			if(Oscar3Props.getInstance().polymerMode && !NETypes.getTypeNames().contains("PM")) p.appendChild(neButton("PM"));
			editorDiv.appendChild(p);

			Element para = (Element)XOMTools.safeCopy(nn.get(i));		
			Nodes n = para.query("//ne");
			for(int j=0;j<n.size();j++) {
				Element e = (Element)n.get(j);
				e.addAttribute((new Attribute("neid", Integer.toString(j))));
				//e.appendChild(neTypeSel(Integer.toString(j)));
			}
			CharacterMarkup.markupCharacters(para, Oscar3Props.getInstance().scrapBookIEFix);		
			editorDiv.appendChild(para);
		}

		if(nextSid != null) {
			Element nextElem = new Element("a");
			nextElem.appendChild("next");
			nextElem.addAttribute(new Attribute("href", "ScrapBook?name=" + name + "&action=edit&sid=" + nextSid));
			Element nextP = new Element("P");
			nextP.appendChild(nextElem);
			editorDiv.appendChild(nextP);
		}
				
		return editorDoc;
	}	
	
	public SciXMLDocument getRelEditor(String sid) throws Exception {
		Element editorPaper = new Element("PAPER");
		Element editorBody = new Element("BODY");
		Element editorDiv = new Element("DIV");
		Element editorTitle = new Element("TITLE");
		editorTitle.appendChild("ScrapBook relation editor: " + name);
		editorPaper.appendChild(editorTitle);

		Element nav = makeNavElement("relEditor");
		
		Document relDoc = rg.getXMLDocument("relitems.xml");
		Element relElem = relDoc.getRootElement();
		relDoc.setRootElement(new Element("dummy"));
		nav.appendChild(relElem);
		
		editorPaper.appendChild(nav);
		
		editorPaper.appendChild(editorBody);
		editorBody.appendChild(editorDiv);
		SciXMLDocument editorDoc = SciXMLDocument.makeFromDoc(new Document(editorPaper));

		Element goodPara = (Element)doc.query("//snippet[@id='" + sid + "']").get(0).getParent();
		
		Nodes nn = goodPara.query(".|following-sibling::P");
		
		String nextSid = null;
		String prevSid = null;
		
		if(nn.size() > EDIT_SIZE) nextSid = nn.get(EDIT_SIZE).query("snippet/@id").get(0).getValue();
		Nodes previous = goodPara.query("preceding-sibling::P");
		if(previous.size() > 0) prevSid = previous.get(Math.max(0, previous.size()-EDIT_SIZE)).query("snippet/@id").get(0).getValue();
				
		if(prevSid != null) {
			Element prevElem = new Element("a");
			prevElem.appendChild("previous");
			prevElem.addAttribute(new Attribute("href", "ScrapBook?name=" + name + "&action=reledit&sid=" + prevSid));
			Element prevP = new Element("P");
			prevP.appendChild(prevElem);
			editorDiv.appendChild(prevP);
		}
		
		for(int i=0;i<nn.size() && i<EDIT_SIZE;i++) {
			Element p = new Element("P");
			for(String typeName : NETypes.getTypeNames()) p.appendChild(neButton(typeName));
			editorDiv.appendChild(p);

			Element para = (Element)XOMTools.safeCopy(nn.get(i));		
			Nodes n = para.query("//ne");
			for(int j=0;j<n.size();j++) {
				Element e = (Element)n.get(j);
				e.addAttribute((new Attribute("neid", Integer.toString(j))));
				Element ee = new Element("neLabel");
				ee.addAttribute(new Attribute("name", Integer.toString(j)));
				XOMTools.insertAfter(e, ee);
			}
			editorDiv.appendChild(para);
		}

		if(nextSid != null) {
			Element nextElem = new Element("a");
			nextElem.appendChild("next");
			nextElem.addAttribute(new Attribute("href", "ScrapBook?name=" + name + "&action=reledit&sid=" + nextSid));
			Element nextP = new Element("P");
			nextP.appendChild(nextElem);
			editorDiv.appendChild(nextP);
		}
				
		return editorDoc;
	}
	
	public SciXMLDocument getSelectorEditor(String type) {
		Element editorPaper = new Element("PAPER");
		Element editorBody = new Element("BODY");
		Element editorDiv = new Element("DIV");
		Element editorTitle = new Element("TITLE");
		editorTitle.appendChild("ScrapBook type editor: " + name);
		editorPaper.appendChild(editorTitle);
		Element nav = makeNavElement("selectorEditor");
		nav.addAttribute(new Attribute("selType", type));
		//for(String typeName : NETypes.getTypeNames()) nav.appendChild(neButton(typeName));
		
		editorPaper.appendChild(nav);
		
		editorPaper.appendChild(editorBody);
		editorBody.appendChild(editorDiv);
		SciXMLDocument editorDoc = SciXMLDocument.makeFromDoc(new Document(editorPaper));

		Nodes nn = doc.query("//snippet");
		
		List<String> testOpts = new ArrayList<String>();
		testOpts.add("A");
		testOpts.add("B");
		testOpts.add("C");
		
		for(int i=0;i<nn.size();i++) {		
			Element para = (Element)XOMTools.safeCopy(nn.get(i).getParent());
			String sid = ((Element)nn.get(i)).getAttributeValue("id");
			Nodes n = para.query("//ne");
			for(int j=0;j<n.size();j++) {
				Element e = (Element)n.get(j);
				e.addAttribute((new Attribute("neid", Integer.toString(j))));
				if(type.equals("type")) {
					XOMTools.insertAfter(e, neAttrSel(sid + ":" + Integer.toString(j), NETypes.getTypeNames(), e.getAttributeValue("type")));					
				} else if(type.equals("subtype")) {
					if(NETypes.getSubTypeNames(e.getAttributeValue("type")) == null) continue;
					XOMTools.insertAfter(e, neAttrSel(sid + ":" + Integer.toString(j), NETypes.getSubTypeNames(e.getAttributeValue("type")), e.getAttributeValue("subtype")));
				} else if(type.equals("ont")) {
					String ontIDs = e.getAttributeValue("ontIDs");
					if(ontIDs == null || ontIDs.length() == 0) continue;
					String defVal = ontIDs;
					if(e.getAttribute("goodOnt") != null) defVal = e.getAttributeValue("goodOnt");
					XOMTools.insertAfter(e, neAttrSel(sid + ":" + Integer.toString(j), StringTools.spaceSepListToSubLists(ontIDs), defVal));
				}
			}
			editorDiv.appendChild(para);
		}

		return editorDoc;		
	}

	public SciXMLDocument getBooleanAttrEditor(String attrName) {
		Element editorPaper = new Element("PAPER");
		Element editorBody = new Element("BODY");
		Element editorDiv = new Element("DIV");
		Element editorTitle = new Element("TITLE");
		editorTitle.appendChild("ScrapBook boolean editor: " + name + " attribute: " + attrName);
		editorPaper.appendChild(editorTitle);
		Element nav = makeNavElement("booleanEditor");
		nav.addAttribute(new Attribute("attrName", attrName));
		for(String typeName : NETypes.getTypeNames()) nav.appendChild(neButton(typeName));
		
		editorPaper.appendChild(nav);
		
		editorPaper.appendChild(editorBody);
		editorBody.appendChild(editorDiv);
		SciXMLDocument editorDoc = SciXMLDocument.makeFromDoc(new Document(editorPaper));

		Nodes nn = doc.query("//snippet");
		
		for(int i=0;i<nn.size();i++) {		
			Element para = (Element)XOMTools.safeCopy(nn.get(i).getParent());
			String sid = ((Element)nn.get(i)).getAttributeValue("id");
			Nodes n = para.query("//ne");
			for(int j=0;j<n.size();j++) {
				Element e = (Element)n.get(j);
				e.addAttribute((new Attribute("neid", Integer.toString(j))));
				Element tb = new Element("neTickyBox");
				if(e.getAttribute(attrName) != null) tb.addAttribute(new Attribute("selected", "yes"));
				tb.addAttribute(new Attribute("name", sid + ":" + Integer.toString(j)));
				XOMTools.insertAfter(e, tb);
			}
			editorDiv.appendChild(para);
		}

		return editorDoc;		
	}
	
	public SciXMLDocument getTextFieldEditor(String type) {
		Element editorPaper = new Element("PAPER");
		Element editorBody = new Element("BODY");
		Element editorDiv = new Element("DIV");
		Element editorTitle = new Element("TITLE");
		editorTitle.appendChild("ScrapBook textfield editor: " + name);
		editorPaper.appendChild(editorTitle);
		Element nav = makeNavElement("textFieldEditor");
		nav.addAttribute(new Attribute("txtType", type));
		//for(String typeName : NETypes.getTypeNames()) nav.appendChild(neButton(typeName));
		
		editorPaper.appendChild(nav);
		
		editorPaper.appendChild(editorBody);
		editorBody.appendChild(editorDiv);
		SciXMLDocument editorDoc = SciXMLDocument.makeFromDoc(new Document(editorPaper));

		Nodes nn = doc.query("//snippet");

				for(int i=0;i<nn.size();i++) {		
			Element para = (Element)XOMTools.safeCopy(nn.get(i).getParent());
			String sid = ((Element)nn.get(i)).getAttributeValue("id");
			Nodes n = para.query("//ne");
			for(int j=0;j<n.size();j++) {
				Element e = (Element)n.get(j);
				e.addAttribute((new Attribute("neid", Integer.toString(j))));
				Element neTextEntry = new Element("neTextEntry");
				neTextEntry.addAttribute(new Attribute("name", sid + ":" + Integer.toString(j)));
				if(e.getAttribute(type) != null) neTextEntry.addAttribute(new Attribute("value", e.getAttributeValue(type)));
				XOMTools.insertAfter(e, neTextEntry);
			}
			editorDiv.appendChild(para);
		}

		return editorDoc;		
	}
	
	public void updateToSelections(Map params, String type) throws Exception {
		//String type = (String)params.get("type");
		//System.out.println(params);
		Set ks = params.keySet();
		Iterator i = ks.iterator();
		while(i.hasNext()) {
			String s = (String)i.next();
			if(!s.matches("s\\d+:\\d+")) continue;
			String sv = ((String [])params.get(s))[0];
			String sid = s.split(":")[0];
			int neid = Integer.parseInt(s.split(":")[1]);
			Element ne = (Element)doc.query("//snippet[@id='" + sid + "']//ne").get(neid);
			if(type.equals("ont")) {
				ne.addAttribute(new Attribute("goodOnt", sv));
				/*if(sv.length() == 0 && ne.getAttribute("ontIDs") != null) {
					ne.removeAttribute(ne.getAttribute("ontIDs"));
				} else if(sv.length() > 0) {
					ne.addAttribute(new Attribute("ontIDs", sv));
				}*/
			} else{
				if(sv == null || sv.length() == 0) {
					Attribute a = ne.getAttribute(type);
					if(a != null) ne.removeAttribute(a);
				} else {
					ne.addAttribute(new Attribute(type, sv));									
				}
			}
		}
		System.out.println("Updated... writing scrapbook");
		writeScrapBook();
	}

	public void submitBooleans(Map params, String attrName) throws Exception {
		Set ks = params.keySet();
		Iterator i = ks.iterator();
		Nodes n = doc.query("//ne");
		for(int j=0;j<n.size();j++) {
			Element e = (Element)n.get(j);
			Attribute a = e.getAttribute(attrName);
			if(a != null) e.removeAttribute(a);
		}
		while(i.hasNext()) {
			String s = (String)i.next();
			if(!s.matches("s\\d+:\\d+")) continue;
			//String sv = ((String [])params.get(s))[0];
			String sid = s.split(":")[0];
			int neid = Integer.parseInt(s.split(":")[1]);
			Element ne = (Element)doc.query("//snippet[@id='" + sid + "']//ne").get(neid);
			ne.addAttribute(new Attribute(attrName, "yes"));
			//ne.addAttribute(new Attribute("type", sv));
		}
		writeScrapBook();
	}
	
	private Element neButton(String neType) {
		Element button = new Element("nebutton");
		button.addAttribute(new Attribute("type", neType));
		return button;
	}
	
	private Element neAttrSel(String id, List<String> values, String defaultValue) {
		Element sel = new Element("neAttrSel");
		sel.addAttribute(new Attribute("name", id));
		for(String value : values) sel.appendChild(neTypeOpt(value, defaultValue));
		return sel;
	}

	private Element neTypeOpt(String value, String defaultValue) {
		Element opt = new Element("neAttrOpt");
		opt.addAttribute(new Attribute("value", value));
		if(value.equals(defaultValue)) opt.addAttribute(new Attribute("selected", "selected"));
		return opt;
	}
	
	public void deleteNe(String sid, String neid) throws Exception {
		Element para = (Element)doc.query("//snippet[@id='" + sid + "']").get(0).getParent();
		Nodes n = para.query(".//ne");

		Element ne = (Element)n.get(Integer.parseInt(neid));
		
		XOMTools.removeElementPreservingText(ne);
		writeScrapBook();
	}

	public void addNe(String sid, String start, String end, String type) throws Exception {
		//System.out.printf("%s %s %s %s\n", sid, start, end, type);
		Element para = (Element)doc.query("//snippet[@id='" + sid + "']").get(0).getParent();
		
		XMLInserter xi = new XMLInserter(para, "a", "c");
				
		Element ne = new Element("ne");
		ne.addAttribute(new Attribute("type", type));
		xi.insertElement(ne, start, end);
		xi.deTagDocument();
		writeScrapBook();
	}
	
	public void moveNe(String sid, String start, String end, String neid) throws Exception {
		Element para = (Element)doc.query("//snippet[@id='" + sid + "']").get(0).getParent();
		Nodes n = para.query(".//ne");

		Element ne = (Element)n.get(Integer.parseInt(neid));
		XOMTools.removeElementPreservingText(ne);

		XMLInserter xi = new XMLInserter(para, "a", "c");		
		xi.insertElement(ne, start, end);
		xi.deTagDocument();
		writeScrapBook();		
	}
	
	public void addSourceDoc(Document sourceDoc) throws Exception {
		File sourceFile = new File(fileDir, "source.xml");
		new Serializer(new FileOutputStream(sourceFile)).write(sourceDoc);
		hasDoc = true;
		refreshNavElements();
	}
	
	public void addPubXMLDoc(PubXMLToSciXML ptsx) throws Exception {
		File sourceFile = new File(fileDir, "pubxml-source.xml");
		new Serializer(new FileOutputStream(sourceFile)).write(ptsx.getSourceXML());
		hasPubXMLDoc = true;
		refreshNavElements();
	}
	
	public SciXMLDocument makePaper() throws Exception {
		if(!hasDoc) return null;
		File sourceFile = new File(fileDir, "source.xml");
		File destFile = new File(fileDir, "markedup.xml");
		File safFile = new File(fileDir, "saf.xml");
		
		Nodes n = doc.query("//ne");
		for(int i=0;i<n.size();i++) {
			Element e = (Element)n.get(i);
			e.addAttribute(new Attribute("surface", e.getValue()));
		}
		
		SciXMLDocument inlineDoc = SciXMLDocument.makeFromDoc(new Builder().build(sourceFile));
		Document sourceDoc = (Document)inlineDoc.copy();
		PaperToScrapBook.importAnnotations(this, inlineDoc);
		new Serializer(new FileOutputStream(destFile)).write(inlineDoc);
		Document safDoc = InlineToSAF.extractSAFs(inlineDoc, sourceDoc, name);
		SciBorgPostProcess.postProcess(safDoc, false);
		SafTools.numberSaf(safDoc, "oscar", "o");
		new Serializer(new FileOutputStream(safFile)).write(safDoc);
		
		return inlineDoc;
	}
	
	public Document makePubXMLPaper() throws Exception {
		if(!hasPubXMLDoc) return null;
		File pubXMLSourceFile = new File(fileDir, "pubxml-source.xml");
		Document pubXMLDoc = new Builder().build(pubXMLSourceFile);
		PubXMLToSciXML ptsx = new PubXMLToSciXML(pubXMLDoc);
		
		Document sciXMLDoc = (Document)ptsx.getSciXML().copy();
		PaperToScrapBook.importAnnotations(this, sciXMLDoc);
		Document safDoc = InlineToSAF.extractSAFs(sciXMLDoc, ptsx.getSciXML(), name);
		
		ptsx.transformSAFs(safDoc);
		Document newPubXMLDoc = SAFToInline.safToInline(safDoc, ptsx.getSourceXML(), false);

		return newPubXMLDoc;
	}
	
	public SciXMLDocument getAttributeEditor(String sid, String neid) throws Exception {
		Element editorPaper = new Element("PAPER");
		Element editorBody = new Element("BODY");
		Element editorDiv = new Element("DIV");
		Element editorTitle = new Element("TITLE");
		editorTitle.appendChild("ScrapBook attribute editor: " + name);
		editorPaper.appendChild(editorTitle);

		Element nav = makeNavElement("attreditor");
		
		editorPaper.appendChild(nav);
		
		editorPaper.appendChild(editorBody);
		editorBody.appendChild(editorDiv);
		SciXMLDocument editorDoc = SciXMLDocument.makeFromDoc(new Document(editorPaper));

		Element para = new Element((Element)doc.query("//snippet[@id='" + sid + "']").get(0).getParent());
		CharacterMarkup.markupCharacters(para, Oscar3Props.getInstance().scrapBookIEFix);		
		editorDiv.appendChild(para);		
		
		Nodes n = para.query("//ne");
		Element ne = (Element)XOMTools.safeCopy(n.get(Integer.parseInt(neid)));
		
		Element attrEd = new Element("attred");
		editorDiv.appendChild(attrEd);
		attrEd.appendChild(ne);
		attrEd.addAttribute(new Attribute("neid", neid));
		attrEd.addAttribute(new Attribute("sid", sid));
		for(int i=0;i<ne.getAttributeCount();i++) {
			Attribute a = ne.getAttribute(i);
			//if(a.getLocalName() == "type") continue;
			Element attr = new Element("attr");
			attr.addAttribute(new Attribute("name", a.getLocalName()));
			attr.appendChild(a.getValue());
			attrEd.appendChild(attr);
		}
		
		editorDoc.addServerProcessingInstructions();
		
		/*ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"toHTMLJS.xsl\"");
		editorDoc.insertChild(pi, 0);
		pi = new ProcessingInstruction("host", Oscar3Props.getInstance().hostname + ":" + Oscar3Props.getInstance().port);
		editorDoc.insertChild(pi, 0);*/

		return editorDoc;	
	}
	
	//params.getkeySet should always return a set of strings,
	//so we ignote type safetly warnings here.
	@SuppressWarnings("unchecked")
	public void editAttributes(String sid, String neid, Map params) throws Exception {
		Element para = (Element)doc.query("//snippet[@id='" + sid + "']").get(0).getParent();
		
		//System.out.printf("%s %s\n", sid, neid);
		
		Nodes n = para.query(".//ne");
		Element ne = (Element)n.get(Integer.parseInt(neid));
		Set<String> keyset = params.keySet();
		List<String> keylist = new ArrayList<String>();
		keylist.addAll(keyset);
		
		for(int i=0;i<keylist.size();i++) {
			String key = (String)keylist.get(i);
			if(key.equals("neid") || key.equals("sid") || key.equals("name") || key.equals("action") || key.equals("attrname")) continue;
			String [] values = (String [])params.get(key);
			String value = values[0];
			if(value == null || value.length() == 0) {
				if(ne.getAttribute(key) != null) ne.removeAttribute(ne.getAttribute(key));
			} else {
				ne.addAttribute(new Attribute(key, value));
			}			
		}
		//System.out.println(ne.toXML());
		writeScrapBook();
	}
	
	public void renderRelations() {
		Map<String,Map<String,List<Element>>> relationMap = new HashMap<String,Map<String,List<Element>>>(); 
		
		Nodes n = doc.query("//snippet");
		Pattern relPattern = Pattern.compile("([a-zA-Z]+)(\\@?[0-9]+)?");
		int unaryID = 0;
		for(int i=0;i<n.size();i++) {
			String sid = ((Element)n.get(i)).getAttributeValue("id");
			Nodes nn = n.get(i).query(".//ne[@relation]");
			for(int j=0;j<nn.size();j++) {
				Element ne = (Element)nn.get(j);
				if(Oscar3Props.getInstance().verbose) System.out.println(ne.getValue() + "\t" + ne.getAttributeValue("relation"));
				String relationStr = ne.getAttributeValue("relation");
				List<String> relations = StringTools.arrayToList(relationStr.split("\\s+"));
				for(String relation : relations) {
					Matcher m = relPattern.matcher(relation);
					if(m.matches()) {
						String fieldName = m.group(1);
						String relId = m.group(2);
						if(relId == null) {
							relId = "U" + unaryID;
							unaryID++;
						} else {
							if(!relId.startsWith("@")) relId = sid + ":" + relId;
						}
						if(Oscar3Props.getInstance().verbose) System.out.println(ne.getValue() + "\t" + fieldName + "\t" + relId);
						
						if(!relationMap.containsKey(relId)) relationMap.put(relId, new HashMap<String,List<Element>>());
						if(!relationMap.get(relId).containsKey(fieldName)) relationMap.get(relId).put(fieldName, new ArrayList<Element>());
						relationMap.get(relId).get(fieldName).add(ne);
					} else {
						if(Oscar3Props.getInstance().verbose) System.out.println("No match: " + relation);
					}
				}
			}
		}
		if(Oscar3Props.getInstance().verbose) System.out.println(relationMap);
	}
	
	private Element makeNavElement(String mode) {
		Element nav = new Element("scrapbook");
		nav.addAttribute(new Attribute("mode", mode));
		nav.addAttribute(new Attribute("name", name));
		if(hasDoc) nav.addAttribute(new Attribute("hasDoc", "yes"));
		if(hasPubXMLDoc) nav.addAttribute(new Attribute("hasPubXMLDoc", "yes"));
		return nav;
	}
	
	private void refreshNavElements() {
		Nodes n = doc.query("//scrapbook");
		Element nav = (Element)n.get(0);
		XOMTools.insertAfter(nav, makeNavElement("show"));
		nav.detach();
	}
	
}
