package uk.ac.cam.ch.wwmm.ptc.experimental.indexersearcher;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Element;

import org.apache.commons.math.distribution.ChiSquaredDistribution;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.interfaces.IMolecule;

import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.LuceneIndexerSearcher;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.UserQuery;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.UserQuery.ResultsType;
import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;
import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**Experimental code to find significant term-term associations.
 * 
 * @author ptc24
 *
 */
public class TextMiner {

	static String sertraline = "InChI=1/C17H17Cl2N/c1-20-17-9-7-12(13-4-2-3-5-14(13)17)11-6-8-15(18)16(19)10-11/h2-6,8,10,12,17,20H,7,9H2,1H3/t12-,17-/m0/s1";
	
	static boolean doSimilarity = false;
	
	class Bags {
		Bag<String> txtBag;
		Bag<String> ontBag;
		Bag<String> expandedOntBag;
		Bag<String> inchiBag;
		Bag<String> similarityBag;
		
		public Bags() {
			txtBag = new Bag<String>();
			ontBag = new Bag<String>();
			inchiBag = new Bag<String>();
			expandedOntBag = null;
			similarityBag = null;
		}
		
		public Bag<String> getExpandedOntBag() {
			if(expandedOntBag == null) {
				expandedOntBag = expandOntologyBag(ontBag);
			}
			return expandedOntBag;
		}

		public Bag<String> getSimilarityBag() throws Exception {
			if(similarityBag == null) {
				similarityBag = makeSimilarityBag(inchiBag);
			}
			return similarityBag;
		}
	}

	LuceneIndexerSearcher lis;
	Bags corpusBags;
	SimilarityMatrix sm;
	static Fingerprinter fingerprinter = new Fingerprinter();
	String heldOutInchi;
	
	public TextMiner(LuceneIndexerSearcher lis) throws Exception {
		this.lis = lis;
		if(doSimilarity) this.sm = makeSimilarityMatrix();
		//System.out.println("Making corpus bags");
		corpusBags = makeCorpusBags();
		heldOutInchi = null;
	}

	private SimilarityMatrix makeSimilarityMatrix() throws Exception {
		Set<String> inchis = new HashSet<String>();
		IndexSearcher is = lis.getIndexSearcher();
		IndexReader ir = is.getIndexReader();
		for(int i=0;i<ir.numDocs();i++) {
			TermFreqVector tfv = ir.getTermFreqVector(i, "InChI");
			if(tfv != null) {
				String [] terms = tfv.getTerms();
				for(int j=0;j<terms.length;j++) inchis.add(terms[j]);
			}
		}
		ir.close();
		return new SimilarityMatrix(inchis);
	}

	private Bags makeCorpusBags() throws Exception {
		IndexSearcher is = lis.getIndexSearcher();
		IndexReader ir = is.getIndexReader();
		Bags bags = new Bags();
		for(int i=0;i<ir.numDocs();i++) {
			addToBag(ir.getTermFreqVector(i, "txt"), bags.txtBag);
			addToBag(ir.getTermFreqVector(i, "Ontology"), bags.ontBag);
			addToBag(ir.getTermFreqVector(i, "InChI"), bags.inchiBag);
		}
		ir.close();
		return bags;
	}
	
	private Bags makeBagsFromHits(Hits h, IndexReader ir) throws Exception {
		Bags bags = new Bags();
		for(int i=0;i<h.length();i++) {
			addToBag(ir.getTermFreqVector(h.id(i), "txt"), bags.txtBag);
			addToBag(ir.getTermFreqVector(h.id(i), "Ontology"), bags.ontBag);
			addToBag(ir.getTermFreqVector(h.id(i), "InChI"), bags.inchiBag);
		}
		//ir.close();
		return bags;		
	}
	
	private Bags makeBagsFromQuery(Query q) throws Exception {
		IndexSearcher is = lis.getIndexSearcher();
		IndexReader ir = is.getIndexReader();
		Hits h = is.search(q);
		Bags bags = makeBagsFromHits(h, ir);
		ir.close();
		return bags;
	}
	
