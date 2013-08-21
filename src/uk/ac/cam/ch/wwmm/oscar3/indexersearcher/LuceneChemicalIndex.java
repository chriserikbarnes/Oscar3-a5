package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.openscience.cdk.Molecule;
import org.openscience.cdk.aromaticity.HueckelAromaticityDetector;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;

import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

/** Stores and indexes CDK Molecules and their fingerprints using Lucene. This should
 * provide a reasonably fast and efficient way to do substructure and similarity 
 * searches on large collections stored on disk. 
 * 
 * @author ptc24
 *
 */
public final class LuceneChemicalIndex {

	private File indexFile;
	private Analyzer myAnalyzer;
	private Fingerprinter fingerprinter;
	private IndexWriter indexWriter;
	private IndexSearcher indexSearcher;
	
	/** Opens a lucene chemical index.
	 * 
	 * @param indexFile Where the index is to be.
	 * @param makeNew If the index exists, whether to delete it and make a new one (true) or open the old one (false).
	 */
	public LuceneChemicalIndex(File indexFile, boolean makeNew) throws Exception {
		this.indexFile = indexFile;
		myAnalyzer = new StandardAnalyzer();
		indexWriter = new IndexWriter(indexFile, myAnalyzer, makeNew, MaxFieldLength.UNLIMITED);
		indexWriter.setSimilarity(new ChemicalSimilarity());
		fingerprinter = new Fingerprinter();
		indexSearcher = null;
	}
	
	LuceneChemicalIndex(File indexFile, InChIToName itn) throws Exception {
		this(indexFile,true);
		for(String inchi : itn.getInChIs()) {
			addMolecule(inchi, null, itn.namesForInChI(inchi));
		}
		closeWriter();
	}
	
