package uk.ac.cam.ch.wwmm.oscar3.recogniser.memm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.oscar3.chemnamedict.ChemNameDictSingleton;
import uk.ac.cam.ch.wwmm.oscar3.models.ExtractTrainingData;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.TokenTypes;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis.NGram;
import uk.ac.cam.ch.wwmm.oscar3.terms.TermSets;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**Converts a list of tokens into features for the MEMM.
 * 
 * @author ptc24
 *
 */
final class FeatureExtractor {
	
	private TokenSequence tokSeq;
	private List<List<String>> features;
	private List<List<String>> contextableFeatures;
	private List<List<String>> bigramableFeatures;
	
	private Set<String> stretchable;
	
	private static Pattern suffixPattern = Pattern.compile(".*?((yl|ide|ite|ate|ium|ane|yne|ene|ol|" +
			"ase|ic|oxy|ino|at(ed|ion|ing)|lys(is|es|ed|ing|tic)|i[sz](ed|ations|ing)|)s?)");
	private static Pattern wordPattern = Pattern.compile(".*[a-z][a-z].*");
	//private static Pattern containsUpperPattern = Pattern.compile(".*[A-Z].*");
	//private static Pattern romanPattern = Pattern.compile("I{1,4}|I{1,3}[XV]|[XV]I{0,4}");
	private static Pattern oxPattern = Pattern.compile("\\(([oO]|[iI]{1,4}|[iI]{0,3}[xvXV]|[xvXV][iI]{0,4})\\)");
	//private static Pattern dPattern = Pattern.compile("\\S+-d\\d+");
	//private static Pattern containsSquareBracketsPattern = Pattern.compile(".*\\[.+\\].*");
	//private static Pattern p450Pattern = Pattern.compile("CYP.*|P-?450.*");
	//private static Pattern fiftyPattern = Pattern.compile("[A-Za-z][A-Za-z]+50");
	private static Pattern pnPattern = Pattern.compile("(Mc|Mac)?[A-Z]\\p{Ll}\\p{Ll}+(s'|'s)?");
	
	private boolean noPC = false;
	private boolean noNG = false;
	private boolean noC = false;
	
	private boolean newSuffixes = false;
	
	public FeatureExtractor(TokenSequence tokSeq, String domain) {
		stretchable = new HashSet<String>();
		stretchable.add("and");
		for(int i=0;i<StringTools.hyphens.length();i++) stretchable.add(StringTools.hyphens.substring(i,i+1));
		this.tokSeq = tokSeq;
		makeFeatures(domain);
		
	}

	public List<String> getFeatures(int pos) {
		return features.get(pos);
	}
	
	public void printFeatures() {
		for(List<String> f : features) System.out.println(f);
	}
	
	private void makeFeatures(String domain) {
		contextableFeatures = new ArrayList<List<String>>(tokSeq.size());
		bigramableFeatures = new ArrayList<List<String>>(tokSeq.size());
		features = new ArrayList<List<String>>(tokSeq.size());
		for(int i=0;i<tokSeq.size();i++) {
			contextableFeatures.add(new LinkedList<String>());
			bigramableFeatures.add(new LinkedList<String>());
			features.add(new LinkedList<String>());
		}
		for(int i=0;i<tokSeq.size();i++) {
			makeFeatures(i);
		}
		for(int i=0;i<tokSeq.size();i++) {
			mergeFeatures(i);
		}
		if(domain != null) {
			for(int i=0;i<tokSeq.size();i++) {
				List<String> ff = new ArrayList<String>(features.get(i));
				//features.get(i).clear();
				for(String f : ff) {
					features.get(i).add("D{" + domain + "}::" + f);
				}
			}
		}
	}
	