	private void addToBag(TermFreqVector tvf, Bag<String> bag) {
		if(tvf == null) return;
		String [] terms = tvf.getTerms();
		int [] termFreqs = tvf.getTermFrequencies();
		for(int i=0;i<tvf.size();i++) {	
			bag.add(terms[i].intern(), termFreqs[i]);
			//bag.add(terms[i].intern());//, termFreqs[i]);
		}
	}
	
	private Bag<String> expandOntologyBag(Bag<String> ontologyBag) {
		Bag<String> results = new Bag<String>();
		OBOOntology oo = OBOOntology.getInstance();
		for(String id : ontologyBag.getSet()) {
			for(String parent : oo.getIdsForIdWithAncestors(id)) {
				results.add(parent, ontologyBag.getCount(id));
			}
		}
		return results;
	}

	private Map<String,Double> termAssociations(Bags subCorpusBags, Bags corpusBags) throws Exception {
		return termAssociations(subCorpusBags, corpusBags, true);
	}
	
	private Map<String,Double> termAssociations(Bags subCorpusBags, Bags corpusBags, boolean translate) throws Exception {
		int subCorpusSize = subCorpusBags.txtBag.totalCount();
		int corpusSize = corpusBags.txtBag.totalCount();
		double corpusRatio = subCorpusSize * 1.0 / corpusSize;
		Map<String,Double> scores = new HashMap<String,Double>();
		for(String s : subCorpusBags.txtBag.getSet()) {
			int corpusObserved = corpusBags.txtBag.getCount(s);
			int observed = subCorpusBags.txtBag.getCount(s);

			double expected = corpusObserved * corpusRatio;
			double g = 2 * observed * Math.log(observed/expected);
			int observedElsewhere = corpusObserved - observed;
			double expectedElsewhere = corpusObserved * (1.0 - corpusRatio);
			if(observedElsewhere > 0) { 
				g += 2 * observedElsewhere * Math.log(observedElsewhere/expectedElsewhere);
			}
			if(observed > expected) scores.put(s, g);
		}
		//System.out.println("Text assocations");
		for(String s : subCorpusBags.getExpandedOntBag().getSet()) {
			int corpusObserved = corpusBags.getExpandedOntBag().getCount(s);
			int observed = subCorpusBags.getExpandedOntBag().getCount(s);
			
			double expected = corpusObserved * corpusRatio;
			double g = 2 * observed * Math.log(observed/expected);
			int observedElsewhere = corpusObserved - observed;
			double expectedElsewhere = corpusObserved * (1.0 - corpusRatio);
			if(observedElsewhere > 0) { 
				g += 2 * observedElsewhere * Math.log(observedElsewhere/expectedElsewhere);
			}
			if(observed > expected) {
				if(translate) {
					scores.put("ONT: " + OBOOntology.getInstance().getNameForID(s), g);					
				} else {
					scores.put("ONT: " + s, g);										
				}
			}
		}
		//System.out.println("ONT assocations");
		for(String s : subCorpusBags.inchiBag.getSet()) {
			int corpusObserved = corpusBags.inchiBag.getCount(s);
			int observed = subCorpusBags.inchiBag.getCount(s);
			
			double expected = corpusObserved * corpusRatio;
			double g = 2 * observed * Math.log(observed/expected);
			int observedElsewhere = corpusObserved - observed;
			double expectedElsewhere = corpusObserved * (1.0 - corpusRatio);
			if(observedElsewhere > 0) { 
				g += 2 * observedElsewhere * Math.log(observedElsewhere/expectedElsewhere);
			}
			if(observed > expected) {
				if(translate) {
					Set<String> oids = ChemNameDictSingleton.getOntologyIdsFromInChI(s);
					//Set<String> names = ChemNameDictSingleton.getNamesFromInChI(s);
					
					if(oids == null || oids.size() != 1) {
						scores.put(s, g);
					} else {
						String name = OBOOntology.getInstance().getNameForID(oids.iterator().next());
						scores.put("CHEM: " + name, g);
					}					
				} else {
					scores.put(s, g);
				}
			}
		}
		//System.out.println("InChI assocations");
		for(String s : subCorpusBags.getSimilarityBag().getSet()) {
			int corpusObserved = corpusBags.getSimilarityBag().getCount(s);
			int observed = subCorpusBags.getSimilarityBag().getCount(s);
			
			double expected = corpusObserved * corpusRatio;
			double g = 2 * observed * Math.log(observed/expected);
			int observedElsewhere = corpusObserved - observed;
			double expectedElsewhere = corpusObserved * (1.0 - corpusRatio);
			if(observedElsewhere > 0) { 
				g += 2 * observedElsewhere * Math.log(observedElsewhere/expectedElsewhere);
			}
			if(observed > expected) {
				if(translate) {
					String [] sp = s.split("\\s+");
					String simRep = sp[0] + " " + sp[1] + " " + nameForInChI(sp[2]);
					scores.put(simRep, g);					
				} else {
					scores.put(s, g);
				}
			}
		}			
		//System.out.println("InChI similarity assocations");

		return scores;
	}
	
