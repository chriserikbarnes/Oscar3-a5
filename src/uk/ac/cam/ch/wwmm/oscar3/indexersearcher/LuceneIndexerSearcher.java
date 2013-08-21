package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.index.TermPositionVector;
import org.apache.lucene.index.TermVectorOffsetInfo;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.Hit;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocCollector;
import org.apache.lucene.search.TopDocs;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.HueckelAromaticityDetector;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.smiles.SmilesParser;

import uk.ac.cam.ch.wwmm.opsin.NameToStructure;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;
import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.indexersearcher.UserQuery.ResultsType;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocument;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.ProcessingDocumentFactory;
import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;
import uk.ac.cam.ch.wwmm.oscar3server.scrapbook.ScrapBook;
import uk.ac.cam.ch.wwmm.ptclib.cdk.ConverterToInChI;
import uk.ac.cam.ch.wwmm.ptclib.cdk.StructureConverter;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SciXMLDocument;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;
import uk.ac.cam.ch.wwmm.ptclib.xml.XMLSpanTagger;
import uk.ac.cam.ch.wwmm.ptclib.xml.XOMTools;

/** Indexes and searches papers in SciXML+Oscar using Apache Lucene.
 * 
 * @author ptc24
 *
 */

public final class LuceneIndexerSearcher {

	/**The name of the files to index, usually markedup.xml */
	private File indexFile;
	private File chemIndexFile;
	private LuceneChemicalIndex lci;
	private SmilesParser smilesParser = new SmilesParser(DefaultChemObjectBuilder.getInstance());
	
	/**Initialises a new LuceneIndexerSearcher.
	 * 
	 * @param overWrite Whether to start from scratch (true), or see if there
	 * is already data in the workspace and use that (false)
	 * @throws Exception
	 */
	public LuceneIndexerSearcher(boolean overWrite) throws Exception {
		this(overWrite, 
				new File(new File(Oscar3Props.getInstance().workspace), "index"),
				new File(new File(Oscar3Props.getInstance().workspace), "chemindex"));
	}

	/**Sets up a new LuceneIndexerSearcher.
	 * 
	 * @param overWrite overWrite Whether to start from scratch (true), or see if there
	 * is already data in the given locations and use that (false)
	 * @param indexFileParam The directory that will contain the index.
	 * @param chemIndexFileParam The directory that will contain the chemicalIndex.
	 * @throws Exception
	 */
	public LuceneIndexerSearcher(boolean overWrite, File indexFileParam, File chemIndexFileParam) throws Exception {
		BooleanQuery.setMaxClauseCount(2048);
		indexFile = indexFileParam;
		chemIndexFile = chemIndexFileParam;
		
		if(!indexFile.exists()) overWrite = true;
		if(!chemIndexFile.exists()) overWrite = true;
		//System.out.println(overWrite);
		IndexWriter iw = new IndexWriter(indexFile, new StandardAnalyzer(), overWrite, MaxFieldLength.UNLIMITED);	
		iw.close();
		try {
			lci = new LuceneChemicalIndex(chemIndexFile, overWrite);
			lci.closeWriter();
			lci.closeSearcher();
		} catch (Exception e) {
			//e.printStackTrace();
			iw = new IndexWriter(indexFile, new StandardAnalyzer(), true, MaxFieldLength.UNLIMITED);
			iw.close();
			lci = new LuceneChemicalIndex(chemIndexFile, true);
			lci.closeWriter();
			lci.closeSearcher();
		}
		//textMiner = new TextMiner(this);
	}
		
	/** Private constructor to enforce the proper one */
	@SuppressWarnings("unused")
	private LuceneIndexerSearcher() {
		
	}
	
	/**Gets the chemical index for this object.
	 * 
	 * @return The chemical index.
	 */
	public LuceneChemicalIndex getLci() {
		return lci;
	}
	
	/**Indexes the contents of the ScrapBook.
	 * 
	 * @throws Exception
	 */
	public void addScrapBook() throws Exception {
		addDirectory(null);
	}
	
