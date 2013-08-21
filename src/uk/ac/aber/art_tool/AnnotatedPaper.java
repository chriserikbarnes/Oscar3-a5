package uk.ac.aber.art_tool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.ProcessingInstruction;
import nu.xom.Serializer;
import nu.xom.Text;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.flow.OscarFlow;
import uk.ac.cam.ch.wwmm.oscar3.misc.NETypes;
import uk.ac.cam.ch.wwmm.oscar3.misc.SciBorgPostProcess;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.InlineToSAF;
import uk.ac.cam.ch.wwmm.ptclib.scixml.PubXMLToSciXML;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.CharacterMarkup;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLInserter;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/**
  * Contains the core logic for SAPIENT annotated papers.
  *
  * @author Maria Liakata
  * @author ptc24
  *
  */
public class AnnotatedPaper {

        /**
	 * 
	 */
	
		// Fields definition
        private ARTSciXMLDocument doc;
        private ARTSciXMLDocument doc2;
        private Element div;
        private int snippetId;
        private File sourceFile;
        private File scrapBookFile;
        private File mode2File;
        private File commentsFile;
        private File fileDir;
        private String name;
        private boolean hasDoc;
        
        private static int EDIT_SIZE = 1;

        // Definition of method

        public static File getScrapBookFile() {
                File scrapbookfile = new File(Oscar3Props.getInstance().workspace,
                                "scrapbook");
                System.err.println(scrapbookfile);
                return scrapbookfile;
        }
        
        public static File getMode2File() {
                return null;
        }

        // Constructors referring to the main constructor
        public AnnotatedPaper(File sbDir) throws Exception {
                this(sbDir.getName(), false, sbDir);
        }

        public AnnotatedPaper(String name) throws Exception {
                this(name, false);
        }

        public AnnotatedPaper(String name, boolean forceNew) throws Exception {
                this(name, forceNew, null);
        }

        // Main Constructor
        public AnnotatedPaper(String name, boolean forceNew, File file)
                        throws Exception {
                this.name = name;

                hasDoc = false;

                if (file == null) {
                        // System.out.println(" file = null");
                        File fileRoot = getScrapBookFile();
                        fileDir = new File(fileRoot, name);
                } else {
                        // System.out.println(" file not null:" + file);
                        fileDir = file;
                }

                if (forceNew && fileDir.exists()) {
                        // System.out.println("force new and file directory exists");
                        File[] subFiles = fileDir.listFiles();
                        for (int i = 0; i < subFiles.length; i++)
                                subFiles[i].delete();
                        fileDir.delete();
                }
                if (!fileDir.exists())
                        // System.out.println("file directory does not exist");
                        fileDir.mkdir();

                sourceFile = new File(fileDir, "source.xml");
                hasDoc = sourceFile.exists();
                scrapBookFile = new File(fileDir, "scrapbook.xml");
                commentsFile = new File(fileDir, "comments.txt");

                if (scrapBookFile.exists()) {
                        doc = ARTSciXMLDocument
                                        .makeFromDoc(new Builder().build(scrapBookFile));
                        snippetId = Integer.parseInt(doc.getRootElement()
                                        .getAttributeValue("currentID").substring(1));
                        Nodes divNodes = doc.query("//DIV");
                        div = (Element) divNodes.get(0);
                } else {
                        doc = makeScrapBook();
                        snippetId = 0;
                        doc.getRootElement().addAttribute(
                                        new Attribute("currentID", "s"
                                                        + Integer.toString(snippetId)));
                        // Nodes divNodes = doc.query("//DIV");
                        Nodes divNodes = doc.query("//DIV");
                        div = (Element) divNodes.get(0);
                        writeScrapBook();
                }

                refreshNavElements();

        }

        public boolean hasMode2Doc() {
                mode2File = new File(fileDir, "mode2.xml");
                return mode2File.exists();
        }

        // Definition of Methods