	private List<Map<String,Double>> disjunctiveQuery(List<Query> queries) throws Exception {
		BooleanQuery overall = new BooleanQuery();
		for(Query q : queries) overall.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
		Bags backgroundBags = makeBagsFromQuery(overall);
		List<Map<String,Double>> results = new ArrayList<Map<String,Double>>();
		for(Query q : queries) {
			results.add(termAssociations(makeBagsFromQuery(q), backgroundBags));
		}
		return results;
	}

	private Map<String,Double> conjunctiveQuery(List<Query> queries) throws Exception {
		BooleanQuery andQuery = new BooleanQuery();
		BooleanQuery orQuery = new BooleanQuery();
		for(Query q : queries) {
			andQuery.add(new BooleanClause(q, BooleanClause.Occur.MUST));
			orQuery.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
		}
		Bags andBags = makeBagsFromQuery(andQuery);
		Bags orBags = makeBagsFromQuery(orQuery);
		return termAssociations(andBags, orBags);
	}
	
	private Map<String,Double> queryVsCorpus(Query query) throws Exception {
		//System.out.println("Make bags...");
		Bags queryBags = makeBagsFromQuery(query);
		//System.out.println("Made bags...");
		return termAssociations(queryBags, corpusBags);
	}
	
	private void disjunctiveTable(List<Map<String,Double>> dqr) {
		int columnNo = dqr.size();
		List<List<String>> columns = new ArrayList<List<String>>();
		List<Integer> columnWidths = new ArrayList<Integer>();
		int maxColumnHeight = 0;
		for(Map<String,Double> resultSet : dqr) {
			List<String> column = new ArrayList<String>();
			columns.add(column);
			int columnWidth = 0;
			for(String s : StringTools.getSortedList(resultSet)) {
				if(s.matches("FP\\d+")) continue;
				if(s.startsWith("InChI")) continue;
				double score = resultSet.get(s);
				if(score < 0.0) break;
				column.add(s);
				columnWidth = Math.max(columnWidth, s.length());
			}
			columnWidths.add(columnWidth);
			maxColumnHeight = Math.max(maxColumnHeight, column.size());
		}
		for(int i=0;i<maxColumnHeight;i++) {
			for(int j=0;j<columnNo;j++) {
				if(columns.get(j).size() <= i) {
					System.out.print(StringTools.multiplyString(" ", columnWidths.get(j) + 2));
				} else {
					String s = columns.get(j).get(i);
					System.out.print(s);
					System.out.print(StringTools.multiplyString(" ", columnWidths.get(j) + 2 - s.length()));
				}
			}
			System.out.println();
		}
		

	}
	
	private Set<String> inchiToFingerprints(String inchi) throws Exception {
 		IMolecule mol = ConverterToInChI.getMolFromInChI(inchi);
		if(mol == null) return null;
		BitSet fp = fingerprinter.getFingerprint(mol);
		Set<String> fps = new HashSet<String>();
		for(int i=fp.nextSetBit(0); i>=0; i=fp.nextSetBit(i+1)) {
			fps.add("FP" + i);
		}
		return fps;
	}
	
