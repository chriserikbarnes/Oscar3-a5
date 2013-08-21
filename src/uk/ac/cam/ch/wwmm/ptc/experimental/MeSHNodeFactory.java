package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.NodeFactory;
import nu.xom.Nodes;

/** Experimental code!
 * 
 * @author ptc24
 *
 */

public class MeSHNodeFactory extends NodeFactory {
	
	private Set<String> allowableSemTypes;
	private Set<String> testSemTypes;
	private Set<String> semTypes;
	private Set<String> nonChemSemTypes;
	private Set<String> nonChemWordSemTypes;
	private Set<String> stopWords;
	private Set<String> abbrevs;
	private Map<String, Integer> extractedChemTerms;
	private Map<String, Integer> extractedNonChemTerms;
	private Map<String, Set<String>> termsByType;
	
	public class MNFComparator implements Comparator<String> {
		public int compare(String s1, String s2) {
			return extractedChemTerms.get(s1).compareTo(extractedChemTerms.get(s2));
		}
	}

	public class MNFNCComparator implements Comparator<String> {
		public int compare(String s1, String s2) {
			return extractedNonChemTerms.get(s1).compareTo(extractedNonChemTerms.get(s2));
		}
	}
	
	public MeSHNodeFactory() {
		super();
		
		semTypes = new HashSet<String>();
		allowableSemTypes = new HashSet<String>();
		//allowableSemTypes.add("Chemical");
		allowableSemTypes.add("Organic Chemical");
		allowableSemTypes.add("Organophosphorus Compound");
		allowableSemTypes.add("Lipid");
		allowableSemTypes.add("Steroid");
		allowableSemTypes.add("Eicosanoid");
		allowableSemTypes.add("Inorganic Chemical");
		//allowableSemTypes.add("Antibiotic");
		testSemTypes = new HashSet<String>();
		testSemTypes.add("Age Group");
		
		stopWords = new HashSet<String>();
		stopWords.add("compound");
		stopWords.add("compound");
		stopWords.add("acidulated");
		stopWords.add("anhydrous");
		stopWords.add("absolute");
		stopWords.add("wood");
		stopWords.add("mixture");
		stopWords.add("gel");
		stopWords.add("dried");
		stopWords.add("retard");
		stopWords.add("fatty");
		stopWords.add("red");
		stopWords.add("orange");
		stopWords.add("yellow");
		stopWords.add("green");
		stopWords.add("blue");
		stopWords.add("indigo");
		stopWords.add("violet");
		stopWords.add("purple");
		stopWords.add("white");
		stopWords.add("black");
		stopWords.add("oil");
		stopWords.add("dietary");
		stopWords.add("toxin");
		stopWords.add("dye");
		stopWords.add("primary");
		stopWords.add("secondary");
		stopWords.add("tertiary");
		stopWords.add("quaternary");
		stopWords.add("mustard");
		stopWords.add("saturated");
		stopWords.add("glacial");
		stopWords.add("oral");
		stopWords.add("insecticide");
		stopWords.add("derivative");
		stopWords.add("ion");
		stopWords.add("conjugate");
		stopWords.add("cyclic");
		stopWords.add("one");
		stopWords.add("synthetic");
		stopWords.add("bay");
		stopWords.add("alloy");
		stopWords.add("of");
		stopWords.add("cement");
		stopWords.add("combination");
		stopWords.add("dental");
		stopWords.add("bayer");
		stopWords.add("fat");
		stopWords.add("solution");
		stopWords.add("hypertonic");
		stopWords.add("hypotonic");
		stopWords.add("density");
		stopWords.add("membrane");
		stopWords.add("factor");
		stopWords.add("organic");
		stopWords.add("inorganic");
		stopWords.add("gastric");
		stopWords.add("analog");
		stopWords.add("analogue");
		stopWords.add("substance");
		stopWords.add("pigment");
		stopWords.add("emulsion");
		stopWords.add("(sterile)");
		stopWords.add("toxin");
		stopWords.add("rubber");
		stopWords.add("bone");
		stopWords.add("resin");
		stopWords.add("complex");
		stopWords.add("complexes");
		stopWords.add("concise");
		stopWords.add("simplex");
		stopWords.add("liver");
		stopWords.add("low");
		stopWords.add("cell");
		stopWords.add("mineral");
		stopWords.add("anchor");
		stopWords.add("basic");
		stopWords.add("ion");
		stopWords.add("artificial");
		stopWords.add("conjugated");
		stopWords.add("intravenous");
		stopWords.add("fish");
		stopWords.add("ring");
		stopWords.add("solid");
		stopWords.add("liquid");
		stopWords.add("gas");
		stopWords.add("bluish");
		stopWords.add("rose");
		stopWords.add("platelet");
		stopWords.add("determinant");
		stopWords.add("and");
		stopWords.add("alloy");
		stopWords.add("essential");
		stopWords.add("chemical");
		stopWords.add("body");
		stopWords.add("bodies");
		stopWords.add("metal");
		stopWords.add("neutral");
		stopWords.add("streptococcal");
		stopWords.add("soap");
		stopWords.add("glacial");
		stopWords.add("base");
		stopWords.add("bridged");
		stopWords.add("hoe");
		stopWords.add("cord");
		stopWords.add("corn");
		stopWords.add("cottonseed");
		stopWords.add("or");
		stopWords.add("commercial");
		stopWords.add("earth");
		stopWords.add("plant");
		stopWords.add("safflower");
		stopWords.add("sesame");
		stopWords.add("steel");
		stopWords.add("antigen");
		stopWords.add("end");
		stopWords.add("product");
		stopWords.add("advanced");
		stopWords.add("opaque");
		stopWords.add("enamel");
		stopWords.add("bond");
		stopWords.add("composite");
		stopWords.add("antibiotic");
		stopWords.add("dispersion");
		stopWords.add("tar");
		stopWords.add("cod");
		stopWords.add("maize");
		stopWords.add("porcelain");
		stopWords.add("forte");
		stopWords.add("android");
		stopWords.add("alkaline");
		stopWords.add("with");
		stopWords.add("very");
		stopWords.add("high");
		stopWords.add("mineral");
		stopWords.add("solvent");
		stopWords.add("slow");
		stopWords.add("jelly");
		stopWords.add("activating");
		stopWords.add("soda");
		stopWords.add("paste");
		stopWords.add("sustained");
		stopWords.add("release");
		stopWords.add("sealant");
		stopWords.add("pearl");
		stopWords.add("contraceptive");
		stopWords.add("surgical");
		stopWords.add("condensed");
		stopWords.add("win");
		stopWords.add("stain");
		stopWords.add("belladonna");
		stopWords.add("butter");
		stopWords.add("milk");
		stopWords.add("stone");
		stopWords.add("plaster");
		stopWords.add("coal");
		stopWords.add("ratio");
		stopWords.add("injection");
		stopWords.add("oft");
		stopWords.add("dry");
		stopWords.add("aloe");
		stopWords.add("ergo");
		stopWords.add("pro");
		stopWords.add("ergo");
		stopWords.add("equine");
		stopWords.add("topical");
		stopWords.add("varnish");
		stopWords.add("varnishes");
		stopWords.add("disease");
		stopWords.add("more");
		stopWords.add("soluble");
		stopWords.add("insoluble");
		stopWords.add("latex");
		stopWords.add("linseed");
		stopWords.add("sublimate");
		stopWords.add("series");
		stopWords.add("element");
		stopWords.add("reversible");
		stopWords.add("irreversible");
		stopWords.add("pancreatic");
		stopWords.add("extract");
		stopWords.add("preparation");
		stopWords.add("aggregating");
		stopWords.add("expanded");
		stopWords.add("tears");
		stopWords.add("stainless");
		stopWords.add("all");
		stopWords.add("crack");
		stopWords.add("embryonic");
		stopWords.add("linkage");
		stopWords.add("fiber");
		stopWords.add("substitute");
		stopWords.add("tea");
		stopWords.add("tree");
		stopWords.add("like");
		stopWords.add("virulence");
		stopWords.add("receptor");
		stopWords.add("modulator");
		stopWords.add("clove");
		stopWords.add("firefly");
		stopWords.add("chain");
		stopWords.add("grain");
		stopWords.add("alkalies");
		stopWords.add("pan");
		stopWords.add("dried");
		stopWords.add("tartar");
		stopWords.add("emetic");
		stopWords.add("chalk");
		stopWords.add("limestone");
		stopWords.add("marble");
		stopWords.add("alabaster");
		stopWords.add("mace");
		stopWords.add("turmeric");
		stopWords.add("dome");
		stopWords.add("amalgam");
		stopWords.add("aerosol");
		stopWords.add("spray");
		stopWords.add("electrolyte");
		stopWords.add("enzyme");
		stopWords.add("rheum");
		stopWords.add("poppy");
		stopWords.add("seed");
		stopWords.add("direct");
		stopWords.add("propellant");
		stopWords.add("crystal");
		stopWords.add("glass");
		stopWords.add("dynamite");
		stopWords.add("radio");
		stopWords.add("syrup");
		stopWords.add("margarine");
		stopWords.add("corrosive");
		stopWords.add("light");
		stopWords.add("metric");
		stopWords.add("versed");
		stopWords.add("fungal");
		stopWords.add("laughing");
		stopWords.add("tear");
		stopWords.add("pan");
		stopWords.add("antibiotic");
		stopWords.add("petroleum");
		stopWords.add("angel");
		stopWords.add("dust");
		stopWords.add("bloat");
		stopWords.add("guard");
		stopWords.add("spandex");
		stopWords.add("predate");
		stopWords.add("quartz");
		stopWords.add("visual");
		stopWords.add("soap");
		stopWords.add("caustic");
		stopWords.add("talc");
		stopWords.add("continuous");
		stopWords.add("rabbit");
		stopWords.add("aorta");
		stopWords.add("contracting");
		stopWords.add("wax");
		stopWords.add("waxes");
		stopWords.add("food");
		stopWords.add("cream");
		stopWords.add("alter");
		stopWords.add("reactive");
		stopWords.add("pit");
		stopWords.add("fissure");
		stopWords.add("conclude");
		stopWords.add("radical");
		stopWords.add("table");
		stopWords.add("baking");
		stopWords.add("amber");
		stopWords.add("ambergris");
		stopWords.add("fiber");
		stopWords.add("vinegar");
		stopWords.add("substitute");
		stopWords.add("plastic");
		stopWords.add("sol");
		stopWords.add("modified");
		stopWords.add("ambush");
		stopWords.add("long");
		stopWords.add("butter");
		stopWords.add("natural");
		stopWords.add("non");
		stopWords.add("sine");
		stopWords.add("dip");
		stopWords.add("yellowish");
		stopWords.add("short");
		stopWords.add("bid");
		stopWords.add("trauma");
		stopWords.add("lost");
		stopWords.add("gag");
		stopWords.add("cross");
		stopWords.add("nuclear");
		stopWords.add("fast");
		stopWords.add("derived");
		stopWords.add("aggregation");
		stopWords.add("enhancing");
		stopWords.add("activity");
		stopWords.add("peg");
		stopWords.add("sand");
		stopWords.add("reacting");
		stopWords.add("constant");
		stopWords.add("steams");
		stopWords.add("far");
		stopWords.add("buffer");
		stopWords.add("anti");
		stopWords.add("stage");
		stopWords.add("specific");
		stopWords.add("anchor");
		stopWords.add("air");
		stopWords.add("seal");
		stopWords.add("sealer");
		stopWords.add("system");
		stopWords.add("group");
		stopWords.add("cloves");
		
		termsByType = new HashMap<String, Set<String>>();
		
		nonChemSemTypes = new HashSet<String>();
		nonChemSemTypes.add("Manufactured Object");
		nonChemSemTypes.add("Intellectual Product");
		nonChemSemTypes.add("Body Location or Region");
		nonChemSemTypes.add("Sign or Symptom");
		nonChemSemTypes.add("Neoplastic Process");
		nonChemSemTypes.add("Tissue");
		nonChemSemTypes.add("Body Part, Organ, or Organ Component");
		nonChemSemTypes.add("Virus");
		nonChemSemTypes.add("Congenital Abnormality");
		nonChemSemTypes.add("Body System");
		//nonChemSemTypes.add("Immunologic Factor");
		nonChemSemTypes.add("Patient or Disabled Group");
		nonChemSemTypes.add("Finding");
		nonChemSemTypes.add("Human-caused Phenomenon or Process");
		nonChemSemTypes.add("Quantitative Concept");
		nonChemSemTypes.add("Health Care Related Organization");
		nonChemSemTypes.add("Mental Process");
		nonChemSemTypes.add("Individual Behavior");
		nonChemSemTypes.add("Occupational Activity");
		nonChemSemTypes.add("Plant");
		nonChemSemTypes.add("Organization");
		nonChemSemTypes.add("Invertebrate");
		nonChemSemTypes.add("Cell");
		nonChemSemTypes.add("Phenomenon or Process");
		nonChemSemTypes.add("Idea or Concept");
		nonChemSemTypes.add("Organ or Tissue Function");
		nonChemSemTypes.add("Occupation or Discipline");
		nonChemSemTypes.add("Governmental or Regulatory Activity");
		nonChemSemTypes.add("Social Behavior");
		nonChemSemTypes.add("Alga");
		nonChemSemTypes.add("Fungus");
		nonChemSemTypes.add("Body Space or Junction");
		nonChemSemTypes.add("Cell Component");
		nonChemSemTypes.add("Daily or Recreational Activity");
		nonChemSemTypes.add("Qualitative Concept");
		nonChemSemTypes.add("Temporal Concept");
		nonChemSemTypes.add("Acquired Abnormality");
		nonChemSemTypes.add("Professional or Occupational Group");
		nonChemSemTypes.add("Age Group");
		nonChemSemTypes.add("Biomedical Occupation or Discipline");
		nonChemSemTypes.add("Geographic Area");
		nonChemSemTypes.add("Health Care Activity");
		nonChemSemTypes.add("Substance");
		nonChemSemTypes.add("Environmental Effect of Humans");
		nonChemSemTypes.add("Self-help or Relief Organization");
		nonChemSemTypes.add("Embryonic Structure");
		nonChemSemTypes.add("Gene or Genome");
		nonChemSemTypes.add("Reptile");
		nonChemSemTypes.add("Functional Concept");
		nonChemSemTypes.add("Mammal");
		nonChemSemTypes.add("Physical Object");
		nonChemSemTypes.add("Amphibian");
		nonChemSemTypes.add("Professional Society");
		nonChemSemTypes.add("Machine Activity");
		nonChemSemTypes.add("Rickettsia or Chlamydia");
		nonChemSemTypes.add("Cell or Molecular Dysfunction");
		nonChemSemTypes.add("Anatomical Abnormality");
		nonChemSemTypes.add("Fish");
		nonChemSemTypes.add("Animal");
		nonChemSemTypes.add("Anatomical Structure");
		nonChemSemTypes.add("Activity");
		nonChemSemTypes.add("Behavior");
		nonChemSemTypes.add("Group");
		nonChemSemTypes.add("Event");
		//nonChemSemTypes.add("Physiologic Function");
		nonChemSemTypes.add("Archaeon");
		nonChemSemTypes.add("Experimental Model of Disease");
		nonChemSemTypes.add("Clinical Attribute");
		nonChemSemTypes.add("Nucleotide Sequence");
		nonChemSemTypes.add("Bird");
		nonChemSemTypes.add("Fully Formed Anatomical Structure");
		nonChemSemTypes.add("Regulation or Law");
		nonChemSemTypes.add("Research Device");
		nonChemSemTypes.add("Biologic Function");
		nonChemSemTypes.add("Family Group");
		nonChemSemTypes.add("Organism");
		nonChemSemTypes.add("Molecular Biology Research Technique");
		nonChemSemTypes.add("Classification");
		nonChemSemTypes.add("Educational Activity");
		nonChemSemTypes.add("Group Attribute");
		nonChemSemTypes.add("Conceptual Entity");
		nonChemSemTypes.add("Entity");
		nonChemSemTypes.add("Vertebrate");
		nonChemSemTypes.add("Molecular Sequence");
		nonChemSemTypes.add("Human");
		nonChemSemTypes.add("Language");
		
		nonChemWordSemTypes = new HashSet<String>();
		nonChemWordSemTypes.add("Injury or Poisoning");
		nonChemWordSemTypes.add("Disease or Syndrome");
		nonChemWordSemTypes.add("Therapeutic or Preventive Procedure");
		nonChemWordSemTypes.add("Pathologic Function");
		nonChemWordSemTypes.add("Medical Device");
		nonChemWordSemTypes.add("Bacterium");
		nonChemWordSemTypes.add("Laboratory or Test Result");
		nonChemWordSemTypes.add("Diagnostic Procedure");
		nonChemWordSemTypes.add("Cell Function");
		nonChemWordSemTypes.add("Laboratory Procedure");
		nonChemWordSemTypes.add("Mental or Behavioral Dysfunction");
		nonChemWordSemTypes.add("Spatial Concept");
		nonChemWordSemTypes.add("Amino Acid Sequence");
		nonChemWordSemTypes.add("Body Substance");
		nonChemWordSemTypes.add("Organism Attribute");
		nonChemWordSemTypes.add("Research Activity");
		nonChemWordSemTypes.add("Physiologic Function");
		nonChemWordSemTypes.add("Population Group");
		
		abbrevs = new HashSet<String>();
		
	}
	
	
	
