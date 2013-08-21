package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.tartarus.snowball.ext.EnglishStemmer;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneChemicalIndex;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.VectorCollector;
import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptc.experimental.ngramtfdf.NGramTfDf;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.misc.Stemmer;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class ClusterAnalyser {
	
	public static int overlapDocs(Map<Integer,Float> resultsVector, Map<Integer,Float> cluster) {
		int overlap = 0;
		for(Integer i : cluster.keySet()) {
			if(resultsVector.containsKey(i)) overlap++;
		}
		return overlap;
	}
	
	public static void analyseCluster(List<Integer> docs, IndexReader ir, DocVectorSimilarity similarity, double threshold) throws Exception {
		Map<Integer,Float> cluster = new HashMap<Integer,Float>();
		for(Integer i : docs) {
			cluster.put(i, 1.0f);
		}
		analyseCluster(cluster, ir, similarity, threshold);
	}

	public static void tfIdfAnalyseCluster(Map<Integer,Float> cluster, IndexReader ir) throws Exception {
		List<File> clusterFiles = new ArrayList<File>();
		for(Integer i : cluster.keySet()) {
			clusterFiles.add(new File(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source")));
		}
		NGramTfDf ngtd = NGramTfDf.analyseFiles(clusterFiles);
		ngtd.calculateNGrams();
		Bag<String> tf = ngtd.getDfBag(1);
		tf.discardInfrequent(2);
		Map<String,Double> tfIdf = new HashMap<String,Double>();
		int numDocs = ir.numDocs();
		IndexSearcher is = new IndexSearcher(ir);
		for(String s : tf.getSet()) {
			//System.out.println(s);
			int docFreq = 0;
			if(s.matches("\\S+")) {
				docFreq = ir.docFreq(new Term("txt", s));
			} else {
				PhraseQuery pq = new PhraseQuery();
				for(String ss : StringTools.arrayToList(s.split("\\s+"))) pq.add(new Term("txt", ss));
				VectorCollector vc = new VectorCollector();
				is.search(pq, vc);
				docFreq = vc.getResultsVector().size();
			}
			double idf = Math.log(numDocs) - Math.log(docFreq);
			tfIdf.put(s, tf.getCount(s) * idf);
		}
		for(String s : StringTools.getSortedList(tfIdf)) {
			System.out.println(s + "\t" + tfIdf.get(s));
		}	
	}
	
	public static Map<String,Double> excessAnalyseCluster(Map<Integer,Float> cluster, IndexReader ir, double threshold, boolean enriched) throws Exception {
		LuceneChemicalIndex lci = new LuceneIndexerSearcher(false).getLci();
		Set<String> inchis = new HashSet<String>();
		Set<String> onts = new HashSet<String>();
		
		List<File> clusterFiles = new ArrayList<File>();
		for(Integer i : cluster.keySet()) {
			clusterFiles.add(new File(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source")));
			if(enriched) {
				TermFreqVector tvf = ir.getTermFreqVector(i, "InChI");
				if(tvf != null) {
					String [] termArray = tvf.getTerms();
					for(int j=0;j<termArray.length;j++) {
						inchis.add(termArray[j]);
					}				
				}
				tvf = ir.getTermFreqVector(i, "Ontology");
				if(tvf != null) {
					String [] termArray = tvf.getTerms();
					for(int j=0;j<termArray.length;j++) {
						onts.add(termArray[j]);
					}				
				}				
			}
		}
		NGramTfDf ngtd = NGramTfDf.analyseFiles(clusterFiles);
		ngtd.calculateNGrams();
		Bag<String> df = ngtd.getDfBag(1);
		df.discardInfrequent(2);
		Map<String,Double> scores = new HashMap<String,Double>();
		int numDocs = ir.numDocs();
		int clusterSize = cluster.size();
		double scaleFactor = clusterSize * 1.0 / numDocs;
		IndexSearcher is = new IndexSearcher(ir);
		for(String s : df.getSet()) {
			//System.out.println(s);
			int docFreq = 0;
			Query q;
			if(s.matches("\\S+")) {
				TermQuery tq = new TermQuery(new Term("txt", s));
				q = tq;
				//docFreq = ir.docFreq(new Term("txt", s));
			} else {
				PhraseQuery pq = new PhraseQuery();
				for(String ss : StringTools.arrayToList(s.split("\\s+"))) pq.add(new Term("txt", ss));
				q = pq;
			}
			VectorCollector vc = new VectorCollector();
			is.search(q, vc);
			docFreq = vc.getResultsVector().size();
			double score;
			double expected = scaleFactor * docFreq;
			double excess = df.getCount(s) - expected;
			score = excess / clusterSize;				
			if(score > threshold) scores.put(s, score);
		}
		Stemmer st = new Stemmer(new EnglishStemmer());
		Map<String,List<String>> stems = st.wordsToStems(df.getSet());
		for(String stem : stems.keySet()) {
			List<String> words = stems.get(stem);
			if(words.size() > 1) {
				BooleanQuery bq = new BooleanQuery(true);
				for(String word : words) {
					bq.add(new BooleanClause(new TermQuery(new Term("txt", word)), Occur.SHOULD));
				}
				VectorCollector vc = new VectorCollector();
				is.search(bq, vc);
				double expected = scaleFactor * vc.getResultsVector().size();
				int overlap = overlapDocs(vc.getResultsVector(), cluster);
				double excess = overlap - expected;
				double score = excess / clusterSize;
				if(score > threshold) {
					df.add(stems.get(stem).toString(), overlap);
					scores.put(stems.get(stem).toString(), score);
				}
			}
		}
		Map<String,List<String>> termStems = ngtd.ngramsByStem();
		for(String stem : termStems.keySet()) {
			List<String> multiWords = termStems.get(stem);
			if(multiWords.size() > 1) {
				BooleanQuery bq = new BooleanQuery(true);
				for(String multiWord : multiWords) {
					PhraseQuery pq = new PhraseQuery();
					for(String ss : StringTools.arrayToList(multiWord.split("\\s+"))) pq.add(new Term("txt", ss));
					bq.add(new BooleanClause(pq, Occur.SHOULD));
				}
				VectorCollector vc = new VectorCollector();
				is.search(bq, vc);
				double expected = scaleFactor * vc.getResultsVector().size();
				int overlap = overlapDocs(vc.getResultsVector(), cluster);
				double excess = overlap - expected;
				double score = excess / clusterSize;
				if(score > threshold) {
					df.add(termStems.get(stem).toString(), overlap);
					scores.put(termStems.get(stem).toString(), score);
				}
			}
		}
		if(enriched) {
			for(String inchi : inchis) {
				Term luceneTerm = new Term("InChI", inchi);
				Query q = new TermQuery(luceneTerm);
				VectorCollector vc = new VectorCollector();
				is.search(q, vc);
				double expected = scaleFactor * vc.getResultsVector().size();
				int overlap = overlapDocs(vc.getResultsVector(), cluster);
				if(overlap < 2) continue;
				double excess = overlap - expected;
				double score = excess / clusterSize;
				
				if(score > threshold) {
					String s = "InChi: " + lci.getName(lci.hitsByInChI(inchi));
					scores.put(s, score);
					df.add(s, overlap);						
				}
			}
			
			Map<String,Set<String>> ontQs = OBOOntology.getInstance().queriesForIds(onts);
			
			for(String ontQ : ontQs.keySet()) {
				/*BooleanQuery bq = new BooleanQuery(true);
				if(ontQs.get(ontQ).size() > BooleanQuery.getMaxClauseCount()) continue;
				for(String ont : ontQs.get(ontQ)) {
					bq.add(new BooleanClause(new TermQuery(new Term("Ontology", ont)), Occur.SHOULD));
				}
				VectorCollector vc = new VectorCollector();
				is.search(bq, vc);*/
				VectorCollector vc = OntologyQueryCache.getResultsStatic(ontQ, ontQs.get(ontQ), is);
				Map<Integer,Float> results = vc.getResultsVector();
				double expected = scaleFactor * results.size();
				int overlap = overlapDocs(vc.getResultsVector(), cluster);
				if(overlap < 2) continue;
				double excess = overlap - expected;
				double score = excess / clusterSize;
				if(score > threshold) {
					String s = ontQ + " " + OBOOntology.getInstance().getNameForID(ontQ);
					scores.put(s, score);
					df.add(s, overlap);						
				}
			}			
		}
		
		//for(String s : StringTools.getSortedList(scores)) {
		//	System.out.println(s + "\t" + scores.get(s) + "\t" + df.getCount(s));
		//}
		return scores;
	}
	
	public static Map<String,Double> simpleExcessAnalyseCluster(Map<Integer,Float> cluster, IndexReader ir, double threshold) throws Exception {
		List<File> clusterFiles = new ArrayList<File>();
		Bag<String> df = new Bag<String>();
		for(Integer i : cluster.keySet()) {
			clusterFiles.add(new File(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source")));
			TermFreqVector tvf = ir.getTermFreqVector(i, "txt");
			if(tvf != null) {
				String [] termArray = tvf.getTerms();
				for(int j=0;j<termArray.length;j++) {
					String term = termArray[j];
					if(!TermSets.getClosedClass().contains(term) && term.matches(".*[A-Za-z].*")) df.add(term);
				}				
			}				
		}
		df.discardInfrequent(2);
		Map<String,Double> scores = new HashMap<String,Double>();
		int numDocs = ir.numDocs();
		int clusterSize = cluster.size();
		double scaleFactor = clusterSize * 1.0 / numDocs;
		for(String s : df.getSet()) {
			int docFreq = ir.docFreq(new Term("txt", s));
			double score;
			double expected = scaleFactor * docFreq;
			double excess = df.getCount(s) - expected;
			score = excess / clusterSize;				
			if(score > threshold) scores.put(s, score);
		}
		return scores;
	}

	
	public static void analyseCluster(Map<Integer,Float> cluster, IndexReader ir, DocVectorSimilarity similarity, double threshold) throws Exception {
		LuceneChemicalIndex lci = new LuceneIndexerSearcher(false).getLci();
		List<File> clusterFiles = new ArrayList<File>();
		Bag<String> dfs = new Bag<String>();
		Set<String> inchis = new HashSet<String>();
		Set<String> onts = new HashSet<String>();
		for(Integer i : cluster.keySet()) {
			cluster.put(i, 1.0f);
			TermFreqVector tvf = ir.getTermFreqVector(i, "txt");
			String [] termArray = tvf.getTerms();
			for(int j=0;j<termArray.length;j++) {
				dfs.add(termArray[j]);
			}
			if(false) {
				tvf = ir.getTermFreqVector(i, "InChI");
				if(tvf != null) {
					termArray = tvf.getTerms();
					for(int j=0;j<termArray.length;j++) {
						inchis.add(termArray[j]);
					}				
				}
				tvf = ir.getTermFreqVector(i, "Ontology");
				if(tvf != null) {
					termArray = tvf.getTerms();
					for(int j=0;j<termArray.length;j++) {
						onts.add(termArray[j]);
					}				
				}				
			}
			

			clusterFiles.add(new File(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source")));
		}
		Stemmer st = new Stemmer(new EnglishStemmer());
		Map<String,List<String>> stems = st.wordsToStems(dfs.getSet());

		dfs.discardInfrequent(2);
		NGramTfDf ngtd = NGramTfDf.analyseFiles(clusterFiles);
		ngtd.calculateNGrams();
		Bag<String> bs = ngtd.getDfBag(2);
		bs.discardInfrequent(2);
		Map<String,List<String>> termStems = ngtd.ngramsByStem();

		Map<String,Double> scores = new HashMap<String,Double>();
		Map<String,Integer> overlaps = new HashMap<String,Integer>();
		IndexSearcher is = new IndexSearcher(ir);
		int docTotal = ir.numDocs();
		for(String term : dfs.getSet()) {
			if(TermSets.getClosedClass().contains(term) || term.matches("[^A-Za-z]+")) continue;
			Term luceneTerm = new Term("txt", term);
			Query q = new TermQuery(luceneTerm);
			VectorCollector vc = new VectorCollector();
			is.search(q, vc);
			double score = similarity.similarity(cluster, vc.getResultsVector());
			if(score > threshold) {
				int overlap = overlapDocs(vc.getResultsVector(), cluster);
				if(overlap > 1) {
					scores.put(term, score);
					overlaps.put(term, overlap);						
				}
			}
		}
		for(String stem : stems.keySet()) {
			List<String> words = stems.get(stem);
			if(words.size() > 1) {
				BooleanQuery bq = new BooleanQuery(true);
				for(String word : words) {
					bq.add(new BooleanClause(new TermQuery(new Term("txt", word)), Occur.SHOULD));
				}
				VectorCollector vc = new VectorCollector();
				is.search(bq, vc);
				double score = similarity.similarity(cluster, vc.getResultsVector());
				if(score > threshold) {
					String s = words.toString();
					int overlap = overlapDocs(vc.getResultsVector(), cluster);
					if(overlap > 1) {
						scores.put(s, score);
						overlaps.put(s, overlap);						
					}
				}
			}
		}
		for(String stem : termStems.keySet()) {
			List<String> multiWords = termStems.get(stem);
			if(multiWords.size() > 1) {
				BooleanQuery bq = new BooleanQuery(true);
				for(String multiWord : multiWords) {
					PhraseQuery pq = new PhraseQuery();
					for(String ss : StringTools.arrayToList(multiWord.split("\\s+"))) pq.add(new Term("txt", ss));
					bq.add(new BooleanClause(pq, Occur.SHOULD));
				}
				VectorCollector vc = new VectorCollector();
				is.search(bq, vc);
				double score = similarity.similarity(cluster, vc.getResultsVector());
				if(score > threshold) {
					String s = multiWords.toString();
					int overlap = overlapDocs(vc.getResultsVector(), cluster);
					if(overlap > 1) {
						scores.put(s, score);
						overlaps.put(s, overlap);						
					}
				}
			}
		}
		for(String s : bs.getList()) {
			if(!s.matches(".*\\s+.*")) continue;
			PhraseQuery pq = new PhraseQuery();
			for(String ss : StringTools.arrayToList(s.split("\\s+"))) pq.add(new Term("txt", ss));
			VectorCollector vc = new VectorCollector();
			is.search(pq, vc);
			double score = similarity.similarity(cluster, vc.getResultsVector());
			if(score > threshold) {
				scores.put(s, score);
				overlaps.put(s, overlapDocs(vc.getResultsVector(), cluster));
			}
		}
		

		if(false) {
			for(String inchi : inchis) {
				Term luceneTerm = new Term("InChI", inchi);
				Query q = new TermQuery(luceneTerm);
				VectorCollector vc = new VectorCollector();
				is.search(q, vc);
				double score = similarity.similarity(cluster, vc.getResultsVector());
				if(score > threshold) {
					int overlap = overlapDocs(vc.getResultsVector(), cluster);
					if(overlap > 1) {
						String s = "InChi: " + lci.getName(lci.hitsByInChI(inchi));
						scores.put(s, score);
						overlaps.put(s, overlap);						
					}
				}
			}
			
			Map<String,Set<String>> ontQs = OBOOntology.getInstance().queriesForIds(onts);
			
			for(String ontQ : ontQs.keySet()) {
				BooleanQuery bq = new BooleanQuery(true);
				if(ontQs.get(ontQ).size() > BooleanQuery.getMaxClauseCount()) continue;
				for(String ont : ontQs.get(ontQ)) {
					bq.add(new BooleanClause(new TermQuery(new Term("Ontology", ont)), Occur.SHOULD));
				}
				VectorCollector vc = new VectorCollector();
				is.search(bq, vc);
				double score = similarity.similarity(cluster, vc.getResultsVector());
				if(score > threshold) {
					int overlap = overlapDocs(vc.getResultsVector(), cluster);
					if(overlap > 1) {
						String s = ontQ + " " + OBOOntology.getInstance().getNameForID(ontQ);
						scores.put(s, score);
						overlaps.put(s, overlap);						
					}
				}
			}			
		}
		
		/*for(String ont : onts) {
			Term luceneTerm = new Term("Ontology", ont);
			Query q = new TermQuery(luceneTerm);
			VectorCollector vc = new VectorCollector();
			is.search(q, vc);
			double score = similarity.similarity(cluster, vc.getResultsVector());
			if(score > threshold) {
				int overlap = overlapDocs(vc.getResultsVector(), cluster);
				if(overlap > 1) {
					String s = ont;
					scores.put(s, score);
					overlaps.put(s, overlap);						
				}
			}
		}*/

		
		for(String term : StringTools.getSortedList(scores)) {
			System.out.println(term + "\t" + scores.get(term) + "\t" + overlaps.get(term));
		}
	}

	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexSearcher is = lis.getIndexSearcher();
		
		Stemmer stemmerTools = new Stemmer(new EnglishStemmer());
		
		//QueryParser qp = new Oscar3QueryParser("txt", new Oscar3Analyzer(), lis, false);
		//Query q = qp.parse("NaCl");
		
		String queryTerm = "content";
		//PhraseQuery pq = new PhraseQuery();
		//pq.add(new Term("txt", "aromatase"));
		//pq.add(new Term("txt", "inhibitors"));
		//Query q = new TermQuery(new Term("txt", queryTerm));
		Query q = new StemQuery(new Term("txt", queryTerm), stemmerTools);
		//q = pq;
		VectorCollector vc = new VectorCollector();
		is.search(q, vc);
		Map<String,Double> scores = simpleExcessAnalyseCluster(vc.getResultsVector(), lis.getIndexReader(), 0.01);
		for(String s : StringTools.getSortedList(scores)) {
			System.out.println(s + "\t" + scores.get(s));
		}

		//tfIdfAnalyseCluster(vc.getResultsVector(), lis.getIndexReader());
		//excessAnalyseCluster(vc.getResultsVector(), lis.getIndexReader(), 0.1, true);
		//analyseCluster(vc.getResultsVector(), lis.getIndexReader(), new CosineSimilarity(), 0.01);
	}
	
}