        public void delete() {
                // DEBUG
                System.err.println("in delete method in AnnotatedPaper");
                File[] subFiles = fileDir.listFiles();
                for (int i = 0; i < subFiles.length; i++) {
                        subFiles[i].delete();
                }
                // System.err.println("fileDir= " + fileDir);
                fileDir.delete();
        }

        public String getName() {
                return name;
        }

        private ARTSciXMLDocument makeScrapBook() {
                Element paper = new Element("PAPER");
                Element title = new Element("TITLE");
                title.appendChild("Annotated Paper");
                paper.appendChild(title);

                paper.appendChild(makeNavElement("show"));

                Element body = new Element("BODY");
                Element div = new Element("DIV");
                ARTSciXMLDocument doc = ARTSciXMLDocument.makeFromDoc(new Document(paper));
                paper.appendChild(body);
                body.appendChild(div);
                div.appendChild("\n\n");

                return doc;
        }

        private void writeScrapBook() throws Exception {
                Nodes titleNodes = doc.query("/PAPER/TITLE");
                Text titleText = (Text) titleNodes.get(0).getChild(0);
                titleText.setValue("AnnotatedPaper: " + name);
                doc.removeProcessingInstructions();
                new Serializer(new FileOutputStream(scrapBookFile)).write(doc);
        }

        public ARTSciXMLDocument getDoc() {
                // System.err.println("doc in AnnotatedPaper getDoc()" + doc);
                Nodes titleNodes = doc.query("/PAPER/TITLE");
                Text titleText = (Text) titleNodes.get(0).getChild(0);
                titleText.setValue("ScrapBook: " + name);
                return doc;
        }

        /*public void addScrap(String html, String fileno) throws Exception {
                Element snippet = HTMLToSciXML.makeSciXML(html);
                addScrap(snippet, fileno);
        }*/

        public void addScrap(Element snippet, String fileno) throws Exception {
                snippet.setLocalName("snippet");
                Element p = new Element("P");
                p.appendChild(snippet);
                // Document tmpDoc = new Document(p);
                // NameRecogniser.getInstance().processDocument(tmpDoc,
                // Config.getXMLLiteConfig());
                // NameResolverInXML.purgeCache();
                // NameResolverInXML.parseDoc(tmpDoc);
                // tmpDoc.setRootElement(new Element("dummy"));
                // p.detach();
                snippetId++;
                doc.getRootElement().addAttribute(
                                new Attribute("currentID", "s" + Integer.toString(snippetId)));
                snippet.addAttribute(new Attribute("id", "s"
                                + Integer.toString(snippetId)));
                if (fileno != null && !fileno.equals("unknown"))
                        snippet.addAttribute(new Attribute("fileno", fileno));
                div.appendChild(p);
                div.appendChild("\n\n");
                writeScrapBook();
        }

        public void deleteScrap(String sid) throws Exception {
                Nodes n = doc.query("//snippet[@id='" + sid + "']");
                if (n.size() == 1) {
                        Element p = (Element) n.get(0).getParent();
                        p.detach();
                        writeScrapBook();
                }
        }

        public void setSnippetProperty(String sid, String property,
                        String propertyName) throws Exception {
                Nodes n = doc.query("//snippet[@id='" + sid + "']");
                if (n.size() == 1) {
                        Element e = (Element) n.get(0);
                        e.addAttribute(new Attribute(propertyName, property));
                        System.out.println(e);
                        writeScrapBook();
                }
        }

        public void clearAnnotations() throws Exception {
                Nodes nodes = doc.query("//ne");
                for (int i = 0; i < nodes.size(); i++) {
                        XOMTools.removeElementPreservingText((Element) nodes.get(i));
                }
                nodes = doc.query("//cmlPile");
                for (int i = 0; i < nodes.size(); i++) {
                        nodes.get(i).detach();
                }
                writeScrapBook();
        }
        