	@Override
	public Nodes finishMakingElement(Element elem) {
		if("DescriptorRecord".equals(elem.getLocalName()) || "SupplementalRecord".equals(elem.getLocalName())) {
			return new Nodes();
		} else if("Concept".equals(elem.getLocalName())) {
			Nodes semTypes = elem.query("SemanticTypeList/SemanticType/SemanticTypeName");
			boolean isChem = false;
			for(int i=0;i<semTypes.size();i++) {
				String st = semTypes.get(i).getValue();
				if(!this.semTypes.contains(st)) {
					this.semTypes.add(st);
					//System.out.println(st);
				}
				if(allowableSemTypes.contains(st)) {
					isChem = true;
					break;
				}
			}
			if(isChem) {
				String name = elem.query("ConceptName/String").get(0).getValue();
				if(containsStopword(name)) return new Nodes();
				Nodes termNames = elem.query("TermList/Term/String");
				for(int i=0;i<termNames.size();i++) {
					if(containsStopword(termNames.get(i).getValue())) return new Nodes();
				}
				dissectTerm(name, true, false);
				for(int i=0;i<termNames.size();i++) {
					dissectTerm(termNames.get(i).getValue(), true, false);
				}
			} else {
				String name = elem.query("ConceptName/String").get(0).getValue();						
				for(int i=0;i<semTypes.size();i++) {
					String st = semTypes.get(i).getValue();
					//if(!termsByType.containsKey(st)) termsByType.put(st, new LinkedHashSet<String>());
					//termsByType.get(st).add(name);
					if(nonChemSemTypes.contains(st)) {
						dissectTerm(name, false, false);
						Nodes termNames = elem.query("TermList/Term/String");
						for(int j=0;j<termNames.size();j++) {
							dissectTerm(termNames.get(j).getValue(), false, false);
						}
						break;
					} else if(nonChemWordSemTypes.contains(st)) {
						dissectTerm(name, false, true);
					}
				}				
			}
			return new Nodes();
		} else {
			return new Nodes(elem);
		}
	}
	
