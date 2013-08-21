package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import nu.xom.Builder;
import nu.xom.Document;
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
import uk.ac.cam.ch.wwmm.ptclib.misc.ClassificationEvaluator;
import uk.ac.cam.ch.wwmm.ptclib.misc.SimpleEventCollector;
import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class WSD {

	static class Entry {
		String type;
		String start;
		String end;
		String filename;
		
		Entry(String type, String start, String end, String filename) {
			this.type = type;
			this.start = start;
			this.end = end;
			this.filename = filename;
		}
		
		@Override
		public String toString() {
			return "[" + type + ", " + start + ", " + end + ", " + filename + "]";
		}
	}
	
	static Stemmer st = new Stemmer(new PorterStemmer());
	
	public static List<String> featuresForToken(Token token) {
		List<String> features = new ArrayList<String>();
		for(int i=1;i<=10;i++) {
			int inum = i;
			Token after = token.getNAfter(i);
			if(after != null) features.add("afters" + inum + "=" + st.getStem(after.getValue()));
			if(after != null) features.add("afters" + (inum+1) + "=" + st.getStem(after.getValue()));
			//if(after != null) features.add("after" + inum + "=" + after.getValue());
			//if(after != null) features.add("after" + (inum+1) + "=" + after.getValue());
			if(after != null) features.add("ui=" + after.getValue());
			Token before = token.getNAfter(-i);
			if(before != null) features.add("befores" + inum + "=" + st.getStem(before.getValue()));
			if(before != null) features.add("befores" + (inum+1) + "=" + st.getStem(before.getValue()));
			//if(before != null) features.add("before" + inum + "=" + before.getValue());
			//if(before != null) features.add("before" + (inum+1) + "=" + before.getValue());
			if(before != null) features.add("ui=" + before.getValue());
		}
		
		return features;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BufferedReader br = new BufferedReader(new FileReader(new File("/home/ptc24/tmp/wsd/reduced.txt")));
		String line = br.readLine();
		
		String type = null;
		String start = null;
		String end = null;
		String filename = null;
		
		Map<String,List<Entry>> entriesByFileName = new HashMap<String,List<Entry>>();
		
		while(line != null) {
			type = line;
			start = br.readLine();
			end = br.readLine();
			filename = br.readLine();
			System.out.println(type + "\t" + start + "\t" + end + "\t" + filename);
			line = br.readLine();
			line = br.readLine();
			Entry e = new Entry(type, start, end, filename);
			if(!entriesByFileName.containsKey(filename)) entriesByFileName.put(filename, new ArrayList<Entry>());
			entriesByFileName.get(filename).add(e);
		}
		
		List<Event> events = new ArrayList<Event>();
		
		for(String fn : entriesByFileName.keySet()) {
			System.out.println(fn);
			for(Entry e : entriesByFileName.get(fn)) {
				System.out.println(e);
			}
			File f = new File(fn);
			f = f.getParentFile();
			Document safDoc = new Builder().build(new File(f, "saf.xml"));
			Document sourceDoc = new Builder().build(new File(f, "source.xml"));			
			ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(sourceDoc, false, false, false, safDoc);
			//NameRecogniser nr = new NameRecogniser();
			//nr.halfProcess(sourceDoc);
			//nr.buildTokenTables(safDoc.getRootElement(), false, false);

			for(Entry e : entriesByFileName.get(fn)) {
				Token t = procDoc.getTokenByStart(e.start);
				if(t != null) {
					List<String> features = featuresForToken(t);
					Event event = new Event(e.type, features.toArray(new String[0]));
					events.add(event);
				}
			}
			//System.out.println(nr.tokensByEnd);
		}
		
		Collections.shuffle(events, new Random(5));
		
		List<Event> trainData = events.subList(0, events.size()/2);
		List<Event> testData = events.subList(events.size()/2, events.size());
		
		if(trainData.size() == 1) trainData.add(trainData.get(0));
		DataIndexer di = new TwoPassDataIndexer(new EventCollectorAsStream(new SimpleEventCollector(trainData)), 1);
		GISModel gm = GIS.trainModel(100, di);
		
		ClassificationEvaluator ce = new ClassificationEvaluator();
		
		for(Event event : testData) {
			double [] results = gm.eval(event.getContext());
			String result = gm.getBestOutcome(results);
			System.out.println(event.getOutcome() + "\t" + result + "\t" + results[gm.getIndex(event.getOutcome())] + "\t" + StringTools.arrayToList(event.getContext()));
			ce.logEvent(event.getOutcome(), result);
		}
		System.out.println(ce.getAccuracy());
		System.out.println(ce.getKappa());
		ce.pprintConfusionMatrix();
		ce.pprintPrecisionRecallEval();

		
	}

}