	/**Indexes all files found in a directory, or children of that directory.
	 * 
	 * @param fileRoot The directory to index.
	 * @throws Exception
	 */
	public void addDirectory(File fileRoot) throws Exception {
		addDirectory(fileRoot, new PrintWriter(System.out));
	}

	/**Indexes all files found in a directory, or children of that directory.
	 * 
	 * @param fileRoot The directory to index.
	 * @param out A PrintWriter to write messages to.
	 * @throws Exception
	 */
	public void addDirectory(File fileRoot, PrintWriter out) throws Exception {
		
		//lci.closeWriter();
		
		String filename;
		if(fileRoot == null) {
			filename = "scrapbook.xml";
			fileRoot = ScrapBook.getScrapBookFile();
		} else {
			//filename = "source.xml";
			filename = "markedup.xml";
		}
		double start; double end; double seconds;
		start = System.currentTimeMillis();

		out.print("Indexing chemical names and structures... ");
		out.flush();
		
		List<File> foundFiles = FileTools.getFilesFromDirectoryByName(fileRoot, filename);
		if(foundFiles.size() == 0) {
			filename = "source.xml";
			foundFiles = FileTools.getFilesFromDirectoryByName(fileRoot, filename);
		}
		InChIToName itn = new InChIToName();
		itn.analyse(foundFiles);
		lci = new LuceneChemicalIndex(new File(new File(Oscar3Props.getInstance().workspace), "chemindex"), itn);

		out.println("Indexed in " + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
		start = System.currentTimeMillis();		
		out.flush();
		
		//IndexWriter iw = new IndexWriter(dir, new StandardAnalyzer(), true);
		IndexWriter iw = new IndexWriter(indexFile, new StandardAnalyzer(), false, MaxFieldLength.UNLIMITED);
		for(File f : foundFiles) {
			if(f.length() < 1024) {
				Document xmlDoc = new Builder().build(f);
				Nodes abnodes = xmlDoc.query("/PAPER/ABSTRACT|/PAPER/BODY/DIV");
				if(abnodes.size() < 1) continue; // Avoid front matter, back matter etc.
			}
			//System.out.println(f);
			Document xmlDoc = new Builder().build(f);
			XMLSpanTagger.tagUpDocument(xmlDoc.getRootElement(), "a");
			Nodes chemicals = xmlDoc.query("//ne");
			/*for(int i=0;i<chemicals.size();i++) {
				Element e = (Element)chemicals.get(i);
				addToChemicalIndex(e);
			}
			lci.closeWriter();*/
			org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
			NETokenStream inchiNets = new NETokenStream();
			NETokenStream ontNets = new NETokenStream();
			for(int i=0;i<chemicals.size();i++) {
				Element e = (Element)chemicals.get(i);
				int startOffset = Integer.parseInt(e.getAttributeValue("xtspanstart"));
				int endOffset = Integer.parseInt(e.getAttributeValue("xtspanend"));
				if(e.getAttribute("InChI") != null) {
					String inchi = e.getAttributeValue("InChI");
					inchiNets.addToken(inchi, startOffset, endOffset);
				}
				if(e.getAttribute("ontIDs") != null) {
					String [] ontIDs = e.getAttributeValue("ontIDs").split(" ");
					for(int j=0;j<ontIDs.length;j++) {
						ontNets.addToken(ontIDs[j], startOffset, endOffset);
					}
				}
				//luceneDoc.add(new Field("txt", e.getValue(), Field.Store.YES, Field.Index.UN_TOKENIZED));
			}
			luceneDoc.add(new Field("InChI", inchiNets, Field.TermVector.WITH_OFFSETS));
			luceneDoc.add(new Field("Ontology", ontNets, Field.TermVector.WITH_OFFSETS));
			if(f.getAbsolutePath() == null) throw new Exception("Absoulte path is null!");
			luceneDoc.add(new Field("filename", f.getAbsolutePath(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
			//out.println(f.getAbsolutePath());
			//out.flush();
			Nodes cmlNodes = xmlDoc.query("//cmlPile");
			for(int i=0;i<cmlNodes.size();i++) cmlNodes.get(i).detach();
			
			//FIXME should use scixml whassname
			/*Nodes wordSources = xmlDoc.query(XMLStrings.CHEMICAL_PLACES_XPATH);
			for(int i=0;i<wordSources.size();i++) {
				Element e = (Element)wordSources.get(i);
				luceneDoc.add(new Field("txt", e.getValue(), Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.YES));
			}*/
			//NameRecogniser nr = new NameRecogniser();
			//nr.halfProcess(xmlDoc);
			//nr.makeTokenisers(false);
			
			ProcessingDocument procDoc = ProcessingDocumentFactory.getInstance().makeTokenisedDocument(xmlDoc, false, false, false);
			
			//for(Tokeniser t : nr.getTokenisers()) {
				TokenStream ts = new Oscar3TokenStream(procDoc.getTokenSequences());
				ts = new Oscar3Filters(ts);
				luceneDoc.add(new Field("txt", ts, Field.TermVector.WITH_POSITIONS_OFFSETS));
				
			//}
			
			//Nodes journalNodes = xmlDoc.query(XMLStrings.JOURNAL_NAME_XPATH);
			//for(int i=0;i<journalNodes.size();i++) luceneDoc.add(new Field("journal", journalNodes.get(i).getValue(), Field.Store.YES, Field.Index.UN_TOKENIZED));
			//IndexWriter iw = new IndexWriter(indexFile, new StandardAnalyzer(), false);
			iw.addDocument(luceneDoc);
			//iw.close();
		}			
		iw.close();
		
		lci.closeWriter();
		end = System.currentTimeMillis();
		seconds = (end - start) / 1000.0;
		int papers = foundFiles.size();
		double perpaper = seconds / papers;
		out.printf("%d files indexed in %f seconds\nAverage time: %f seconds\n\n", papers, seconds, perpaper);
		out.flush();
	}
	
	/**Interpret a UserQuery, and generate a SciXML document with the results.
	 * 
	 * @param uq The UserQuery to use.
	 * @return The results page.
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public SciXMLDocument getResultsByUserQuery(UserQuery uq) throws Exception {		
		BooleanQuery bq = new BooleanQuery();
		Query nq;
		for(UserQueryElement lqe : uq.queryElems) {
			Query q;
			if(lqe.queryType.equals("word")) {
				q = new BooleanQuery();
				QueryParser qp = new Oscar3QueryParser("txt", new Oscar3Analyzer(), this, "strict".equals(lqe.parameter));				
				Query qq = qp.parse(lqe.queryStr);
				((BooleanQuery)q).add(new BooleanClause(qq, BooleanClause.Occur.SHOULD));
			} else if(lqe.queryType.equals("strictword")) {
				q = new BooleanQuery();
				QueryParser qp = new Oscar3QueryParser("txt", new Oscar3Analyzer(), this, true);				
				Query qq = qp.parse(lqe.queryStr);
				((BooleanQuery)q).add(new BooleanClause(qq, BooleanClause.Occur.SHOULD));
			} else if (lqe.queryType.equals("ontology")) {
				Set<String> myOntIDs = new HashSet<String>();
				myOntIDs.addAll(OBOOntology.getInstance().getIdsForIdWithDescendants(lqe.queryStr));
				OntologyQuery oq = new OntologyQuery(myOntIDs);
				q = oq.getLuceneQuery();
			} else if (lqe.queryType.equals("inchi")) {
				q = new TermQuery(new Term("InChI", lqe.queryStr));
			} else {
				System.out.printf("%s %s %s", lqe.queryStr, lqe.queryType, lqe.parameter);
				ChemQuery cq = makeChemQueryFromSMILES(lqe.queryStr, lqe.queryType, lqe.parameter);
				q = cq.getLuceneQuery();
			}
			bq.add(new BooleanClause(q, BooleanClause.Occur.MUST));
		}
		
		IndexSearcher searcher = new IndexSearcher(indexFile.getAbsolutePath());

		nq = bq.rewrite(searcher.getIndexReader());
		Set s = new HashSet();
		nq.extractTerms(s);
		//explainQuery(nq);
		//System.out.println(nq.extractTerms(s));
		Collection<String> inchis = new HashSet<String>();
		Collection<String> words = new HashSet<String>();
		Collection<String> ontIDs = new HashSet<String>();
		for(Object o : s) {
			Term t = (Term)o;
			//System.out.println(t);
			if(t.field().equals("txt")) {
				words.add(t.text()); 
			} else if(t.field().equals("InChI")) {
				inchis.add(t.text());
			} else if(t.field().equals("Ontology")) {
				ontIDs.add(t.text());
			}
		}
		
		/*if(uq.moreLikeThis) {
			MoreLikeThis mlt = new MoreLikeThis(searcher.getIndexReader());
			mlt.setFieldNames("txt InChI".split(" "));
			mlt.setMinDocFreq(1);
			mlt.setMinTermFreq(1);
			mlt.setMaxQueryTerms(1000);

			Query q = mlt.like(uq.docId);
			Query oldbq = bq;
			bq = new BooleanQuery();
			bq.add(new BooleanClause(oldbq, BooleanClause.Occur.SHOULD));
			bq.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
		}*/	
		SciXMLDocument resultsDoc;
		if(uq.rt == ResultsType.SNIPPETS) {
			int hitsNeeded = uq.size + uq.skip;
			TopDocCollector tdc = new TopDocCollector(hitsNeeded);
			searcher.search(bq, tdc);
			resultsDoc = getResultsDocument(tdc, inchis, words, ontIDs, nq, uq, searcher.getIndexReader());			
		} else {
			SimpleHitCollector shc = new SimpleHitCollector();
			searcher.search(bq, shc);
			Set<File> files = getFilesFromSimpleHitCollector(shc, searcher.getIndexReader());
			switch (uq.rt) {
			case COMPOUNDSLIST:			
				resultsDoc = CompoundsList.makeCompundsList(files);
				break;
			case HITSLIST:
				resultsDoc = CompoundsList.makeCompoundsList(files, inchis, words, ontIDs);
				break;
			//case ASSOC:
			//	resultsDoc = textMiner.associations(hits, uq);
			//	break;
			default:
				resultsDoc = null;
			    break;
			}
			
		}
		
		searcher.close();
		return resultsDoc;
	}
	
	
	/*public ChemQuery makeChemQueryFromMolecules(Collection<IMolecule> mols, String type, String parameters) throws Exception {
	List<Hit> hits;
	if(type.equals("exact")) {
		throw new Exception();
	} else if(type.equals("substructure")) {
		throw new Exception();
	} else if(type.equals("match")) {
		throw new Exception("Match searching currently not implemented!");
	} else if(type.equals("similarity")) {
		int number = Integer.parseInt(parameters);
		hits = lci.hitsBySimilarityToSet(mols, number);
		//chemIDs = myLis.findChemicalIDsBySimilarity(mol, queryPrint, number);
	} else {
		throw new Exception();
	}
	Map<String, Float> inchiMap = lci.getInChIMap(hits);
	lci.closeSearcher();
	ChemQuery cq = new ChemQuery(inchiMap);
	return cq;
}*/
	
	ChemQuery makeChemQueryFromSMILES(String smiles, String type, String parameters) throws Exception {
		if(!smiles.contains(" ")) {
			IMolecule mol = smilesParser.parseSmiles(smiles);
			try {
				HueckelAromaticityDetector.detectAromaticity(mol);				
			} catch (Exception e) {
				// Fail silently for now...
			}

			return makeChemQueryFromMolecule(mol, type, parameters);
		} else {
			/*List<IMolecule> mols = new ArrayList<IMolecule>();
			String [] smileses = smiles.split(" ");
			for(int i=0;i<smileses.length;i++) {
				mols.add(smilesParser.parseSmiles(smileses[i]));
			}
			return makeChemQueryFromMolecules(mols, type, parameters);*/
			throw new Error("Space-separated SMILES lists not currently supported");
		}
	}
		
	ChemQuery makeChemQueryFromMolecule(IMolecule mol, String type, String parameters) throws Exception {
		VectorCollector vc;
		//List<Hit> hits;
		if(type.equals("exact")) {
			vc = lci.hitsExact(mol);
			//chemIDs = myLis.findChemicalIDsByExactMatch(mol, queryPrint);
		} else if(type.equals("substructure")) {
			vc = lci.hitsBySubstructure(mol);
			//chemIDs = myLis.findChemicalIDsBySubstructure(mol, queryPrint);
		} else if(type.equals("match")) {
			throw new Exception("Match searching currently not implemented!");
		} else if(type.equals("similarity")) {
			int number = Integer.parseInt(parameters);
			vc = lci.hitsBySimilarity(mol, number);
			//chemIDs = myLis.findChemicalIDsBySimilarity(mol, queryPrint, number);
		} else {
			throw new Exception();
		}
		Map<String, Float> inchiMap = lci.getInChIMap(vc);
		lci.closeSearcher();
		ChemQuery cq = new ChemQuery(inchiMap);
		return cq;
	}
	
	private SciXMLDocument getResultsDocument(TopDocCollector tdc, Collection<String> inchis, Collection<String> words, Collection<String> ontIDs, Query q, UserQuery lq, IndexReader ir) throws Exception {
		SciXMLDocument doc = new SciXMLDocument();
		doc.setTitle("Search Results");
		int hitsTotal = tdc.getTotalHits();
		if(lq.size + lq.skip > hitsTotal) {
			lq.size = hitsTotal - lq.skip;
		}
		Element p = doc.addPara();
		p.appendChild("Results " + Integer.toString(lq.skip + 1) + " to " + Integer.toString(lq.size + lq.skip) + " of " + Integer.toString(hitsTotal) + ": ");
		if(lq.skip > 0) p.appendChild(doc.makeLink(lq.getQueryURL(Math.max(lq.skip - lq.size, 0), lq.size), "prev"));
		p.appendChild(" ");
		if(lq.size + lq.skip < hitsTotal) p.appendChild(doc.makeLink(lq.getQueryURL(lq.skip + lq.size, lq.size), "next"));
		TopDocs td = tdc.topDocs();
		for(int i=lq.skip;i<lq.skip+lq.size;i++) {
			
			File f = new File(ir.document(td.scoreDocs[i].doc).get("filename"));
			Element body = doc.getBody();
			try {
				String href;
				if(f.getName().equals("scrapbook.xml")) {
					String sbname = f.getParentFile().getName();
					href = "ScrapBook?action=show&name=" + sbname;
				} else {
					String fname = f.getParent();
					fname = fname.split("corpora")[1].substring(1);
					href = "ViewPaper/" + fname;
				}
				QueryDigester qd = new QueryDigester(q);
				List<TermVectorOffsetInfo> offsets = qd.getOffsets(ir, td.scoreDocs[i].doc, "txt");
				//List<TermVectorOffsetInfo> offsets = getOffsets(ir, td.scoreDocs[i].doc, "txt", words);
				//List<TermVectorOffsetInfo> offsets = getOffsets(ir, td.scoreDocs[i].doc, "InChI", inchis);
				//List<TermVectorOffsetInfo> offsets = getOffsets(ir, td.scoreDocs[i].doc, "Ontology", ontIDs);
				body.appendChild(SnippetGetter.getSearchResult(inchis, offsets, ontIDs, new Builder().build(f), href, td.scoreDocs[i].doc, lq, f.getParentFile()));
				//System.out.println(f + "\t" + hits.score(i));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		doc.getDiv().appendChild(XOMTools.safeCopy(p));
		return doc;
	}
	
	private List<TermVectorOffsetInfo> getOffsets(IndexReader ir, int docNo, String fieldName, Collection<String> words) throws Exception {
		List<TermVectorOffsetInfo> results = new ArrayList<TermVectorOffsetInfo>();
		TermFreqVector tfv = ir.getTermFreqVector(docNo, fieldName);
		if(tfv instanceof TermPositionVector) {
			TermPositionVector tpv = (TermPositionVector)tfv;
			String [] terms = tpv.getTerms();
			for(int i=0;i<tpv.getTerms().length;i++) {
				if(words.contains(terms[i])) {
					//System.out.println(terms[i]);
					TermVectorOffsetInfo[] tps = tpv.getOffsets(i);
					int[] positions = tpv.getTermPositions(i);
					for(int j=0;j<tps.length;j++) {
						results.add(tps[j]);
						System.out.println(terms[i] + "\t" + positions[j] + "\t" + tps[j].getStartOffset() + "\t" + tps[j].getEndOffset());
					}					
				}
			}
		}
		return results;
	}
	/*public void addToChemicalIndex(Element chemElem) {
		try {
			if(chemElem.getAttribute("InChI") == null || chemElem.getAttribute("SMILES") == null) return;
			String inchi = chemElem.getAttributeValue("InChI");
			String smiles = chemElem.getAttributeValue("SMILES");
			lci.addMolecule(inchi, smiles, null);
		} catch (Exception e) {
			return;
		}
	}*/

	

	/*public Set<File> getFilesFromHits(Hits h) throws Exception {
		Set<File> files = new LinkedHashSet<File>();
		for(int i=0;i<h.length();i++) {
			//System.out.printf("%s %f\n", h.doc(i).get("filename"), h.score(i));
			files.add(new File(h.doc(i).get("filename")));
		}
		return files;
	}*/

	private Set<File> getFilesFromSimpleHitCollector(SimpleHitCollector shc, IndexReader ir) throws Exception {
		Set<File> files = new LinkedHashSet<File>();
		for(Integer i : shc.getHits()) {
			files.add(new File(ir.document(i).get("filename")));
		}
		return files;
	}
	
	//TODO put this somewhere more sensible
	
	String getInchiFromName(String name) throws Exception {
		String inchi = ChemNameDictSingleton.getInChIForShortestSmiles(name);
		if(inchi == null) {
			try {
				Element cml = NameToStructure.getInstance().parseToCML(name);
				inchi = ConverterToInChI.getInChI(StructureConverter.cmlToMolecule(cml));
			} catch (Exception e) {
				inchi = null;
			}
		}
		return inchi;
	}

	String getSmilesFromName(String name) throws Exception {
		String smiles = ChemNameDictSingleton.getShortestSmiles(name);
		if(smiles == null) {
			try {
				Element cml = NameToStructure.getInstance().parseToCML(name);
				smiles = StructureConverter.cmlToSMILES(cml);
			} catch (Exception e) {
				smiles = null;
			}
		}
		return smiles;
	}
	
	/**Execute a query, and get the results.
	 * 
	 * @param q The query.
	 * @return A map from document number to score.
	 * @throws Exception
	 */
	public Map<Integer,Float> getScoresVectorForQuery(Query q) throws Exception {
		IndexSearcher searcher = new IndexSearcher(indexFile.getAbsolutePath());
		VectorCollector vc = new VectorCollector();
		searcher.search(q, vc);
		searcher.close();
		return vc.getResultsVector();
	}
	

	/*@SuppressWarnings("unchecked")
	private void explainQuery(Query q) {
		Set s = new HashSet();
		q.extractTerms(s);
		//System.out.println(nq.extractTerms(s));
		for(Object o : s) {
			Term t = (Term)o;
			System.out.println(t);
		}
	}*/

	
	/**Gets the IndexSearcher for this object.
	 * 
	 * @return The IndexSearcher.
	 * @throws Exception
	 */
	public IndexSearcher getIndexSearcher() throws Exception {
		return new IndexSearcher(indexFile.getAbsolutePath());
	}
	
	/**Gets the IndexReader for this object.
	 * 
	 * @return The IndexReader.
	 * @throws Exception
	 */
	public IndexReader getIndexReader() throws Exception {
		return new IndexSearcher(indexFile.getAbsolutePath()).getIndexReader();
	}

	/**Makes a big results document detailing all of the compounds found in
	 * the corpus.
	 * 
	 * @return The results document.
	 * @throws Exception
	 */
	public SciXMLDocument getBigCompoundsList() throws Exception {		
		IndexSearcher searcher = new IndexSearcher(indexFile.getAbsolutePath());
		SimpleHitCollector shc = new SimpleHitCollector();
		searcher.search(new MatchAllDocsQuery(), shc);
		Set<File> files = getFilesFromSimpleHitCollector(shc, searcher.getIndexReader());
		SciXMLDocument resultsDoc = CompoundsList.makeCompundsList(files);
		searcher.close();
		return resultsDoc;
	}

	/**Given a prefix, suggest search terms that might match the prefix.
	 * 
	 * @param start The prefix.
	 * @return The possible expansions.
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public List<String> suggestTerms(String start) throws Exception {
		Query q = new FuzzyQuery(new Term("txt", start+"xxxx"), 0.0f, start.length());
		Query qq = getIndexSearcher().rewrite(q);
		
		if(qq instanceof BooleanQuery) {
			BooleanQuery bq = (BooleanQuery)qq;
			BooleanClause [] clauses = bq.getClauses();
			final HashMap<String,Float> hm = new HashMap<String,Float>();
			
			for(int i=0;i<clauses.length;i++) {
				Set s = new HashSet();		
				clauses[i].getQuery().extractTerms(s);
				Term t = (Term)s.toArray()[0];
				hm.put(t.text(), clauses[i].getQuery().getBoost());
			}
			
			List<String> al = new ArrayList<String>(hm.keySet());
			Collections.sort(al, Collections.reverseOrder(new Comparator<String>() {
				public int compare(String arg0, String arg1) {
					return hm.get(arg0).compareTo(hm.get(arg1));
				}
			}));
			return al;
		} else if(qq instanceof TermQuery) {
			TermQuery tq = (TermQuery)qq;
			List<String> al = new ArrayList<String>();
			al.add(tq.getTerm().text());
			return al;
		} else {
			return new ArrayList<String>();
		}
	}
	
	/**Produce JSON for the possible expansions of a prefix, for OpenSearch
	 * use.
	 * 
	 * @param start The prefix of the search term.
	 * @param ontological Whether to return ontology terms (true) or simple
	 * strings found in the corpus (false).
	 * @return JSON to return to the browser.
	 * @throws Exception
	 */
	public String suggestJSON(String start, boolean ontological) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("[\"" + start + "\", ");
		Collection<String> terms;
		if(ontological) {
			terms = OntologicalArguaments.getTerms(start);
		} else {
			terms = suggestTerms(start);
		}
		boolean first = true;
		sb.append("[");
		for(String term : terms) {
			if(!first) {
				sb.append(", ");
			} else {
				first = false;
			}
			sb.append("\"");
			sb.append(term);
			sb.append("\"");
		}
		sb.append("]]");
		//System.out.println(sb.toString());
		return sb.toString();
	}
	
	/**Given an InChI string, return the most common name for that InChI string.
	 * 
	 * @param inchi The InChI String
	 * @return The name
	 * @throws Exception
	 */
	public String nameForInChI(String inchi) throws Exception {
		return lci.getName(lci.hitsByInChI(inchi));
	}
	
	/**Return all files that contain a given word.
	 * 
	 * @param word The query word.
	 * @return The files.
	 * @throws Exception
	 */
	public Set<File> filesForWord(String word) throws Exception {
		Query q = new TermQuery(new Term("txt", StringTools.normaliseName2(word)));
		IndexSearcher searcher = new IndexSearcher(indexFile.getAbsolutePath());
		SimpleHitCollector shc = new SimpleHitCollector();
		searcher.search(q, shc);
		Set<File> files = getFilesFromSimpleHitCollector(shc, searcher.getIndexReader());
		return files;
	}

	/**Return all files that contain two words, within about 10 words of
	 * each other.
	 * 
	 * @param word1 The first word.
	 * @param word2 The second word.
	 * @return The files.
	 * @throws Exception
	 */
	public Set<File> filesForWordPair(String word1, String word2) throws Exception {
		PhraseQuery q = new PhraseQuery();
		q.add(new Term("txt", StringTools.normaliseName2(word1)));
		q.add(new Term("txt", StringTools.normaliseName2(word2)));
		q.setSlop(10);
		IndexSearcher searcher = new IndexSearcher(indexFile.getAbsolutePath());
		SimpleHitCollector shc = new SimpleHitCollector();
		searcher.search(q, shc);
		Set<File> files = getFilesFromSimpleHitCollector(shc, searcher.getIndexReader());
		return files;
	}
}