	public boolean containsStopword(String term) {
		if(term.matches(".*\\b[Tt]oxins?\\b.*")) return true;
		if(term.matches(".*\\b[Bb]rands?\\b.*")) return true;
		if(term.matches(".*\\b[Aa]gents?\\b.*")) return true;
		if(term.matches(".*\\b[Hh]ormones?\\b.*")) return true;
		return false;
	}
	
	@Override
	public Document startMakingDocument() {
		extractedChemTerms = new LinkedHashMap<String, Integer>();
		extractedNonChemTerms = new LinkedHashMap<String, Integer>();
		// TODO Auto-generated method stub
		return super.startMakingDocument();
	}
	
	@Override
	public void finishMakingDocument(Document arg0) {
		File outDir = new File("/home/ptc24/MeSH");
		File chemOutFile = new File(outDir, "newchem.txt");
		File nonChemOutFile = new File(outDir, "newnonchem.txt");
		
		Writer chemWriter; 
		Writer nonChemWriter;			
		
		try {
			chemWriter = new FileWriter(chemOutFile);
			nonChemWriter = new FileWriter(nonChemOutFile);			

			for(String s : abbrevs) {
				System.out.println(s);
				//nonChemWriter.write(s + "\n");
			}
		
		//if(1==1) return;

			List<String> l = new ArrayList<String>(extractedChemTerms.keySet());
			Collections.sort(l, Collections.reverseOrder(new MNFComparator()));
			for(String term : l) {
				System.out.println(term);
				chemWriter.write(term+"\n");
			}
			
			System.out.println("**************");
			
			l = new ArrayList<String>(extractedNonChemTerms.keySet());
			Collections.sort(l, Collections.reverseOrder(new MNFNCComparator()));
			for(String term : l) {
				System.out.println(term);
				nonChemWriter.write(term+"\n");
			}
			chemWriter.close();
			nonChemWriter.close();

		} catch (Exception e) {
			return;
		}
		
		
		System.out.println(extractedChemTerms.size());
		System.out.println(extractedNonChemTerms.size());
		//for(String t : termsByType.keySet()) {
		//	System.out.println();
		//	System.out.println(t);
		//	System.out.println(StringTools.multiplyString("*", t.length()));
		//	for(String term : termsByType.get(t)) System.out.println(term);
		//}
	}
	
