package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher.classify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.maxent.DataIndexer;
import opennlp.maxent.Event;
import opennlp.maxent.EventCollectorAsStream;
import opennlp.maxent.GIS;
import opennlp.maxent.GISModel;
import opennlp.maxent.TwoPassDataIndexer;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermFreqVector;
import org.tartarus.snowball.ext.EnglishStemmer;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.misc.SimpleEventCollector;
import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class DocClassifier {

	public static Event docToEvent(IndexReader ir, int doc, String cue) throws Exception {
		Stemmer st = new Stemmer(new EnglishStemmer());
		List<String> words = new ArrayList<String>();
		boolean hasCue = false;
		TermFreqVector tvf = ir.getTermFreqVector(doc, "txt");
		String [] termArray = tvf.getTerms();
		int [] termFreqs = tvf.getTermFrequencies();
		for(int j=0;j<termArray.length;j++) {
			if(TermSets.getClosedClass().contains(termArray[j])) {
				//ignore
			} else if(termArray[j].equals(cue)) {
				hasCue = true;
				//words.add(termArray[j].intern());
			} else {
				//for(int k=0;k<termFreqs[j];k++) words.add(termArray[j].intern());
				words.add(st.getStem(termArray[j]).intern());
				words.add(termArray[j].intern());
			}
		}
		String c = hasCue ? "TRUE" : "FALSE";
		return new Event(c, words.toArray(new String[0]));
	}
	
	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexReader ir = lis.getIndexReader();
		
		int numDocs = ir.maxDoc();
		
		String c = "cholesterol";
		
		List<Event> events = new ArrayList<Event>();
		for(int i=0;i<numDocs/2;i++) {
			events.add(docToEvent(ir, i, c));
		}
		
		DataIndexer di = null;
		di = new TwoPassDataIndexer(new EventCollectorAsStream(new SimpleEventCollector(events)), 3);
		GISModel gm = GIS.trainModel(100, di);
	
		Map<String,Double> byProb = new HashMap<String,Double>();
		Map<String,String> res = new HashMap<String,String>();
		int mp = 0;
		int mn = 0;
		for(int i=numDocs/2;i<numDocs;i++) { 
			Event e = docToEvent(ir, i, c);
			//System.out.println(gm.getBestOutcome(gm.eval(e.getContext())));
			double prob = gm.eval(e.getContext())[gm.getIndex("TRUE")];
			//System.out.println(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source"));
			String name = ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source");
			byProb.put(name, prob);
			res.put(name, e.getOutcome());
			if(e.getOutcome().equals("TRUE")) {
				mp++;
			} else {
				mn++;
			}
		}
		
		int tp=0;
		int fp=0;
		int fn=mp;
		int tn=mn;
		for(String s : StringTools.getSortedList(byProb)) {
			//System.out.println(s);
			//System.out.println(byProb.get(s) + "\t" + res.get(s));
			if(res.get(s).equals("TRUE")) {
				tp++;
				fn--;
			} else {
				fp++;
				tn--;
			}
			// Precision/Recall curve
			System.out.println((tp * 1.0 / (tp+fn)) + "," + (tp * 1.0 / (tp+fp)));
			// ROC
			//System.out.println((fp * 1.0 / (fp+tn)) + "," + (tp * 1.0 / (tp+fn)));
		}
	}

}
