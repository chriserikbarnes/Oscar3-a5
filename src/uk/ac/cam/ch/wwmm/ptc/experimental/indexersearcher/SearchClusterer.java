package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.VectorCollector;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class SearchClusterer {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		LuceneIndexerSearcher lis = new LuceneIndexerSearcher(false);
		IndexSearcher is = lis.getIndexSearcher();
		IndexReader ir = lis.getIndexReader();
		
		List<String> docFiles = new ArrayList<String>();
		for(int i=0;i<ir.numDocs();i++) {
			docFiles.add(ir.document(i).getField("filename").stringValue().replaceAll("markedup", "source"));
		}
		
		TermEnum textEnum = ir.terms();
		Map<String,Integer> docFreqs = new HashMap<String,Integer>();
		while(textEnum.next()) {
			Term t = textEnum.term();
			if("txt".equals(t.field())) {
				String text = t.text();
				if(TermSets.getClosedClass().contains(text)) continue;
				if(!text.matches(".*[A-Za-z].*")) continue;
				int docFreq = ir.docFreq(t);
				if(docFreq > 1) {
					docFreqs.put(text, ir.docFreq(t));
				}
			}			
		}
		List<String> dfl = StringTools.getSortedList(docFreqs);

		List<Query> queries = new ArrayList<Query>();
		for(int i=0;i<50;i++) {
			queries.add(new TermQuery(new Term("txt", dfl.get(i))));
			System.out.println(dfl.get(i));
		}
		
		for(int i=0;i<10;i++) {
			Map<Integer,Integer> bestClusters = new HashMap<Integer,Integer>();
			Map<Integer,Float> bestClusterScores = new HashMap<Integer,Float>();
			List<Map<Integer,Float>> clusters = new ArrayList<Map<Integer,Float>>();
			for(int j=0;j<queries.size();j++) {
				clusters.add(new HashMap<Integer,Float>());
				VectorCollector vc = new VectorCollector();
				is.search(queries.get(j), vc);
				//System.out.println(vc.getResultsVector());
				for(Integer k : vc.getResultsVector().keySet()) {
					float score = vc.getResultsVector().get(k);
					if(score < 0.001) continue;
					if(!bestClusterScores.containsKey(k) || bestClusterScores.get(k) < score) {
						bestClusters.put(k, j);
						bestClusterScores.put(k, score);
					}
				}
			}
			for(Integer j : bestClusters.keySet()) {
				clusters.get(bestClusters.get(j)).put(j, bestClusterScores.get(j));
			}
			//for(Map<Integer,Float> cluster : clusters) System.out.println(cluster);
			queries.clear();
			for(int j=0;j<clusters.size();j++) {
				System.out.println("Size: " + clusters.get(j).size());
				/*if(i == 9) {
					for(Integer k : clusters.get(j).keySet()) {
						System.out.println(docFiles.get(k) + "\t" + bestClusterScores.get(k));
					}
				}*/
				//if(i == 9) ClusterAnalyser.excessAnalyseCluster(clusters.get(j), lis.getIndexReader(), 0.2, true);
				Map<String,Double> scores = ClusterAnalyser.simpleExcessAnalyseCluster(clusters.get(j), lis.getIndexReader(), 0.1);
				BooleanQuery bq = new BooleanQuery(false);
				List<String> terms = StringTools.getSortedList(scores);
				if(terms.size() > 20) terms = terms.subList(0, 20);
				for(String s : terms) {
					System.out.println(s + "\t" + scores.get(s));
					TermQuery tq = new TermQuery(new Term("txt", s));
					tq.setBoost(scores.get(s).floatValue());
					bq.add(new BooleanClause(tq, Occur.SHOULD));
				}
				System.out.println();
				queries.add(bq);
			}
			System.out.println();
		}
		List<Map<Integer,Float>> clusters = new ArrayList<Map<Integer,Float>>();
		final Map<Integer,Integer> clusterSizes = new HashMap<Integer,Integer>();
		for(int j=0;j<queries.size();j++) {
			VectorCollector vc = new VectorCollector();
			is.search(queries.get(j), vc);
			final Map<Integer,Float> cluster = new HashMap<Integer,Float>();
			//System.out.println(vc.getResultsVector());
			for(Integer k : vc.getResultsVector().keySet()) {
				float score = vc.getResultsVector().get(k);
				if(score < 0.2) continue;
				cluster.put(k, score);
			}
			clusters.add(cluster);
			clusterSizes.put(j, cluster.size());
		}
		List<Integer> clustersBySize = new ArrayList<Integer>(clusterSizes.keySet());
		Collections.sort(clustersBySize, Collections.reverseOrder(new Comparator<Integer>() {
			@SuppressWarnings("unchecked")
			public int compare(Integer o1, Integer o2) {
				return clusterSizes.get(o1).compareTo(clusterSizes.get(o2));
			}
		}));
		
		for(Integer j : clustersBySize) {
			final Map<Integer,Float> cluster = clusters.get(j);
			System.out.println("Size:\t" + cluster.size());
			List<Integer> list = new ArrayList<Integer>(cluster.keySet());
			Collections.sort(list, Collections.reverseOrder(new Comparator<Integer>() {
				@SuppressWarnings("unchecked")
				public int compare(Integer o1, Integer o2) {
					return cluster.get(o1).compareTo(cluster.get(o2));
				}
			}));
			for(Integer k : list) {
				System.out.println(docFiles.get(k) + "\t" + cluster.get(k));
			}
			ClusterAnalyser.excessAnalyseCluster(cluster, lis.getIndexReader(), 0.2, true);
			System.out.println();
		}


	}

}
