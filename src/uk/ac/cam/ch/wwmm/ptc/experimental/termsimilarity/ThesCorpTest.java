package uk.ac.cam.ch.wwmm.ptc.experimental.termsimilarity;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class ThesCorpTest {

	public static void printClusters(Set<Set<String>> clusters) {
		System.out.println("==================================");
		for(Set<String> cluster : clusters) {
			for(String s : cluster) {
				System.out.println(s);
			}
			System.out.println();
		}
		System.out.println("==================================");
	}
	
	public static void printFuzzyClusters(List<Map<String,Double>> clusters, SimilarityExtractor se) {
		System.out.println("==================================");
		for(Map<String,Double> cluster : clusters) {
			List<String> memberList = StringTools.getSortedList(cluster);
			for(String s : memberList) {
				System.out.printf("%s %.1f%%\n", s, cluster.get(s) * 100.0);
			}
			System.out.println();
			Map<String,Double> center = se.getVectorForFuzzyTermSet(cluster);
			se.explainWeightVector(center);
			System.out.println();
		}
		System.out.println("==================================");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		List<File> files = new ArrayList<File>();			
		//files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/morersc"), "markedup.xml");
		//files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/paperset1_"), "markedup.xml");
		files = FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/oscarworkspace/corpora/paperset1"), "markedup.xml");
		//files.addAll(FileTools.getFilesFromDirectoryByName(new File("/home/ptc24/newows/corpora/BioIE"), "markedup.xml"));

		FeatureVectorExtractor fve = new InlineFVE(files);
		SimilarityExtractor se = new SimilarityExtractor(fve, new MinLRDiceSimilarity(), new PMIWeighting());
		//FeatureVectorExtractor fve = new PaperFVE(files);
		//SimilarityExtractor se = new SimilarityExtractor(fve, new MinLRDiceSimilarity(), new TTestWeighting());
		//FeatureVectorExtractor fve = new EntityPaperFVE(files);
		//SimilarityExtractor se = new SimilarityExtractor(fve, new DiceSimilarity(), new PMIWeighting());

		/*SimilarityMatrix sm = new SimilarityMatrix(se, se.getMostFrequentTerms(5000));
		QTClusterer qt = new QTClusterer(sm);
		qt.makeClusters(0.05);

		Set<Set<String>> clusterrs = new LinkedHashSet<Set<String>>(qt.getClustersOfNames());
		printClusters(clusterrs);
		
		if(true) return;*/
		
		/*KMeansClusterer kmc = new KMeansClusterer(se);
		Set<Set<String>> clusters = kmc.centersToClusters(kmc.getInitialCenters(0.05));
		for(int i=0;i<10;i++) {
			clusters = kmc.KMeansIteration(clusters, 0.05);
			printClusters(clusters);
		}*/
		FuzzyCMeansClusterer fcmc = new FuzzyCMeansClusterer(se);
		List<Map<String,Double>> clusters = new ArrayList<Map<String,Double>>();
		clusters = fcmc.addNewCenters(clusters, 0.01);
			//fcmc.centersToFuzzyClusters(fcmc.getInitialCenters(0.01));
		List<Map<String,Double>> oldclusters = null;
		boolean converged = false;
		while(!converged) {
			oldclusters = clusters;
			clusters = fcmc.CMeansIteration(clusters, 0.05);
			clusters = fcmc.mergeClusters(clusters, 0.9);
			clusters = fcmc.filterClusters(clusters, 1.0);
			printFuzzyClusters(clusters, se);
			converged = fcmc.checkConvergence(oldclusters, clusters);
			Set<String> clusteredTerms = fcmc.getClusteredTerms(clusters);
			System.out.println(clusteredTerms.size() + " terms clustered");
			double avgFreq = se.averageFrequency(clusteredTerms);
			int words = (int)(avgFreq * clusteredTerms.size());
			Set<String> unclusteredTerms = se.complementOfSet(clusteredTerms);
			double avgFreqUnclustered = se.averageFrequency(unclusteredTerms);
			int wordsUnclustered = (int)(avgFreqUnclustered * unclusteredTerms.size());
			System.out.println("Average frequency: " + avgFreq + " (" + words + ")");
			System.out.println("Average frequency of unclustered: " + avgFreqUnclustered + " (" + wordsUnclustered + ")");
			System.out.println((double)words / (double)(words + wordsUnclustered));
		}
		
		
		/*Map<String,Double> similar = se.getSimilarToTerm("organic");
		List<String> similarList = StringTools.getSortedList(similar);
		for(String s : similarList) {
			System.out.println(s + " " + similar.get(s));
		}
		System.out.println();
		
		Set<String> ss = new HashSet<String>();
		ss.add("organic");
		ss.add("physical");
		ss.add("inorganic");
		ss.add("biological");
		similar = se.getSimilarToTermSet(ss);
		similarList = StringTools.getSortedList(similar);
		for(String s : similarList) {
			System.out.println(s + " " + similar.get(s));
		}
		System.out.println();*/
		
	}

}