	private void makeFeatures(int position) {
		List<String> local = features.get(position);
		List<String> contextable = contextableFeatures.get(position);
		List<String> bigramable = bigramableFeatures.get(position);

		Token token = tokSeq.getToken(position);
		String word = token.getValue();
		contextable.add(("w=" + word));
		
		String normWord = StringTools.normaliseName(word);
		if(!word.equals(normWord)) {
			contextable.add(("w=" + normWord));			
		}
		
		ExtractTrainingData etd = ExtractTrainingData.getInstance();
		
		if(noPC) {
			if(word.length() < 4) {
				bigramable.add(("w=" + word));
				if(!word.equals(normWord)) bigramable.add(("w=" + normWord));
			}
		} else {
			if(word.length() < 4 || etd.polysemous.contains(word) || etd.rnEnd.contains(word) || etd.rnMid.contains(word)) {
				bigramable.add(("w=" + word));
				if(!word.equals(normWord)) bigramable.add(("w=" + normWord));
			}			
		}
	
		if(!noPC) {
			if(etd.rnEnd.contains(word)) {
				bigramable.add("$RNEND");
				contextable.add("$RNEND");
			}
			if(etd.rnMid.contains(word)) {
				bigramable.add("$RNMID");
				contextable.add("$RNMID");
			}			
		}
		
		String wts = StringTools.removeTerminalS(normWord);
		contextable.add("wts=" + wts);
		
		String wordShape = wordShape(word);
		if(wordShape.length() > 3) wordShape = "complex";
		if(!wordShape.equals(word)) {
			String wordShapeFeature = "ws=" + wordShape;
			bigramable.add(wordShapeFeature);
			contextable.add(wordShapeFeature);			
		}
		
		String suffix = getSuffix(word);
		String suffixFeature = "s=" + suffix;
		//bigramable.add(suffixFeature);
		contextable.add(suffixFeature);
		
		if(!noNG) {
			String decWord = "^" + word + "$";
			for(int j=0;j<decWord.length()-3;j++) {
				for(int k=1;k<=4;k++) {
					if(j < 4 - k) continue;
					local.add((k + "G=" + decWord.substring(j, j+k)).intern());
				}
			}
		}
		
		if(wordPattern.matcher(word).matches()) {
			if(newSuffixes) {
				double suffixScore = NGram.getInstance().testWordSuffix(word);
				String type = TokenTypes.getTypeForSuffix(token.getValue());
				if(noPC) {
					suffixScore = -1;
					if(TermSets.getUsrDictWords().contains(normWord) ||
							TermSets.getUsrDictWords().contains(word)) suffixScore = -100;
					if(ChemNameDictSingleton.hasName(word)) suffixScore = 100;								
				} else {
					suffixScore = Math.max(suffixScore, -15.0);
					suffixScore = Math.min(suffixScore, 15.0);
					for(int i=0;i<suffixScore;i++) local.add(("sscore+=" + type).intern());
					for(int i=0;i>suffixScore;i--) local.add(("sscore-=" + type).intern());
					
					if(TermSets.getUsrDictWords().contains(normWord) ||
							TermSets.getUsrDictWords().contains(word)) suffixScore = -100;
					if(ExtractTrainingData.getInstance().chemicalWords.contains(normWord)) suffixScore = 100;
					if(ChemNameDictSingleton.hasName(word)) suffixScore = 100;				
					double ngscore = NGram.getInstance().testWord(word);
					ngscore = Math.max(ngscore, -15.0);
					ngscore = Math.min(ngscore, 15.0);
					for(int i=0;i<ngscore;i++) local.add(("ngscore+=" + type).intern());
					for(int i=0;i>ngscore;i--) local.add(("ngscore-=" + type).intern());
				}
				if(suffixScore > 0) {
					contextable.add("ct=" + type);
					bigramable.add("ct=" + type);
				}
			} else {
				double ngscore = NGram.getInstance().testWord(word);
				// Already seen
				String type = TokenTypes.getTypeForSuffix(token.getValue());
				if(noPC) {
					ngscore = -1;
					if(TermSets.getUsrDictWords().contains(normWord) ||
							TermSets.getUsrDictWords().contains(word)) ngscore = -100;
					if(ChemNameDictSingleton.hasName(word)) ngscore = 100;								
				} else {
					ngscore = Math.max(ngscore, -15.0);
					ngscore = Math.min(ngscore, 15.0);
					for(int i=0;i<ngscore;i++) local.add(("ngram+=" + type).intern());
					for(int i=0;i>ngscore;i--) local.add(("ngram-=" + type).intern());
					
					if(TermSets.getUsrDictWords().contains(normWord) ||
							TermSets.getUsrDictWords().contains(word)) ngscore = -100;
					if(ExtractTrainingData.getInstance().chemicalWords.contains(normWord)) ngscore = 100;
					if(ChemNameDictSingleton.hasName(word)) ngscore = 100;				
				}
				if(ngscore > 0) {
					contextable.add("ct=" + type);
					bigramable.add("ct=" + type);
				}				
			}
		} 
		
		if(ChemNameDictSingleton.hasName(word)) local.add("inCND");

		if(TermSets.getElements().contains(normWord)) {
			contextable.add("element");
			bigramable.add("element");
		}
		if(TermSets.getEndingInElementPattern().matcher(word).matches()) {
			contextable.add("endsinem");
			bigramable.add("endsinem");
		}
		if(oxPattern.matcher(word).matches()) {
			contextable.add("oxidationState");
			bigramable.add("oxidationstate");
		}

		if(TermSets.getStopWords().contains(normWord) || ChemNameDictSingleton.hasStopWord(normWord)) {
			local.add("$STOP:STOPWORD");
		}
		if(TermSets.getClosedClass().contains(normWord)) {
			local.add("$STOP:CLOSEDCLASS");
		}
		if(noPC) {
			if(TermSets.getUsrDictWords().contains(normWord) &&
					!(ChemNameDictSingleton.hasName(normWord))) {
				local.add("$STOP:UDW");
			}
		} else {
			if(ExtractTrainingData.getInstance().nonChemicalWords.contains(normWord)) {
				local.add("$STOP:NCW");
			}
			if(ExtractTrainingData.getInstance().nonChemicalNonWords.contains(normWord)
					&& !TermSets.getElements().contains(normWord)) {
				local.add("$STOP:NCNW");
			}
			if(TermSets.getUsrDictWords().contains(normWord) &&
					!(ChemNameDictSingleton.hasName(normWord) || ExtractTrainingData.getInstance().chemicalWords.contains(normWord))) {
				local.add("$STOP:UDW");
			}			
		}
	}
	