        public void clearAnnotations2() throws Exception {
        		System.out.print("The XML document: \n" + doc2.toXML());
                Nodes nodes = doc2.query("//ne");
                for (int i = 0; i < nodes.size(); i++) {
                        XOMTools.removeElementPreservingText((Element) nodes.get(i));
                }
                nodes = doc2.query("//cmlPile");
                for (int i = 0; i < nodes.size(); i++) {
                        nodes.get(i).detach();
                }
                //System.out.print("The XML document: \n" + doc2.toXML());
                writeMode2Doc(doc2);
        }

        public void autoAnnotate() throws Exception {
                        clearAnnotations();
                        OscarFlow oscarFlow = new OscarFlow(doc);
                        oscarFlow.runFlow("recognise inline");
                        doc = ARTSciXMLDocument.makeFromDoc(oscarFlow.getInlineXML());
                        writeScrapBook();
                
                        // Adapt this and clear annotations for mode 2
                        // need a SciXMLDocument object from mode2.xml using Builder
                        // need a writeMode2 method
                
        }
        
        public ARTSciXMLDocument autoAnnotate2() throws Exception {
                clearAnnotations2();
                OscarFlow oscarFlow = new OscarFlow(doc2);
                oscarFlow.runFlow("recognise inline");
                doc2 = ARTSciXMLDocument.makeFromDoc(oscarFlow.getInlineXML());
                writeMode2Doc(doc2);
                return doc2;
        }
        
