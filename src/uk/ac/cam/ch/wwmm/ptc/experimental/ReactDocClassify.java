package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import opennlp.maxent.DataIndexer;
import opennlp.maxent.Event;
import opennlp.maxent.EventCollectorAsStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.TwoPassDataIndexer;

import org.tartarus.snowball.ext.PorterStemmer;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.ScrapBook;
import uk.ac.cam.ch.wwmm.ptc.experimental.classifiers.BagEvent;
import uk.ac.cam.ch.wwmm.ptc.experimental.classifiers.MultinomialNaiveBayes;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.misc.ClassificationEvaluator;
import uk.ac.cam.ch.wwmm.ptclib.misc.SimpleEventCollector;
import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.InlineToSAF;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

public class ReactDocClassify {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Stemmer stemmer = new Stemmer(new PorterStemmer());
		
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/reactnewpubmed"), "scrapbook.xml");

		List<Event> events = new ArrayList<Event>();
		
		List<BagEvent> eventBags = new ArrayList<BagEvent>();
		
		for(File f : files) {
			ScrapBook sb = new ScrapBook(f.getParentFile());
			Document doc = (Document)sb.getDoc().copy();
			Nodes nodes = doc.query("//cmlPile");
			for(int i=0;i<nodes.size();i++) nodes.get(i).detach();
			Document sourceDoc = (Document)doc.copy();
			nodes = sourceDoc.query("//ne");
			for(int i=0;i<nodes.size();i++) {
				XOMTools.removeElementPreservingText((Element)nodes.get(i));
			}
			Document safDoc = InlineToSAF.extractSAFs(doc, sourceDoc, "foo");

			ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(sourceDoc, false, false, false);
			//NameRecogniser nr = new NameRecogniser();
			//nr.halfProcess(sourceDoc);
			//nr.makeTokenisers(false);
			Set<String> tokenSet = new HashSet<String>();
			Bag<String> tokenBag = new Bag<String>();
			for(TokenSequence t : procDoc.getTokenSequences()) {
				//System.out.println(t.getSourceString());
				for(Token token : t.getTokens()) {
					//tokenSet.add("stem=" + stemmer.getStem(token.getValue().toLowerCase()));
					//tokenSet.add(token.getValue().toLowerCase());
					tokenBag.add(token.getValue().toLowerCase());
				}
			}
			//for(String t : tokenBag.getList()) {
			//	System.out.println(t + "\t" + tokenBag.getCount(t));
			//}
			
			//File safFile = new File(f.getParentFile(), "saf.xml");
			//Document safDoc = new Builder().build(safFile);
			Nodes n = safDoc.query("/saf/annot[slot[@name='type']['PRW']]");
			Set<String> wpss = new HashSet<String>();
			boolean hasReact = false;
			boolean hasPotentialReact = n.size() > 0;
			for(int i=0;i<n.size();i++) {
				Element annot = (Element)n.get(i);
				String s = SafTools.getSlotValue(annot, "surface").toLowerCase();
				String subtype = SafTools.getSlotValue(annot, "subtype");
				if("REACT".equals(subtype)) hasReact = true;
				String wps = s+"_"+subtype;
				wpss.add(wps);
				//tokenSet.remove(s);
				//tokenSet.remove(stemmer.getStem(s));
				tokenSet.add("PROTECT:" + s);
				tokenSet.add("PROTECT:stem=" + stemmer.getStem(s));
			}
			if(hasPotentialReact) {
				Event e = new Event(hasReact ? "TRUE" : "FALSE", tokenSet.toArray(new String[0]));
				events.add(e);
				BagEvent be = new BagEvent(hasReact ? "TRUE" : "FALSE", tokenBag);
				eventBags.add(be);
			}
		}
				
		if(false) {
			ClassificationEvaluator ce = new ClassificationEvaluator();

			MultinomialNaiveBayes mnb = new MultinomialNaiveBayes(eventBags);
			for(int i=0;i<eventBags.size();i++) {
				BagEvent be = eventBags.get(i);
			//for(BagEvent be : eventBags) {
				Map<String,Double> results = mnb.testBag(be.getClassLabel(), be.getFeatures());
				System.out.println(be.getClassLabel() + "\t" + mnb.testBag(be.getFeatures()));
				ce.logEvent(be.getClassLabel(), mnb.bestResult(results));
				String rf = "MNB:" + mnb.bestResult(results);
				Event e = events.get(i);
				String [] sa = new String[e.getContext().length + 1];
				for(int j=0;j<e.getContext().length;j++) {
					sa[j] = e.getContext()[j];
				}
				sa[e.getContext().length] = rf;
				events.set(i, new Event(e.getOutcome(), sa));
			}
			System.out.println(ce.getAccuracy());
			System.out.println(ce.getKappa());			
			ce.pprintConfusionMatrix();
			ce.pprintPrecisionRecallEval();
			//return;
		}

		
		int seed = 5;
		//for(int seed=0;seed<10;seed++) {
			Collections.shuffle(events, new Random(seed));
			
			List<Event> trainData = events.subList(0, events.size()/2);
			//trainData = new FeatureSelector().selectFeatures(trainData, 200.0);
			List<Event> testData = events.subList(events.size()/2, events.size());
			
			if(trainData.size() == 1) trainData.add(trainData.get(0));
			DataIndexer di = new TwoPassDataIndexer(new EventCollectorAsStream(new SimpleEventCollector(trainData)), 1);
			GISModel gm = GIS.trainModel(100, di);
			
			ClassificationEvaluator ce = new ClassificationEvaluator();
			
			for(Event event : testData) {
				double [] results = gm.eval(event.getContext());
				String result = results[gm.getIndex("TRUE")] > 0.5 ? "TRUE" : "FALSE";
				//String result = gm.getBestOutcome(results);
				//System.out.println(event.getOutcome() + "\t" + result + "\t" + results[gm.getIndex(event.getOutcome())] + "\t" + StringTools.arrayToList(event.getContext()));
				ce.logEvent(event.getOutcome(), result);
			}
			System.out.println("seed: " + seed);
			System.out.println(ce.getAccuracy());
			System.out.println(ce.getKappa());			
		//}
		ce.pprintConfusionMatrix();
		ce.pprintPrecisionRecallEval();

	}

}
