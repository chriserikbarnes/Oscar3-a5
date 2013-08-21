package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import nu.xom.Serializer;

import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.isomorphism.UniversalIsomorphismTester;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainer;
import org.openscience.cdk.isomorphism.matchers.QueryAtomContainerCreator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.HydrogenAdder;
import org.openscience.cdk.tools.ValencyHybridChecker;

import uk.ac.cam.ch.wwmm.oscar3.terms.OBOOntology;
import uk.ac.cam.ch.wwmm.oscar3.terms.OntologyTerm;
import uk.ac.cam.ch.wwmm.oscar3.terms.Synonym;
import uk.ac.cam.ch.wwmm.ptclib.io.FileTools;
import uk.ac.cam.ch.wwmm.ptclib.saf.SafTools;
import uk.ac.cam.ch.wwmm.ptclib.scixml.SAFToInline;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

public class ChEBIGenerics {

	private static Set<String> badQueries = new HashSet<String>(StringTools.arrayToList(new String[]{
			"monoatomic monoanions",
			"monoatomic pentacations",
			"monoatomic tetracations",
			"dipositronium",
			"muon",
			"monoatomic dications",
			"positronium(1-)",
			"electron",
			"monoatomic tetraanions",
			"ununbium",
			"roentgenium",
			"muonium",
			"monoatomic trications",
			"monoatomic monocations",
			"darmstadtium",
			"monoatomic hexacations",
			"antimuon",
			"dimuonium",
			"positronium",
			"positron",
			"muonide",
			"diamond",
			"muoniomethane"}));
	private SmilesParser sp;
	private Map<String,QueryAtomContainer> qacs;
	private Map<String,String> qsmiles;
	private Map<String,String> qid;
	private HydrogenAdder ha;
	private Map<String,List<String>> cache;
	
	public ChEBIGenerics() {
		try {
			cache = new HashMap<String,List<String>>();
			sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
			ha = new HydrogenAdder(new ValencyHybridChecker());
			OBOOntology o = OBOOntology.getInstance();
			
			qacs = new HashMap<String, QueryAtomContainer>();
			qsmiles = new HashMap<String, String>();
			qid = new HashMap<String, String>();
			
			for(String id : o.getTerms().keySet()) {
				if(!id.startsWith("CHEBI:")) continue;
				OntologyTerm term = o.getTerms().get(id);
				String name = term.getName();
				if(badQueries.contains(name)) continue;
				if(name.startsWith("graph")) continue;
				if(name.matches(".*(muonium|protide|protium|protide|positronium).*")) continue;
				String smiles = null;
				for(Synonym s : term.getSynonyms()){
					if(s.getType().equals("RELATED SMILES")) { 
						smiles = s.getSyn();
					}
				}
				if(smiles != null && smiles.contains("*")) {
					try {
						//System.out.println(name + "\t" + smiles);
						QueryAtomContainer qac = makeQAC(smiles);
						qacs.put(name, qac);
						qsmiles.put(name, smiles);
						qid.put(name, id);
					} catch (Exception e) {
						//e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public List<String> getChebiIDs(String smiles) {
		if(cache.containsKey(smiles)) {
			return cache.get(smiles);
		}
		List<String> ids = new ArrayList<String>();
		try {
			IMolecule mol = sp.parseSmiles(smiles);
			ha.addExplicitHydrogensToSatisfyValency(mol);
			for(String s : qacs.keySet()) {
				if(UniversalIsomorphismTester.isSubgraph(mol, qacs.get(s))) {
					ids.add(qid.get(s));
					//System.out.println(s + "\t" + qsmiles.get(s));							
				}
			}
			cache.put(smiles, ids);
			return ids;
		} catch (Exception e) {
			cache.put(smiles, ids);
			return ids;
			//e.printStackTrace();
		}
	}
	
	private QueryAtomContainer makeQAC(String smiles) throws Exception {
		IMolecule mol = sp.parseSmiles(smiles);
		ha.addExplicitHydrogensToSatisfyValency(mol);
		QueryAtomContainer qac = QueryAtomContainerCreator.createBasicQueryContainer(mol);
		for(int i=0;i<qac.getAtomCount();i++) {
			IAtom a = qac.getAtom(i);
			if(a.getSymbol().equals("R")) {
				a.setSymbol("C");
				a.setAtomicNumber(6);
			}
		}
		return qac;
	}
	
	private void test() {
		OBOOntology o = OBOOntology.getInstance();

		//System.out.println("----------------------------------------------");
		for(String id : o.getTerms().keySet()) {
			if(!id.startsWith("CHEBI:")) continue;
			OntologyTerm term = o.getTerms().get(id);
			String name = term.getName();
			String smiles = null;
			for(Synonym s : term.getSynonyms()){
				if(s.getType().equals("RELATED SMILES")) { 
					smiles = s.getSyn();
				}
			}
			if(smiles != null && !smiles.contains("*")) {
				System.out.println(name + "\t" + smiles);
				System.out.println(getChebiIDs(smiles));
			}
		}	
	}
	
	public static void main(String[] args) throws Exception {
		ChEBIGenerics cg = new ChEBIGenerics();
		List<File> files = FileTools.getFilesFromDirectoryByName(new File("/local/scratch/ptc24/tmp/ne_annotated_070509"), "saf.xml");
		for(File f : files) {
			System.out.println(f);
			Document doc = new Builder().build(f);
			Nodes nn = doc.query("/saf/annot[slot/@name='SMILES']");
			for(int i=0;i<nn.size();i++) {
				Element e = (Element)nn.get(i);
				String smiles = e.query("slot[@name='SMILES']").get(0).getValue();
				System.out.println(smiles);
				List<String> ids = cg.getChebiIDs(smiles);
				String idstr = StringTools.objectListToString(ids, " ");
				if(ids.size() > 0) {
					SafTools.setSlot(e, "calcdOntIds", idstr);
				}
				
			}
			System.out.println();
			Serializer ser = new Serializer(new FileOutputStream(f));
			ser.write(doc);
			Document sourceDoc = new Builder().build(new File(f.getParentFile(), "source.xml"));
			Document inlineDoc = SAFToInline.safToInline(doc, sourceDoc, false);
			ser = new Serializer(new FileOutputStream(new File(f.getParentFile(), "markedup.xml")));
			ser.write(inlineDoc);
		}
	}
}