	public void dissectTerm(String term, boolean isChem, boolean oneWordOnly) {
		//if(term.contains("coli")) System.out.println(term);
		String [] substrs = term.split(",? ");
		if(substrs.length > 1 && oneWordOnly) return;
		for(int i=0;i<substrs.length;i++) {
			String s = substrs[i];
			if(!s.matches(".*[a-z][a-z].*")) {
				if(!isChem) abbrevs.add(s);
				continue;
			}
			s = s.toLowerCase();
			if(!isChem || checkTerm(s)) addTerm(s, isChem);
		}
	}
	
	public boolean checkTerm(String term) {
		if(extractedChemTerms.containsKey(term)) return true;
		if(stopWords.contains(term)) return false;
		if(term.matches("\\([a-z]+\\)?")) return false;
		if(term.matches("[a-z]+\\)")) return false;
		if(term.endsWith("s") && stopWords.contains(term.substring(0, term.length()-1))) return false;
		if(term.endsWith("mer")) return false;
		if(term.endsWith("mers")) return false;
		if(term.endsWith("cyclic")) return false;
		if(term.endsWith("cyclics")) return false;
		if(term.endsWith("labelled")) return false;
		if(term.endsWith("protein")) return false;
		if(term.endsWith("proteins")) return false;
		if(term.endsWith("saturated")) return false;
		if(term.endsWith("'s")) return false;
		return true;
	}
	
	public void addTerm(String term, boolean isChem) {
		Map<String, Integer> termMap;
		if(isChem) {
			termMap = extractedChemTerms;
		} else {
			termMap = extractedNonChemTerms;
		}		
		if(!termMap.containsKey(term)) termMap.put(term, 0);
		termMap.put(term, termMap.get(term)+1);
			
	}

	public static void main(String[] args) throws Exception {
		new Builder(new MeSHNodeFactory()).build(new File("/home/ptc24/MeSH/desc2006"));
		System.out.println("Loaded!");
	}
	
}