	private Bag<String> makeFingerprintBag(Bag<String> inchis) {
		try {
			Bag<String> fpBag = new Bag<String>();
			for(String s : inchis.getSet()) {
				if(s.equals(heldOutInchi))  {
					System.out.println("Skipping");
					continue;
				}
				Set<String> fingerprints = inchiToFingerprints(s);
				if(fingerprints != null) {
					for(String fp : fingerprints) fpBag.add(fp, inchis.getCount(s));
				}
			}
			return fpBag;			
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
	private Bag<String> makeSimilarityBag(Bag<String> inchis) throws Exception {
		if(!doSimilarity) return new Bag<String>();
		return sm.similarityEvents(inchis);
	}
	
	private double similarity(Map<String,Double> v1, Map<String,Double> v2) {
		double weightSumSquare1 = 0.0;
		for(String f : v1.keySet()) {
			weightSumSquare1 += v1.get(f) * v1.get(f);
		}
		double weightSumSquare2 = 0.0;
		for(String f : v2.keySet()) {
			weightSumSquare2 += v2.get(f) * v2.get(f);
		}
		double coWeightSum = 0.0;
		for(String f : v1.keySet()) {
				if(v2.containsKey(f)) {
					coWeightSum += (v1.get(f) * v2.get(f));				
				}
		}
		return coWeightSum / (Math.sqrt(weightSumSquare1) * Math.sqrt(weightSumSquare2));
	}
	
	private Map<String,Double> reduceToFingerprints(Map<String,Double> v) {
		Map<String,Double> newV = new HashMap<String,Double>();
		for(String s : v.keySet()) {
			if(s.matches("FP\\d+")) newV.put(s, v.get(s));
		}
		return newV;
	}
	
	private Map<String,Double> inchiToVector(String inchi) throws Exception {
		Map<String,Double> v = new HashMap<String,Double>();
		Set<String> set = inchiToFingerprints(inchi);
		if(set == null) return v;
		for(String fp : set) v.put(fp, 1.0);
		return v;
	}
	
	public void setHeldOutInchi(String heldOutInchi) {
		this.heldOutInchi = heldOutInchi;
	}
	
	public SciXMLDocument associations(Hits hits, UserQuery uq) throws Exception {		
		Bags bags = makeBagsFromHits(hits, lis.getIndexReader());
		Map<String,Double> assocs = termAssociations(bags, corpusBags, false);
		List<String> assocsList = new ArrayList<String>();
		for(String a : assocs.keySet()) {
			// 95% confidence for 1 DOF chi-squared
			if(assocs.get(a) > 3.8414589250873323) assocsList.add(a);
			//if(assocs.get(a) > 0.0) assocsList.add(a);
		}
		StringTools.sortStringList(assocsList, assocs);
		
		SciXMLDocument sxd = new SciXMLDocument();
		sxd.setTitle("Term Associations");
		
		Element list = sxd.addList();

		
		for(String a : assocsList) {
			//System.out.println(a + "\t" + assocs.get(a));
			Element li = new Element("LI");
			list.appendChild(li);
			if(a.startsWith("InChI=")) {
				String name = lis.nameForInChI(a);
				Element ne = new Element("ne");
				ne.addAttribute(new Attribute("type", "CM"));
				ne.addAttribute(new Attribute("InChI", a));
				ne.appendChild(name);
				li.appendChild(ne);

				Element anchor = new Element("a");
				UserQuery nuq = new UserQuery(uq, UserQuery.ResultsType.SNIPPETS);
				nuq.addTerm(a, "inchi", "");
				anchor.addAttribute(new Attribute("href", nuq.getQueryURL(0, nuq.getSize())));
				anchor.appendChild("search");
				
				li.appendChild(" ");
				li.appendChild(anchor);
			} else if(a.matches("0\\.\\d+ like InChI=.+")) {
				String [] sp = a.split("\\s+");
				String name = lis.nameForInChI(sp[2]);
				li.appendChild(sp[0] + " like ");
				
				Element ne = new Element("ne");
				ne.addAttribute(new Attribute("type", "CM"));
				ne.addAttribute(new Attribute("InChI", a));
				ne.appendChild(name);
				li.appendChild(ne);
				
			} else if(a.startsWith("ONT: ")) {
				String postFix = a.substring(5);
				String name = OBOOntology.getInstance().getNameForID(postFix);
				Element ne = new Element("ne");
				ne.addAttribute(new Attribute("type", "ONT"));
				ne.addAttribute(new Attribute("ontIDs", postFix));
				ne.appendChild(name);
				li.appendChild(ne);
				
				Element anchor = new Element("a");
				UserQuery nuq = new UserQuery(uq, UserQuery.ResultsType.SNIPPETS);
				nuq.addTerm(postFix, "ontology", "");
				anchor.addAttribute(new Attribute("href", nuq.getQueryURL(0, nuq.getSize())));
				anchor.appendChild("search");
				
				li.appendChild(" ");
				li.appendChild(anchor);				
		    } else {
				li.appendChild(a);				

				Element anchor = new Element("a");
				UserQuery nuq = new UserQuery(uq, UserQuery.ResultsType.SNIPPETS);
				nuq.addTerm(a, "word", "strict");
				anchor.addAttribute(new Attribute("href", nuq.getQueryURL(0, nuq.getSize())));
				anchor.appendChild("search");
				
				li.appendChild(" ");
				li.appendChild(anchor);
			}
		}
		
		return sxd;
	}
	
	public static Map<String,Set<String>> getP450Related() {
		Map<String,Set<String>> related = new HashMap<String,Set<String>>();
		related.put("cyp1a2", new HashSet<String>());
		related.put("cyp2b6", new HashSet<String>());
		related.put("cyp2c19", new HashSet<String>());
		related.put("cyp2d6", new HashSet<String>());
		related.put("cyp2e1", new HashSet<String>());
		related.get("cyp1a2").add("amitriptyline");
		related.get("cyp1a2").add("caffeine");
		related.get("cyp1a2").add("clomipramine");
		related.get("cyp1a2").add("cimetidine");
		related.get("cyp1a2").add("omeprazole");
		related.get("cyp2b6").add("bupropion");
		related.get("cyp2b6").add("cyclophosphamide");
		related.get("cyp2b6").add("ifosphamide");
		related.get("cyp2b6").add("thiotepa");
		related.get("cyp2b6").add("ticlopidine");
		related.get("cyp2b6").add("phenobarbital");
		related.get("cyp2b6").add("phenytoin");
		related.get("cyp2b6").add("rifampin");
		related.get("cyp2c19").add("chloramphenicol");
		related.get("cyp2c19").add("citalopram");
		related.get("cyp2c19").add("clomipramine");
		related.get("cyp2c19").add("fluoxetine");
		related.get("cyp2c19").add("carbamazepine");
		related.get("cyp2d6").add("alprenolol");
		related.get("cyp2d6").add("amphetamine");
		related.get("cyp2d6").add("aripiprazole");
		related.get("cyp2d6").add("atomoxetine");
		related.get("cyp2d6").add("bufuralol");
		related.get("cyp2e1").add("acetaminophen");
		related.get("cyp2e1").add("aniline");
		related.get("cyp2e1").add("benzene");
		related.get("cyp2e1").add("chlorzoxazone");
		related.get("cyp2e1").add("ethanol");
		return related;
	}
	
	private static String nameForInChI(String inchi) throws Exception {
		//if(true) return inchi;
		Set<String> oids = ChemNameDictSingleton.getOntologyIdsFromInChI(inchi);
		if(oids != null && oids.size() == 1) {
			String name = OBOOntology.getInstance().getNameForID(oids.iterator().next());
			return name;
		} else {
			return inchi;
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Query q;//if(true) {
		//	System.out.println(ChemNameDictSingleton.getInChIForShortestSmiles("sertraline"));
		//	return;
		//}
		
		ChiSquaredDistribution csd = new ChiSquaredDistributionImpl(1);
		List<Query> lq = new ArrayList<Query>();
		TextMiner tm = new TextMiner(new LuceneIndexerSearcher(false));

		System.out.println(tm.corpusBags.txtBag.totalCount());
		System.out.println(tm.corpusBags.txtBag.getCount("dextromethorphan"));
		
		q = new TermQuery(new Term("txt", "CYP2D6"));
		Bags bags = tm.makeBagsFromQuery(q);
		System.out.println(bags.txtBag.totalCount());
		System.out.println(bags.txtBag.getCount("dextromethorphan"));
		
		if(true) return;
		
		//OntologyQuery oq = new OntologyQuery(OBOOntology.getInstance().getIdsForTermWithChildren("catalase"));
		//System.out.println(oq.getIDs());
		
		/*q = new TermQuery(new Term("txt", "morphine"));
		//q = oq.getLuceneQuery();
		//Bags bags = tm.makeBagsFromQuery(q);
		Map<String,Double> results = tm.queryVsCorpus(q);
		int ranking = 0;
		System.out.println(results.size());
		for(String result : StringTools.getSortedList(results)) {
			System.out.println(result);
		}*/
		
		/*Query q = new TermQuery(new Term("txt", "cyp2d6"));
		Bags bags = tm.makeBagsFromQuery(q);
		SimilarityMatrix sm = new SimilarityMatrix(bags.inchiBag.getSet());
		Bag<String> similarities = sm.similarityEvents(bags.inchiBag);
		for(String sim : similarities.getList()) {
			String [] sp = sim.split("\\s+");
			String simRep = sim;
			simRep = sp[0] + " " + sp[1] + " " + nameForInChI(sp[2]);
			System.out.println(simRep + "\t" + similarities.getCount(sim));
			System.out.println(bags.inchiBag.getCount(sp[2]));
			double similarityThreshold = Double.parseDouble(sp[0]);
			for(String inchi : bags.inchiBag.getSet()) {
				if(sm.getSimilarity(inchi, sp[2]) > similarityThreshold) {
					System.out.println("\t" + nameForInChI(inchi) + "\t" + sm.getSimilarity(inchi, sp[2]) + "\t" + bags.inchiBag.getCount(inchi));
				}
			}
		}*/
		
		
		
		/*Map<String,Double> scores = tm.queryVsCorpus(new TermQuery(new Term("txt", "cyp2d6")));
		for(String s : StringTools.getSortedList(scores)) {
			System.out.println(s + "\t" + scores.get(s) + "\t" + (1.0 - csd.cumulativeProbability(scores.get(s))));
		}

		if(true) return;*/
		
		List<String> cytochromes = new ArrayList<String>();
		cytochromes.add("cyp1a1");
		cytochromes.add("cyp1a2");
		cytochromes.add("cyp1b1");
		cytochromes.add("cyp2a6");
		cytochromes.add("cyp2b6");
		cytochromes.add("cyp2c8");
		cytochromes.add("cyp2c19");
		cytochromes.add("cyp2d6");
		cytochromes.add("cyp2e1");
		cytochromes.add("grapefruit");
		cytochromes.add("orange");
		cytochromes.add("juice");
		cytochromes.add("demethylation");
		cytochromes.add("N-demethylation");
		cytochromes.add("O-demethylation");
		cytochromes.add("demethylase");
		cytochromes.add("N-demethylase");
		cytochromes.add("O-demethylase");
		cytochromes.add("deethylation");
		cytochromes.add("N-deethylation");
		cytochromes.add("O-deethylation");
		cytochromes.add("deethylase");
		//cytochromes.add("N-deethylase");
		cytochromes.add("O-deethylase");
		cytochromes.add("hydroxylation");
		cytochromes.add("hydroxylated");
		cytochromes.add("oxidation");
		cytochromes.add("metabolism");
		cytochromes.add("hydrolysis");
		cytochromes.add("ethanol");
		cytochromes.add("water");
		cytochromes.add("steroid");
		cytochromes.add("hormone");
		cytochromes.add("estradiol");
		cytochromes.add("testosterone");
		cytochromes.add("morphine");
		cytochromes.add("codeine");
		cytochromes.add("dextromethorphan");
		cytochromes.add("mice");
		cytochromes.add("rats");
		cytochromes.add("liver");
		cytochromes.add("hepatic");
		cytochromes.add("microsome");
		cytochromes.add("microsomes");
		cytochromes.add("microsomal");
		cytochromes.add("enzyme");
		cytochromes.add("protein");
		cytochromes.add("substrate");
		cytochromes.add("inhibitor");
		cytochromes.add("inhibition");
		cytochromes.add("inhibits");
		cytochromes.add("inhibit");
		cytochromes.add("inhibiting");
		
		Map<String,Map<String,Double>> cytoscores = new HashMap<String,Map<String,Double>>();
		for(String cytochrome : cytochromes) {
			cytoscores.put(cytochrome, tm.queryVsCorpus(new TermQuery(new Term("txt", cytochrome))));
		}
		System.out.println(cytoscores.keySet());
		Map<String,Double> cytoSims = new HashMap<String,Double>();
		for(String cytochromeA : cytochromes) {
			for(String cytochromeB : cytochromes) {
				if(cytochromeA.compareTo(cytochromeB) < 0) {
					cytoSims.put(cytochromeA + "->" + cytochromeB, tm.similarity(cytoscores.get(cytochromeA),cytoscores.get(cytochromeB)));
				}
			}
		}
		for(String c : StringTools.getSortedList(cytoSims)) {
			System.out.println(c + "\t" + cytoSims.get(c));
		}
		System.out.println(cytoSims.size());
		System.out.println((cytochromes.size() - 1) * (cytochromes.size() - 2));
		
		//for(String s : StringTools.getSortedList(scores)) {
		//	System.out.println(s + "\t" + scores.get(s) + "\t" + (1.0 - csd.cumulativeProbability(scores.get(s))));
		//}
		//tm.setHeldOutInchi(ChemNameDictSingleton.getInChIForShortestSmiles("sertraline"));
		
		
		/*lq.add(new TermQuery(new Term("txt", "cyp1a1")));
		lq.add(new TermQuery(new Term("txt", "cyp1a2")));
		lq.add(new TermQuery(new Term("txt", "cyp1b1")));
		lq.add(new TermQuery(new Term("txt", "cyp2a6")));
		lq.add(new TermQuery(new Term("txt", "cyp2b6")));
		lq.add(new TermQuery(new Term("txt", "cyp2c8")));
		lq.add(new TermQuery(new Term("txt", "cyp2c19")));*/
		//lq.add(new TermQuery(new Term("txt", "cyp2d6")));
		//lq.add(new TermQuery(new Term("txt", "cyp2e1")));
		//lq.add(new TermQuery(new Term("txt", "cyp3a4")));
		
		//lq.add(new TermQuery(new Term("txt", "")));
		//lq.add(new TermQuery(new Term("txt", "")));
		
		//List<Map<String,Double>> results = tm.disjunctiveQuery(lq);
		//tm.disjunctiveTable(results);

		
		/*Map<String,Set<String>> related = getP450Related();
		for(String p450 : related.keySet()) {
			for(String cm : related.get(p450)) {
				String inchi = ChemNameDictSingleton.getInChIForShortestSmiles(cm);
				if(inchi == null) {
					System.out.println("Skipping: " + cm);
					continue;
				}
				//tm.setHeldOutInchi(inchi);
				System.out.println(p450 + "\t" + cm);
				Map<String,Double> sv = tm.inchiToVector(inchi);
				List<Map<String,Double>> results = tm.disjunctiveQuery(lq);
				//tm.disjunctiveTable(results);
				for(Map<String,Double> result : results) {
					System.out.println(tm.similarity(result, sv));
				}
				
			}			
		}*/
		
		
		//OntologyQuery oq = new OntologyQuery(OBOOntology.getInstance().getIdsForTermWithChildren("antibiotic"));
		
		/*Query q = new TermQuery(new Term("txt", "grapefruit"));
		//Map<String,Double> scores = tm.queryVsCorpus(oq.getLuceneQuery());
		
		
		
		Map<String,Double> scores = tm.queryVsCorpus(q);
		for(String s : StringTools.getSortedList(scores)) {
			System.out.println(s + "\t" + scores.get(s) + "\t" + (1.0 - csd.cumulativeProbability(scores.get(s))));
		}*/
		
		
		//tm.disjunctiveTable(results);*/
		
		
		
		//for(Map<String,Double> resultSet : results) {
		//	for(String s : StringTools.getSortedList(resultSet)) {
		//		double score = resultSet.get(s);
		//		if(score < 1) break;
		//		System.out.println(s);
		//	}
		//	System.out.println("=====================================");
		//}

	}

}