        public void writeComments(String comments) {
                try {
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                                        new FileOutputStream(commentsFile),
                                        "UTF8"));
                        out.write(comments);
                        out.close();
                } catch (UnsupportedEncodingException e) {
                } catch (IOException e) {
                }
        }
        
        public String getCommentsAsString() {
                try {
                        BufferedReader out = new BufferedReader(new InputStreamReader(
                                        new FileInputStream(commentsFile),
                                        "UTF8"));
                        StringBuffer comment = new StringBuffer();
                        String thisLine;
                        while((thisLine = out.readLine()) != null) {
                                comment.append(thisLine);
                        }
                        return comment.toString();
                } catch (UnsupportedEncodingException e) {
                        return null;
                } catch (IOException e) {
                        return null;
                }
        }
        

        public ARTSciXMLDocument makeMode2Doc() throws Exception {

                mode2File = sentenceExtraction(sourceFile.getAbsolutePath());
                doc2 = ARTSciXMLDocument.makeFromDoc(new Builder()
                                .build(mode2File));
                
                return doc2;
        }

        public ARTSciXMLDocument getMode2Doc() throws Exception {
                doc2 = ARTSciXMLDocument.makeFromDoc(new Builder().build(mode2File));
                return doc2;
        }

        public void writeMode2Doc(ARTSciXMLDocument mode2doc) throws Exception {
                new Serializer(new FileOutputStream(new File(fileDir, "mode2.xml")),
                                "UTF8").write(mode2doc);
        }
        
        public void writeStringToMode2File(String docString) {
                try {
                        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                                        new FileOutputStream(new File(fileDir, "mode2.xml")),
                                        "UTF8"));
                        out.write(docString);
                        out.close();
                } catch (UnsupportedEncodingException e) {
                } catch (IOException e) {
                }
        }
        
        //IF you change this method, update the version number at the top
        public File sentenceExtraction(String origFileName) throws Exception {
                // Read the whole file into a stringbuffer
                StringBuffer sb = new StringBuffer(1024);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                                new FileInputStream(origFileName), "UTF8"));
                char[] chars = new char[1024];
                int numRead = 0;
                while ((numRead = reader.read(chars)) > -1) {
                        sb.append(String.valueOf(chars));
                }
                reader.close();
                //covert to string
                String fileString = sb.toString();
                
                String extractedString = SSSplit.sentenceExtraction(fileString, name);
                // fileString.replace("(<\\?xml.+?><PAPER>)","")
                // System.out.println("finalbuffer" + finalbuffer);
                // System.out.println("fileString" + fileString);
                writeStringToMode2File(extractedString);
                return new File(fileDir, "mode2.xml");
        }

        public ARTSciXMLDocument getEditor(String sid) {
                System.err.println("in get editor");
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
                ARTSciXMLDocument editorDoc = ARTSciXMLDocument.makeFromDoc(new Document(
                                editorPaper));

                Element goodPara = (Element) doc.query("//snippet[@id='" + sid + "']")
                                .get(0).getParent();

                Nodes nn = goodPara.query(".|following-sibling::P");

                String nextSid = null;
                String prevSid = null;

                if (nn.size() > EDIT_SIZE)
                        nextSid = nn.get(EDIT_SIZE).query("snippet/@id").get(0).getValue();
                Nodes previous = goodPara.query("preceding-sibling::P");
                if (previous.size() > 0)
                        prevSid = previous.get(Math.max(0, previous.size() - EDIT_SIZE))
                                        .query("snippet/@id").get(0).getValue();

                if (prevSid != null) {
                        Element prevElem = new Element("a");
                        prevElem.appendChild("previous");
                        prevElem.addAttribute(new Attribute("href", "ScrapBook?name="
                                        + name + "&action=edit&sid=" + prevSid));
                        Element prevP = new Element("P");
                        prevP.appendChild(prevElem);
                        editorDiv.appendChild(prevP);
                }

                for (int i = 0; i < nn.size() && i < EDIT_SIZE; i++) {
                        Element p = new Element("P");
                        // ne buttons for old edit mode
                        // for (String typeName : NETypes.getTypeNames())
                        // p.appendChild(neButton(typeName));
                        // editorDiv.appendChild(p);
                        // TODO: pull this out to somewhere else
                        String[] names = { "Conclusion", "Results", "Goal", "Method",
                                        "Object", "Experiment", "Observation", "Hypothesis",
                                        "Motivation", "Problem", "Background", "Model", "Example" };
                        Arrays.sort(names);
                        for (String s : names) {
                                p.appendChild(keyDiv(s));
                        }
                        System.err.println(p);
                        editorDiv.appendChild(p);

                        Element para = (Element) XOMTools.safeCopy(nn.get(i));
                        Nodes n = para.query("//ne");
                        for (int j = 0; j < n.size(); j++) {
                                Element e = (Element) n.get(j);
                                e.addAttribute((new Attribute("neid", Integer.toString(j))));
                                // e.appendChild(neTypeSel(Integer.toString(j)));
                        }
                        CharacterMarkup.markupCharacters(para);
                        editorDiv.appendChild(para);
                }

                if (nextSid != null) {
                        Element nextElem = new Element("a");
                        nextElem.appendChild("next");
                        nextElem.addAttribute(new Attribute("href", "ScrapBook?name="
                                        + name + "&action=edit&sid=" + nextSid));
                        Element nextP = new Element("P");
                        nextP.appendChild(nextElem);
                        editorDiv.appendChild(nextP);
                }

                return editorDoc;
        }

        public ARTSciXMLDocument getSelectorEditor(String type) {
                Element editorPaper = new Element("PAPER");
                Element editorBody = new Element("BODY");
                Element editorDiv = new Element("DIV");
                Element editorTitle = new Element("TITLE");
                editorTitle.appendChild("ScrapBook type editor: " + name);
                editorPaper.appendChild(editorTitle);
                Element nav = makeNavElement("selectorEditor");
                nav.addAttribute(new Attribute("selType", type));
                // for(String typeName : NETypes.getTypeNames())
                // nav.appendChild(neButton(typeName));

                editorPaper.appendChild(nav);

                editorPaper.appendChild(editorBody);
                editorBody.appendChild(editorDiv);
                ARTSciXMLDocument editorDoc = ARTSciXMLDocument.makeFromDoc(new Document(
                                editorPaper));

                Nodes nn = doc.query("//snippet");

                List<String> testOpts = new ArrayList<String>();
                testOpts.add("A");
                testOpts.add("B");
                testOpts.add("C");

                for (int i = 0; i < nn.size(); i++) {
                        Element para = (Element) XOMTools.safeCopy(nn.get(i).getParent());
                        String sid = ((Element) nn.get(i)).getAttributeValue("id");
                        Nodes n = para.query("//ne");
                        for (int j = 0; j < n.size(); j++) {
                                Element e = (Element) n.get(j);
                                e.addAttribute((new Attribute("neid", Integer.toString(j))));
                                if (type.equals("type")) {
                                        XOMTools.insertAfter(e, neAttrSel(sid + ":"
                                                        + Integer.toString(j), NETypes.getTypeNames(), e
                                                        .getAttributeValue("type")));
                                } else if (type.equals("subtype")) {
                                        if (NETypes.getSubTypeNames(e.getAttributeValue("type")) == null)
                                                continue;
                                        XOMTools.insertAfter(e, neAttrSel(sid + ":"
                                                        + Integer.toString(j), NETypes.getSubTypeNames(e
                                                        .getAttributeValue("type")), e
                                                        .getAttributeValue("subtype")));
                                } else if (type.equals("ont")) {
                                        String ontIDs = e.getAttributeValue("ontIDs");
                                        if (ontIDs == null || ontIDs.length() == 0)
                                                continue;
                                        String defVal = ontIDs;
                                        if (e.getAttribute("goodOnt") != null)
                                                defVal = e.getAttributeValue("goodOnt");
                                        XOMTools.insertAfter(e, neAttrSel(sid + ":"
                                                        + Integer.toString(j), StringTools
                                                        .spaceSepListToSubLists(ontIDs), defVal));
                                }
                        }
                        editorDiv.appendChild(para);
                }

                return editorDoc;
        }

        public ARTSciXMLDocument getTextFieldEditor(String type) {
                Element editorPaper = new Element("PAPER");
                Element editorBody = new Element("BODY");
                Element editorDiv = new Element("DIV");
                Element editorTitle = new Element("TITLE");
                editorTitle.appendChild("ScrapBook textfield editor: " + name);
                editorPaper.appendChild(editorTitle);
                Element nav = makeNavElement("textFieldEditor");
                nav.addAttribute(new Attribute("txtType", type));
                // for(String typeName : NETypes.getTypeNames())
                // nav.appendChild(neButton(typeName));

                editorPaper.appendChild(nav);

                editorPaper.appendChild(editorBody);
                editorBody.appendChild(editorDiv);
                ARTSciXMLDocument editorDoc = ARTSciXMLDocument.makeFromDoc(new Document(
                                editorPaper));

                Nodes nn = doc.query("//snippet");

                for (int i = 0; i < nn.size(); i++) {
                        Element para = (Element) XOMTools.safeCopy(nn.get(i).getParent());
                        String sid = ((Element) nn.get(i)).getAttributeValue("id");
                        Nodes n = para.query("//ne");
                        for (int j = 0; j < n.size(); j++) {
                                Element e = (Element) n.get(j);
                                e.addAttribute((new Attribute("neid", Integer.toString(j))));
                                Element neTextEntry = new Element("neTextEntry");
                                neTextEntry.addAttribute(new Attribute("name", sid + ":"
                                                + Integer.toString(j)));
                                if (e.getAttribute(type) != null)
                                        neTextEntry.addAttribute(new Attribute("value", e
                                                        .getAttributeValue(type)));
                                XOMTools.insertAfter(e, neTextEntry);
                        }
                        editorDiv.appendChild(para);
                }

                return editorDoc;
        }

        public void updateToSelections(Map params, String type) throws  