	/**Add a molecule to the index.
	 * 
	 * @param inchi The InChI for the molecule.
	 * @param smiles The SMILES for the molecule.
	 * @param names Names for the molecule, with occurrence counts.
	 * @throws Exception
	 */
	public void addMolecule(String inchi, String smiles, Bag<String> names) throws Exception {
		/*if(inchi != null) {
			setToSearch();
			Query q = new TermQuery(new Term("InChI", inchi));
			if(indexSearcher.search(q).length() > 0) return;
		}*/
		IMolecule mol;
		try {
			mol = new Molecule();
			mol = ConverterToInChI.getMolFromInChI(inchi);
			StructureConverter.configureMolecule(mol);
			try {
				HueckelAromaticityDetector.detectAromaticity(mol);				
			} catch (Exception e) {
				// Fail silently for now...
			}
			addMoleculeNoCheck(mol, inchi, smiles, names);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}
	
	private void addMoleculeNoCheck(IMolecule mol, String inchi, String smiles, Bag<String> names) throws Exception {
		/* First check for uniqueness of InChIs */
		setToWrite();
		Document doc = new Document();
		doc.add(new Field("mol", serialiseMol(mol), Field.Store.COMPRESS, Field.Index.NO));
		BitSet fp = fingerprinter.getFingerprint(mol);
		for(int i=fp.nextSetBit(0); i>=0; i=fp.nextSetBit(i+1)) {
			Field f = new Field("FP", Integer.toString(i), Field.Store.NO, Field.Index.NOT_ANALYZED_NO_NORMS, Field.TermVector.NO);
			//f.setOmitNorms(true);
			doc.add(f);
		}
		if(inchi != null) doc.add(new Field("InChI", inchi, Field.Store.COMPRESS, Field.Index.NOT_ANALYZED_NO_NORMS));
		if(smiles != null) doc.add(new Field("SMILES", smiles, Field.Store.COMPRESS, Field.Index.NO));
		if(names != null) {
			for(String name : names.getSet()) {
				for(int i=0;i<names.getCount(name);i++)	doc.add(new Field("name", name, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			}
		}
		indexWriter.addDocument(doc);
		//closeWriter();
	}
	
	/**Closes the index writer. This writer will be re-opened automatically
	 * when needed.
	 * 
	 * @throws Exception
	 */
	public void closeWriter() throws Exception {
		if(indexWriter != null) {
			indexWriter.close();
			indexWriter = null;
		}
	}
	
	/**Closes the index reader. This reader will be re-opened automatically
	 * when needed. Note that once a query has been run, this routine should
	 * not be called (directly, or indirectly (by writing to the index) until
	 * any VectorCollectors produced by the search have been interpreted.
	 * 
	 * @throws Exception
	 */
	public void closeSearcher() throws Exception {
		if(indexSearcher != null) {
			indexSearcher.close();
			indexSearcher = null;
		}
	}
	
	private void setToSearch() throws Exception {
		closeWriter();
		if(indexSearcher == null) {
			indexSearcher = new IndexSearcher(indexFile.getAbsolutePath());		
			indexSearcher.setSimilarity(new ChemicalSimilarity());
		}
	}
	
	private void setToWrite() throws Exception {
		closeSearcher();
		if(indexWriter == null) {
			indexWriter = new IndexWriter(indexFile, myAnalyzer, false, MaxFieldLength.UNLIMITED);
			indexWriter.setSimilarity(new ChemicalSimilarity());
		}
	}

	/**Returns the molecules that match the given InChI string, simply by
	 * performing a string match.
	 * 
	 * @param inchi The InChI string.
	 * @return A VectorCollector containing the hits.
	 * @throws Exception
	 */
	public VectorCollector hitsByInChI(String inchi) throws Exception {
		setToSearch();
		VectorCollector vc = new VectorCollector();
		Query q = new TermQuery(new Term("InChI", inchi));
		indexSearcher.search(q, vc);
		return vc;
	}
	
	/**Returns the molecules that match the given CDK molecule exactly.
	 * 
	 * @param queryMol The query molecule.
	 * @return A VectorCollector containing the hits.
	 * @throws Exception
	 */
	public VectorCollector hitsExact(IMolecule queryMol) throws Exception {
		setToSearch();
		VectorCollector vc = new VectorCollector();
		BitSet fp = fingerprinter.getFingerprint(queryMol);
		BooleanQuery bq = new BooleanQuery();
		for(int i=fp.nextSetBit(0); i>=0; i=fp.nextSetBit(i+1)) { 
			bq.add(new BooleanClause(new TermQuery(new Term("FP", Integer.toString(i))), BooleanClause.Occur.MUST));
		}
		indexSearcher.search(bq, vc);
		List<Integer> bestResults = vc.hitsByScore();
		VectorCollector newVc = new VectorCollector();
		IndexReader ir = indexSearcher.getIndexReader();
		float maxScore = -1;
		for(Integer i : bestResults) {
			IMolecule hitMol = deSerialiseMol(ir.document(i).get("mol"));
			float score = vc.getResultsVector().get(i);
			if(UniversalIsomorphismTester.isIsomorph(hitMol, queryMol)) {
				newVc.collect(i, score);
			}
			if(score < maxScore) break;
			maxScore = score;

		}
		return newVc;
	}
	
	/**Returns the molecules that contain the given CDK molecule as a
	 * substructure.
	 * 
	 * @param queryMol The query molecule.
	 * @return A VectorCollector containing the hits.
	 * @throws Exception
	 */
	public VectorCollector hitsBySubstructure(IMolecule queryMol) throws Exception {
		return hitsBySubstructure(queryMol, -1);
	}
	
	/**Returns the molecules that contain the given CDK molecule as a
	 * substructure, selecting the best matches if more than a given
	 * maximum are found.
	 * 
	 * @param queryMol The query molecule.
	 * @param maxHits The maximum number of hits to return.
	 * @return A VectorCollector containing the hits.
	 * @throws Exception
	 */	
	public VectorCollector hitsBySubstructure(IMolecule queryMol, int maxHits) throws Exception {
		setToSearch();
		VectorCollector vc = new VectorCollector();
		BitSet fp = fingerprinter.getFingerprint(queryMol);
		BooleanQuery bq = new BooleanQuery();
		for(int i=fp.nextSetBit(0); i>=0; i=fp.nextSetBit(i+1)) { 
			bq.add(new BooleanClause(new TermQuery(new Term("FP", Integer.toString(i))), BooleanClause.Occur.MUST));
		}
		indexSearcher.search(bq, vc);
		List<Integer> bestResults = vc.hitsByScore();
		VectorCollector newVc = new VectorCollector();
		IndexReader ir = indexSearcher.getIndexReader();
		for(Integer i : bestResults) {
			if(maxHits != -1 && newVc.getResultsVector().size() >= maxHits) break;
			IMolecule hitMol = deSerialiseMol(ir.document(i).get("mol"));
			float score = vc.getResultsVector().get(i);
			if(UniversalIsomorphismTester.isSubgraph(hitMol, queryMol)) {
				newVc.collect(i, score);
			}
		}
		return newVc;
	}
	
	/**Returns the molecules that are most similar to the given CDK molecule.
	 * 
	 * @param queryMol The query molecule.
	 * @param maxHits The maximum number of hits to return.
	 * @return A VectorCollector containing the hits.
	 * @throws Exception
	 */
	public VectorCollector hitsBySimilarity(IMolecule queryMol, int maxHits) throws Exception {
		setToSearch();
		VectorCollector vc = new VectorCollector();
		BitSet fp = fingerprinter.getFingerprint(queryMol);
		BooleanQuery bq = new BooleanQuery();
		for(int i=fp.nextSetBit(0); i>=0; i=fp.nextSetBit(i+1)) { 
			bq.add(new BooleanClause(new TermQuery(new Term("FP", Integer.toString(i))), BooleanClause.Occur.SHOULD));
		}
		indexSearcher.search(bq, vc);
		List<Integer> bestResults = vc.hitsByScore();
		VectorCollector newVc = new VectorCollector();
		for(Integer i : bestResults) {
			if(maxHits != -1 && newVc.getResultsVector().size() >= maxHits) break;
			float score = vc.getResultsVector().get(i);
			newVc.collect(i, score);
		}
		return newVc;
	}

	/*public List<Hit> hitsBySimilarityToSet(Collection<IMolecule> queryMols, int maxHits) throws Exception {
		setToSearch();
		BooleanQuery bigbq = new BooleanQuery();
		Map<Integer,Integer> printMap = new HashMap<Integer,Integer>();
		for(IMolecule queryMol : queryMols) {
			BitSet fp = fingerprinter.getFingerprint(queryMol);
			//BooleanQuery bq = new BooleanQuery();
			for(int i=fp.nextSetBit(0); i>=0; i=fp.nextSetBit(i+1)) { 
				if(!printMap.containsKey(i)) printMap.put(i, 0);
				printMap.put(i, printMap.get(i)+1);
				//bigbq.add(new BooleanClause(new TermQuery(new Term("FP", Integer.toString(i))), BooleanClause.Occur.SHOULD));
				//System.out.println("FP" + Integer.toString(i));
			}
			//bigbq.add(new BooleanClause(bq, BooleanClause.Occur.SHOULD));
		}
		for(Integer i : printMap.keySet()) {
			Query q = new TermQuery(new Term("FP", Integer.toString(i)));
			q.setBoost(printMap.get(i));
			bigbq.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
		}
		for(int i=0;i<bigbq.getClauses().length;i++) {
			System.out.println(bigbq.getClauses()[i].getQuery().getBoost());
		}
		System.out.println(bigbq.toString());
		List<Hit> hitList = new ArrayList<Hit>();
		Hits h = indexSearcher.search(bigbq);
		HitIterator hi = (HitIterator) h.iterator();
		while(hi.hasNext() && (maxHits == -1 || hitList.size() < maxHits)) {
			Hit hit = (Hit)hi.next();
			hitList.add(hit);
		}
		return hitList;
	}*/
	
	/**Converts a VectorCollector produced by this class into a map from InChI
	 * values to match scores.
	 * 
	 * @param vc The VectorCollector to interpret.
	 * @return The InChIs, with their scores.
	 */
	public Map<String, Float> getInChIMap(VectorCollector vc) {
		Map<String, Float> inchiMap = new HashMap<String, Float>();
		IndexReader ir = indexSearcher.getIndexReader();
		for(Integer i : vc.getResultsVector().keySet()) {
			try {
				inchiMap.put(ir.document(i).get("InChI"), (float)Math.sqrt(vc.getResultsVector().get(i)));
			} catch (Exception e) {
				// This ought to work...
				e.printStackTrace();
			}
		}
		try	{
			closeSearcher();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return inchiMap;
	}

	/*public Map<String, Float> getNameMap(VectorCollector vc) {
		Map<String, Float> nameMap = new HashMap<String, Float>();
		IndexReader ir = indexSearcher.getIndexReader();
		for(Integer i : vc.getResultsVector().keySet()) {
			try {
				Field [] names = ir.document(i).getFields("name");
				Bag<String> nameBag = new Bag<String>();
				for(int j=0;j<names.length;j++) {
					nameBag.add(names[j].stringValue());
				}
				float total = nameBag.totalCount();
				for(String name : nameBag.getSet()) {
					nameMap.put(name, nameBag.getCount(name) * (float)Math.sqrt(vc.getResultsVector().get(i)) / total);					
				}
				//nameMap.put(h.get("name"), (float)Math.sqrt(h.getScore()));
			} catch (Exception e) {
				// This ought to work...
				e.printStackTrace();
			}
		}
		try	{
			closeSearcher();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nameMap;
	}*/
	
	/**Extracts the most common name from a VectorCollector.
	 * 
	 * @param vc The vector collector to interpret.
	 * @return The name, or null.
	 */
	public String getName(VectorCollector vc) {
		if(vc.getResultsVector().size() != 1) return null;
		String result = null;
		IndexReader ir = indexSearcher.getIndexReader();
		for(Integer i : vc.getResultsVector().keySet()) {
			try {
				Field [] names = ir.document(i).getFields("name");
				Bag<String> nameBag = new Bag<String>();
				for(int j=0;j<names.length;j++) {
					nameBag.add(names[j].stringValue());
				}
				result = nameBag.mostCommon();
			 } catch (Exception e) {
				// This ought to work...
				e.printStackTrace();
			}
		}		
		try	{
			closeSearcher();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static String serialiseMol(IMolecule mol) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(mol);
		
		String s = bytesToBase16(baos.toByteArray());
		return s;
	}
	
	private static String bytesToBase16(byte [] bytes) {
		StringBuffer sb = new StringBuffer();
		for(int i=0;i<bytes.length;i++) {
			int b = bytes[i];
			if(b < 0) b += 256;
			String s = Integer.toHexString(b);
			if(s.length() == 1) sb.append("0");
			//System.out.println(s);
			sb.append(s);
		}
		return sb.toString();
	}
	
	private static byte[] base16ToBytes(String s) {
		byte[] out = new byte[s.length()/2];
		for(int i=0;i<s.length()/2;i++) {
			String n = s.substring(i*2, (i*2)+2);
			int ii = Integer.parseInt(n, 16);
			byte b = new Integer(ii).byteValue();
			out[i] = b;
		}
		return out;
	}

	private static IMolecule deSerialiseMol(String molStr) throws Exception {
		byte [] ba = base16ToBytes(molStr);
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(ba));
		return (IMolecule) ois.readObject();
	}	

}