	private void mergeFeatures(int position) {
		List<String> mergedFeatures = features.get(position);
		
		int backwards = Math.min(1, position);
		int forwards = 1;
		/*while((position + forwards) < tokSeq.size()) {
			String fv = tokSeq.getToken(position + forwards).getValue();
			if(fv == null) break;
			if(stretchable.contains(fv)) {
				forwards++;
			} else {
				break;
			}
		}*/
		forwards = Math.min(forwards, tokSeq.size() - position - 1);
		//boolean expanded = false;
		/*String word = tokSeq.getToken(position).getValue();
		if(word.equals("lead") || (word.length() < 3 && TermSets.getElements().contains(word))) {
			backwards = Math.min(2, position);
			forwards = Math.min(2, tokSeq.size() - position - 1);			
			expanded = true;
		}*/
		
		if(!noC) {
			for(int i = -backwards;i<=forwards;i++) {
				for(String cf : contextableFeatures.get(position + i)) {
					mergedFeatures.add(("c" + i + ":" + cf).intern());				
				}
			}			
		}
		
		// NB support left in incase bg:0:0: etc. become viable
		for(int i = -backwards;i<=forwards;i++) {
			for(int j=i+1;j<=forwards;j++) {
				if(j-i == 1 || j == i) {
					String prefix = "bg:" + i + ":" + j + ":";
					for(String feature1 : bigramableFeatures.get(position + i)) {
						for(String feature2 : bigramableFeatures.get(position + j)) {
							// feature1 != feature2 is not a bug, if j == i
							if(j != i || feature1 != feature2) mergedFeatures.add((prefix + feature1 + "__" + feature2).intern());
						}
					}
				}
				//String prefix = "bg:" + i + ":" + j + ":";
				//for(String bg : StringTools.makeNGrams(bigramableFeatures.subList(i + position, j+1 + position))) {
				//	mergedFeatures.add((prefix + bg).intern());
				//}
			}
		}
		
		String word = tokSeq.getToken(position).getValue();

		if(pnPattern.matcher(word).matches()) {
			boolean suspect = false;
			if(word.matches("[A-Z][a-z]+") && TermSets.getUsrDictWords().contains(word.toLowerCase()) && !TermSets.getUsrDictWords().contains(word)) suspect = true;
			if(!noPC && ExtractTrainingData.getInstance().pnStops.contains(word)) suspect = true;
			int patternPosition = position + 1;
			while(patternPosition < (tokSeq.size()-2) 
					&& StringTools.hyphens.contains(tokSeq.getToken(patternPosition).getValue())
					&& pnPattern.matcher(tokSeq.getToken(patternPosition+1).getValue()).matches()) {
				patternPosition += 2;
				suspect = false;
			}
			if(patternPosition < tokSeq.size()) {
				for(String feature : bigramableFeatures.get(patternPosition)) {
					if(suspect) {
						mergedFeatures.add(("suspectpn->bg:" + feature).intern());
					} else {
						mergedFeatures.add(("pn->bg:" + feature).intern());						
					}
				}
				if(!suspect) {
					for(String feature : contextableFeatures.get(patternPosition)) {
						mergedFeatures.add(("pn->c:" + feature).intern());						
					}
				}
				for(int i=position+1;i<=patternPosition;i++) {
					if(suspect) {
						features.get(i).add("inSuspectPN");
					} else {
						features.get(i).add("inPN");
					}
				}
			}
			/*if(suspect) {
				System.out.println("Suspect DGC: " + tokSeq.getSubstring(position, patternPosition));				
			} else {
				System.out.println("Non-Suspect DGC: " + tokSeq.getSubstring(position, patternPosition));
			}*/
		}

		
		//for(int i=Math.max(0, position-5);i<Math.min(position+5+1,tokSeq.size());i++) {
		//	if(i == position) continue;
		//	mergedFeatures.add(("ww=" + tokSeq.getToken(i).getValue()).intern());
		//}
		
	}
	
	private String getSuffix(String word) {
		Matcher m = suffixPattern.matcher(word);
		if(m.matches()) {
			return m.group(1);
		} else {
			 return "unknown";
		}

	}
		
	private static String wordShape(String word) {
		String ws = word;
		ws = ws.replaceAll("[0-9]+", "0");
		ws = ws.replaceAll("[a-z][a-z]+", "1");
		ws = ws.replaceAll("[a-z]", "2");
		ws = ws.replaceAll("[A-Z][A-Z]+", "3");
		ws = ws.replaceAll("[A-Z]", "4");
		ws = ws.replaceAll("[" + StringTools.lowerGreek + "]+", "5");
		return ws;
	}
}