Exception {
                // String type = (String)params.get("type");
                // System.out.println(params);
                Set ks = params.keySet();
                Iterator i = ks.iterator();
                while (i.hasNext()) {
                        String s = (String) i.next();
                        if (!s.matches("s\\d+:\\d+"))
                                continue;
                        String sv = ((String[]) params.get(s))[0];
                        String sid = s.split(":")[0];
                        int neid = Integer.parseInt(s.split(":")[1]);
                        Element ne = (Element) doc
                                        .query("//snippet[@id='" + sid + "']//ne").get(neid);
                        if (type.equals("ont")) {
                                ne.addAttribute(new Attribute("goodOnt", sv));
                                /*
                                 * if(sv.length() == 0 && ne.getAttribute("ontIDs") != null) {
                                 * ne.removeAttribute(ne.getAttribute("ontIDs")); } else
                                 * if(sv.length() > 0) { ne.addAttribute(new Attribute("ontIDs",
                                 * sv)); }
                                 */
                        } else {
                                if (sv == null || sv.length() == 0) {
                                        Attribute a = ne.getAttribute(type);
                                        if (a != null)
                                                ne.removeAttribute(a);
                                } else {
                                        ne.addAttribute(new Attribute(type, sv));
                                }
                        }
                }
                System.out.println("Updated... writing scrapbook");
                writeScrapBook();
        }

        // private Element neButton(String neType) {
        // Element button = new Element("nebutton");
        // button.addAttribute(new Attribute("type", neType));
        // return button;
        // }

        private Element keyDiv(String label) {
                Element div = new Element("keydiv");
                div.addAttribute(new Attribute("class", label));
                return div;

        }

        private Element neAttrSel(String id, List<String> values,
                        String defaultValue) {
                Element sel = new Element("neAttrSel");
                sel.addAttribute(new Attribute("name", id));
                for (String value : values)
                        sel.appendChild(neTypeOpt(value, defaultValue));
                return sel;
        }

        private Element neTypeOpt(String value, String defaultValue) {
                Element opt = new Element("neAttrOpt");
                opt.addAttribute(new Attribute("value", value));
                if (value.equals(defaultValue))
                        opt.addAttribute(new Attribute("selected", "selected"));
                return opt;
        }

        public void deleteNe(String sid, String neid) throws Exception {
                Element para = (Element) doc.query("//snippet[@id='" + sid +  
"']").get(
                                0).getParent();
                Nodes n = para.query(".//ne");

                Element ne = (Element) n.get(Integer.parseInt(neid));

                XOMTools.removeElementPreservingText(ne);
                writeScrapBook();
        }

        public void addNe(String sid, String start, String end, String type)
                        throws Exception {
                // System.out.printf("%s %s %s %s\n", sid, start, end, type);
                Element para = (Element) doc.query("//snippet[@id='" + sid +  
"']").get(
                                0).getParent();

                XMLInserter xi = new XMLInserter(para, "a", "c");

                Element ne = new Element("ne");
                ne.addAttribute(new Attribute("type", type));
                xi.insertElement(ne, start, end);
                xi.deTagDocument();
                writeScrapBook();
        }

        public void moveNe(String sid, String start, String end, String neid)
                        throws Exception {
                Element para = (Element) doc.query("//snippet[@id='" + sid +  
"']").get(
                                0).getParent();
                Nodes n = para.query(".//ne");

                Element ne = (Element) n.get(Integer.parseInt(neid));
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
                new Serializer(new FileOutputStream(sourceFile)).write(ptsx
                                .getSourceXML());
                refreshNavElements();
        }

        // public SciXMLDocument makeMode2() throws Exception {

        // }

        public ARTSciXMLDocument makePaper() throws Exception {
                if (!hasDoc)
                        return null;
                File sourceFile = new File(fileDir, "source.xml");
                File destFile = new File(fileDir, "markedup.xml");
                File safFile = new File(fileDir, "saf.xml");

                Nodes n = doc.query("//ne");
                for (int i = 0; i < n.size(); i++) {
                        Element e = (Element) n.get(i);
                        e.addAttribute(new Attribute("surface", e.getValue()));
                }

                ARTSciXMLDocument inlineDoc = ARTSciXMLDocument.makeFromDoc(new Builder()
                                .build(sourceFile));
                Document sourceDoc = (Document) inlineDoc.copy();
                ConvertToAnnotatedPaper.importAnnotations(this, inlineDoc);
                new Serializer(new FileOutputStream(destFile)).write(inlineDoc);
                Document safDoc = InlineToSAF.extractSAFs(inlineDoc, sourceDoc, name);
                SciBorgPostProcess.postProcess(safDoc, false);
                SafTools.numberSaf(safDoc, "oscar", "o");
                new Serializer(new FileOutputStream(safFile)).write(safDoc);

                return inlineDoc;
        }

 
        public ARTSciXMLDocument getAttributeEditor(String sid, String neid)
                        throws Exception {
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
                ARTSciXMLDocument editorDoc = ARTSciXMLDocument.makeFromDoc(new Document(
                                editorPaper));

                Element para = new Element((Element) doc.query(
                                "//snippet[@id='" + sid + "']").get(0).getParent());
                CharacterMarkup.markupCharacters(para);
                editorDiv.appendChild(para);

                Nodes n = para.query("//ne");
                Element ne = (Element)  
XOMTools.safeCopy(n.get(Integer.parseInt(neid)));

                Element attrEd = new Element("attred");
                editorDiv.appendChild(attrEd);
                attrEd.appendChild(ne);
                attrEd.addAttribute(new Attribute("neid", neid));
                attrEd.addAttribute(new Attribute("sid", sid));
                for (int i = 0; i < ne.getAttributeCount(); i++) {
                        Attribute a = ne.getAttribute(i);
                        // if(a.getLocalName() == "type") continue;
                        Element attr = new Element("attr");
                        attr.addAttribute(new Attribute("name", a.getLocalName()));
                        attr.appendChild(a.getValue());
                        attrEd.appendChild(attr);
                }

                ProcessingInstruction pi = new ProcessingInstruction("xml-stylesheet",
                                "type=\"text/xsl\" href=\"ARTtoHTMLJS.xsl\"");
                editorDoc.insertChild(pi, 0);
                pi = new ProcessingInstruction("host",
                                Oscar3Props.getInstance().hostname + ":8181");
                editorDoc.insertChild(pi, 0);

                return editorDoc;
        }

        // params.getkeySet should always return a set of strings,
        // so we ignote type safetly warnings here.
        @SuppressWarnings("unchecked")
        public void editAttributes(String sid, String neid, Map params)
                        throws Exception {
                Element para = (Element) doc.query("//snippet[@id='" + sid +  
"']").get(
                                0).getParent();

                // System.out.printf("%s %s\n", sid, neid);

                Nodes n = para.query(".//ne");
                Element ne = (Element) n.get(Integer.parseInt(neid));
                Set<String> keyset = params.keySet();
                List<String> keylist = new ArrayList<String>();
                keylist.addAll(keyset);

                for (int i = 0; i < keylist.size(); i++) {
                        String key = (String) keylist.get(i);
                        if (key.equals("neid") || key.equals("sid") || key.equals("name")
                                        || key.equals("action") || key.equals("attrname"))
                                continue;
                        String[] values = (String[]) params.get(key);
                        String value = values[0];
                        if (value == null || value.length() == 0) {
                                if (ne.getAttribute(key) != null)
                                        ne.removeAttribute(ne.getAttribute(key));
                        } else {
                                ne.addAttribute(new Attribute(key, value));
                        }
                }
                // System.out.println(ne.toXML());
                writeScrapBook();
        }

        private Element makeNavElement(String mode) {
                Element nav = new Element("scrapbook");
                nav.addAttribute(new Attribute("mode", mode));
                nav.addAttribute(new Attribute("name", name));
                if (hasDoc)
                        nav.addAttribute(new Attribute("hasDoc", "yes"));
                return nav;
        }

        private void refreshNavElements() {
                Nodes n = doc.query("//scrapbook");
                Element nav = (Element) n.get(0);
                XOMTools.insertAfter(nav, makeNavElement("show"));
                nav.detach();
        }

}

