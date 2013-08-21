package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.ptclib.datastruct.Bag;

public class PrunePubChemSynonyms {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File o = new File("/scratch/CID-Synonym-filtered");
		Writer w = new FileWriter(o);
		
		List<Pattern> patterns = new ArrayList<Pattern>();
		patterns.add(Pattern.compile(".+_.+"));
		patterns.add(Pattern.compile(".+\\d{4,}.*"));
		patterns.add(Pattern.compile(".+No\\..+"));
		patterns.add(Pattern.compile("[A-Z]{2,3}[0-9]{3,12}"));
		patterns.add(Pattern.compile(".*[A-Z]{6,}.*"));
		patterns.add(Pattern.compile(".*@.*"));
		patterns.add(Pattern.compile("ZINC\\d+"));
		patterns.add(Pattern.compile("[A-Z]?\\d+_(FLUKA|SIAL|ALDRICH|REIDEL)"));
		patterns.add(Pattern.compile(".+, .+"));
		patterns.add(Pattern.compile(".+ \\[.+\\]"));
		patterns.add(Pattern.compile(".+ \\(.+\\)"));
		patterns.add(Pattern.compile("EINECS .*"));
		patterns.add(Pattern.compile("\\d+-\\d+-\\d+"));
		patterns.add(Pattern.compile(".+ waste no\\..+"));
		patterns.add(Pattern.compile(".+ Chemical Code .+"));
		patterns.add(Pattern.compile("Caswell No\\. .+"));
		patterns.add(Pattern.compile("AKI-[A-Z][A-Z][A-Z]-\\d+"));
		patterns.add(Pattern.compile("UPCMLD.*"));
		patterns.add(Pattern.compile("MLS-\\d+\\.\\d+"));
		patterns.add(Pattern.compile(".+([Mm]ixture|[Ss]olution|[Cc]olloid|[Pp]olymer|[Oo]ligomer|[Ss]tabili[sz]er" +
				"|[Ff]ungicide|[Ss]pray|Beilstein|Shell|Stauffer|[Bb]rands? |[Nn]umber|[Gg]roup|[Ss]tandard|[Rr]etard" +
				"|[Cc]ombination|[Dd]eriv\\.|[Dd]er\\.|DER\\.|[Ii]sostere|[Ss]equence|[Dd]erived|[Bb]ased|[Cc]ongener" +
				"|[Dd]eriv [Oo]f |[Aa]duct| \\+ | & | B\\.P\\.).*| [Cc]onjgates?"));
		patterns.add(Pattern.compile(".*([Ss]ubstituted|[Tt]riterpenoid|[Pp]eptides|[Nn]ucleotides|[Ee]sters|[Aa]na\\." +
				"|[Cc]ontain(s|ing)|[Pp]rotonated|[Cc]ompd\\.|[Tt]reated|[Pp]art [Oo]f| [Ff]or " +
				"| [Oo]n | [Aa]nd |[Rr]esidue|[Ii]solated?|[Ll]ocated|[Pp]rotein|[Ff]ragment" +
				"|[Tt]runcated|[Ii]mpregnated|[Aa]ssociated|[Cc]oated|[Ff]rom |[Ss]tereoisomer" +
				"| [Ss]ite| [Ww]ith |[Uu]named|[Ss]achet|[Aa]nalog(ue)?s?|[Dd]eriv[ai]tive" +
				"|[Pp]rodrug|[Oo]ligonucleotide|[Cc]omponent|[Tt]ablet|[Aa]ccessor(y|ies)" +
				"| [Aa]gent).*| [Cc]omplex|[Pp]resent|[Rr]emark"));
		patterns.add(Pattern.compile("(From ).*"));
		patterns.add(Pattern.compile("A[A-Z]-\\d+/\\d+"));
		patterns.add(Pattern.compile("NYU-.*"));
		patterns.add(Pattern.compile("JM-.*"));
		patterns.add(Pattern.compile("NCI\\d+_\\d+"));
		patterns.add(Pattern.compile(".+ \\(\\d+(:\\d+)+\\)"));
		patterns.add(Pattern.compile("See Remark.*"));
		patterns.add(Pattern.compile("(MLS|SMR|STK|BAS |BRN |SPECTRUM|AIDS-?|STOCK\\d[A-Z]|NCGC" +
				"|Spectrum\\d?_|LMFA|TULIP|NSC ?-?|CHEBI:|Tocris-|HSDB ?|CCRIS " +
				"|SpecPlus|KBio(\\d|[A-Z][A-Z])_|DivK1c|SPBio_|NINDS_|AI3-|BBV-|CPD-)\\d+"));
		patterns.add(Pattern.compile(".+ mixed.*"));
		patterns.add(Pattern.compile("InChI=.*"));
		patterns.add(Pattern.compile(".+ \\(USAN|USP|medium|TN\\)"));
		patterns.add(Pattern.compile("NCIStruc.*"));
		patterns.add(Pattern.compile("Noname|Hem"));
		patterns.add(Pattern.compile("C\\d\\d\\d\\d\\d"));
		patterns.add(Pattern.compile(".*\\d%.*"));
		patterns.add(Pattern.compile(".+ ([Dd]er(iv)?|[Aa]lkaloid|[Ii]nhibitor)"));
		patterns.add(Pattern.compile(".+carboxylic"));
		patterns.add(Pattern.compile(".*;[^0-9].*"));
		patterns.add(Pattern.compile(".*[0-9]+\\.[0-9]+ .*"));
		patterns.add(Pattern.compile("(.+ )?([Cc]old|[Ff]lu|[Aa]llergy|[Ss]inus|[Hh]eadache" +
				"|[Dd]rowsiness|[Ff]ormula|[Pp]ain|[Ss]evere|[Mm]enstrual|[Mm]aximum" +
				"|[Ss]trength|[Mm]edicine|[Cc]aplets|[Pp]ediatric|[Cc]hewable" +
				"|[Dd]econgestant|[Ll]iquid|[Rr]oots?|[Ll]ea(f|ves))( .+)?"));
		//patterns.add(Pattern.compile(""));
		//patterns.add(Pattern.compile(""));
		//patterns.add(Pattern.compile(""));
		//patterns.add(Pattern.compile(""));
		//patterns.add(Pattern.compile(""));

		Bag<Pattern> patternBag = new Bag<Pattern>();
		
		File f = new File("/scratch/CID-Synonym-new");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		int totalSyn = 0;
		int goodSyn = 0;
		
		
		while(line != null) {
			String [] ll = line.split("\t");
			if(ll.length != 2) {
				line = br.readLine();
				continue;
			}
			String name = ll[1];
			//System.out.println(name);
			totalSyn++;
			boolean isGood = true;
			for(Pattern p : patterns) {
				//System.out.println(p);
				if(p.matcher(name).matches()) {
					isGood = false;
					patternBag.add(p);
					break;
				}
			}
			if(isGood) {
				w.write(line + "\n");
				goodSyn++;
			}
			if(totalSyn % 100000 == 0) System.out.println(totalSyn);
			line = br.readLine();
		}
		System.out.println(totalSyn + "\t->\t" + goodSyn);
		
		for(Pattern p : patternBag.getList()) {
			System.out.println(p + "\t" + patternBag.getCount(p));
		}
		w.close();
	}

}
