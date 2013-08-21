package uk.ac.cam.ch.wwmm.oscar3.recogniser.tokenanalysis;

import java.util.regex.Pattern;

import uk.ac.cam.ch.wwmm.oscar3.misc.NETypes;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.ptclib.scixml.XMLStrings;

public class TokenTypes {
	
	public static Pattern twoLowerPattern = Pattern.compile("[a-z][a-z]");
	public static Pattern oneCapitalPattern = Pattern.compile("[A-Z]");
	public static Pattern oxidationStatePattern = Pattern.compile("\\((o|i{1,4}|i{0,3}[xv]|[xv]i{0,4})\\)", Pattern.CASE_INSENSITIVE);
	public static Pattern oxidationStateEndPattern = Pattern.compile(".*\\((o|i{1,4}|i{0,3}[xv]|[xv]i{0,4})\\)", Pattern.CASE_INSENSITIVE);

	public static boolean isCompRef(Token token) {
		boolean isCr = false;
		if(token.getDoc() != null && XMLStrings.getInstance().isCompoundReferenceUnderStyle(token.getDoc().getStandoffTable().getElemAtOffset(token.getStart()))) {
			isCr = true;
			for(int i=0;i<token.getValue().length();i++) {
				if(!XMLStrings.getInstance().isCompoundReferenceUnderStyle(token.getDoc().getStandoffTable().getElemAtOffset(token.getStart()))) {
					isCr = false;
					break;
				}
			}
		}
		return isCr;
	}
	
	public static boolean isRef(Token token) {
		if(token.getDoc() == null) return false;
		return XMLStrings.getInstance().isCitationReferenceUnderStyle(token.getDoc().getStandoffTable().getElemAtOffset(token.getStart()));
	}

	public static String getTypeForSuffix(String s) {
		if(s.matches(".*nucleobase")) return NETypes.COMPOUND;
		if(s.matches(".*nucleobases")) return NETypes.COMPOUNDS;
		if(s.matches(".*ases?")) return NETypes.ASE;
		//if(value.matches(".*ases")) return NETypes.ASES;
		if(s.matches(".*arsenic")) return NETypes.COMPOUND;
		if(s.matches(".*arsenics")) return NETypes.COMPOUNDS;
		if(s.matches(".*(ic)")) return NETypes.ADJECTIVE;
		if(s.matches(".*(biphenyl)")) return NETypes.COMPOUND;
		if(s.matches(".*(yl|o|oxy)\\)?-?")) return NETypes.GROUP;
		if(s.matches(".*at(ed|ions?|ing)")) return NETypes.REACTION;
		if(s.matches(".*i[sz](ed|ations?|ing)")) return NETypes.REACTION;
		if(s.matches(".*(lys(is|es|ed?|ing|tic))")) return NETypes.REACTION;
		if(s.matches(".*s")) return NETypes.COMPOUNDS;
		return NETypes.COMPOUND;
	}

}
