package uk.ac.cam.ch.wwmm.oscar3.resolver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Element;


/**Breaks a reaction name into stem, suffix, locant and other prefixes.
 * 
 * @author ptc24
 *
 */
final class ReactionNameParser {

	private static String beforeSuffix = "((?:[0-9OCNS]+[a-z]?'*,)*[0-9OCNS]+[a-z]?'*-)?" +
	"(de|re|non|di|tri|mono|poly)?-?" +
	"(.+?)";
	
	private static Pattern rnPattern = Pattern.compile(beforeSuffix + 
			"(" +
			"if(?:y(?:ing)?|ied|ication)|" +
			"is(?:e[ds]?|ing|ation)|" +
			"iz(?:e[ds]?|ing|ation)|" +
			"at(?:e[ds]?|ion|ing|ive(?:ly)?)|" +
			"ly(?:se[ds]?|sis|sing|tic(?:ly|ally)?)|" +
			"ed|" +
			"tion|" +
			"ing" +
	")");	
	
	private static Pattern asePattern = Pattern.compile(beforeSuffix + 
			"-?(" +
			"(?:" +
			"ly|transfer|mut|dismut|diester|ester|lig|peptid|at|" +
			"reduct|oxid|kin|epimer|synth|desatur|satur" +
	")?ases?)");
	
	public static void parseASE(NameResolver nres, Element chem) {
		String name = nres.getNEName(chem);
		Matcher m = asePattern.matcher(name);
		if(m.matches()) {
			String locants = m.group(1);
			String multiplier = m.group(2);
			String stem = m.group(3);
			String suffix = m.group(4);
			if(locants != null) nres.setNEAttribute(chem, "aseLocants", locants);
			if(multiplier != null) nres.setNEAttribute(chem, "aseMultiplier", multiplier);
			if(stem != null) nres.setNEAttribute(chem, "aseStem", stem);
			if(suffix != null) nres.setNEAttribute(chem, "aseSuffix", suffix);
		}
	}
	
	public static void parseRN(NameResolver nres, Element chem) {
		String qualifier = null;
		String name = nres.getNEName(chem);
		String [] words = name.split("\\s+");
		if(words.length > 1) {
			name = words[words.length-1];
			StringBuffer preName = new StringBuffer();
			for(int i=0;i<words.length-1;i++) {
				if(i > 0) preName.append(" ");
				preName.append(words[i]);
			}
			qualifier = preName.toString();
			//StringTools.arrayToString()
		}
		if(qualifier != null) { 
			nres.setNEAttribute(chem, "rnQualifier", qualifier);
		} else if("reaction".equals(name)) {
			nres.setNEAttribute(chem, "rnType", name);
		} else if("addition".equals(name)) { 
			nres.setNEAttribute(chem, "rnType", name);
		} else if("elimination".equals(name)) { 
			nres.setNEAttribute(chem, "rnType", name);
		} else if("substitution".equals(name)) { 
			nres.setNEAttribute(chem, "rnType", name);
		} else if("cyclisation".equals(name)) { 
			nres.setNEAttribute(chem, "rnType", name);
		} else if("rearrangement".equals(name)) {
			nres.setNEAttribute(chem, "rnType", name);
		} else if("condensation".equals(name)) {
			nres.setNEAttribute(chem, "rnType", name);
		} else if("coupling".equals(name)) {
			nres.setNEAttribute(chem, "rnType", name);
		} else if("metathesis".equals(name)) {
			nres.setNEAttribute(chem, "rnType", name);
		} else if("synthesis".equals(name)) {
			nres.setNEAttribute(chem, "rnType", name);
		} else if("oxidation".equals(name)) {
			nres.setNEAttribute(chem, "rnType", name);
		} else if("polymerization".equals(name)) {
			nres.setNEAttribute(chem, "rnType", name);
		} else {
			Matcher m = rnPattern.matcher(name);
			if(m.matches()) {
				String locants = m.group(1);
				String multiplier = m.group(2);
				String stem = m.group(3);
				String suffix = m.group(4);
				if(locants != null) nres.setNEAttribute(chem, "rnLocants", locants);
				if(multiplier != null) nres.setNEAttribute(chem, "rnMultiplier", multiplier);
				if(stem != null) nres.setNEAttribute(chem, "rnStem", stem);
				if(suffix != null) nres.setNEAttribute(chem, "rnSuffix", suffix);
			}			
		}
	}

}
