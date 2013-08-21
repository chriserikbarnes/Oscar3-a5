package uk.ac.aber.art_tool;


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Splits a SciXML file into sentences, respecting the XML tags.
 * 
 * @author Maria Liakata (mal)
 * @author Claire Q (cim)
 *
 */

public class SSSplit {
	private static final int sapientVersion = 1258;
	public static String sentenceExtraction(String fileString, String name) throws Exception {
       
        
        //to be appended later
        String replacementSCIstart = "<PAPER>";
        String replacementSCIend = "</BODY></PAPER>";
        
        //to be removed                
        String badWhiteSpace = "(\t|\r|\n|\\s)";
        String replacement = "<\\?jarpath /\\?>|<\\?host null:8181\\?>|<\\?viewer picture\\?>";
        fileString = fileString.replaceAll(replacement, "");
        fileString = fileString.replaceAll(badWhiteSpace, "sapientPOO");
        fileString = fileString.replaceAll("(sapientPOO)+", " ");
        String allowedAttrChars = "[\\w-_\2013\\.\\(\\)\\[\\]]";
        fileString = fileString.replaceAll(">\\s<", "><");
       
        //fileString = fileString.replaceAll("\n", "");
        //System.out.print(fileString);
        //defining the section of the paper to be split into sentences
        Pattern toBeSplit = Pattern.compile("<ABSTRACT>.+(?=</BODY>)");
        Matcher tbs = toBeSplit.matcher(fileString);
        Pattern currentTitle = Pattern.compile("<CURRENT_TITLE>(.+?)</CURRENT_TITLE>");
        Matcher cu = currentTitle.matcher(fileString);
        Pattern title=Pattern.compile("<TITLE>(.+?)</TITLE>");
        Matcher ti = title.matcher(fileString);
        String clearString = "";
        if (tbs.find()) {
                System.out.println("found a match");
                clearString = tbs.group();
        }
        //TODO: removing figure tags. Is this necessary now?
        clearString = clearString.replaceAll("<FIGURE/>", "");
        //String refSCIgeneral = "(?:(?i)(?:(?:\\d+[,\u2013])*\\d+)?(?:</IT>)?<REF(?:\\sTYPE=\"\\w+?\")?(?:\\stext=\"(?:refs?\\.(?:\\s)?)?(?:(?:\\d+[,\u2013])*\\d+)?\\w*?\")?(?:\\sID=\"(?:\\w-)?\\w+?(?:\\s\\w+?)*\")(?:\\sREFID=\"\\w+?\")?(?:/>|>(?:(?:refs?\\.(?:\\s)?)?(?:(?:\\d+[,-\u2013])*\\d+)?\\w*?</REF>))(?-i))";
        //works but not fully: String refSCIgeneral = "(?:(?i)(?:(?:\\d+[,\u2013])*\\d+)?(?:</IT>)?<REF(?:\\sTYPE=\"\\w+?\")?(?:\\stext=\"(?:refs?\\.(?:\\s)?)?(?:(?:\\d+[,\u2013])*\\d+)?\\w*?\")?(?:\\sID=\"(?:\\w-)?\\w+?(?:\\s\\w+?)*\")(?:\\sREFID=\"\\w+?\")?(?:/>|>(?:(?:refs?\\.(?:\\s)?)?(?:(?:\\d+(?:(?:<IT>)?\\w?(?:</IT>)?)[,-\u2013])*\\d+)?(?:(?:<IT>)?\\w?(?:</IT>)?)*?</REF>))(?-i))";
        //hangs: String refSCIgeneral = "(?:(?i)(?:(?:\\d+[,\u2013])*\\d+)?(?:</IT>)?<REF(?:\\sTYPE=\"\\w+?\")?(?:\\stext=\"(?:refs?\\.(?:\\s)?)?(?:(?:\\d+[,\u2013\u002D])*\\d+)?\\w*?\")?(?:\\sID=\"(?:\\w-)?\\w+?(?:\\s\\w+?)*\")(?:\\sREFID=\"\\w+?\")?(?:/>|>(?:(?:refs?\\.(?:\\s)?)?(?:\\d+(?:\\d*(?:<IT>)?\\w?(?:</IT>)?[,-\u2013])*(\\d+|(?:\\d*(?:<IT>)?\\w(?:</IT>)?))?</REF>))(?-i))";
        String refSCIgeneral = "(?:(?i)(?:(?:\\d+[,\u2013])*\\d+)?(?:</IT>)?<REF(?:\\sTYPE=\"\\w+?\")?(?:\\stext=\"(?:refs?\\.(?:\\s)?)?(?:(?:\\d+[,\u2013\u002D])*\\d+)?\\w*?\")?(?:\\sID=\"(?:\\w-)?\\w+?(?:\\s\\w+?)*\")(?:\\sREFID=\"\\w+?\")?(?:/>|>(?:(?:refs?\\.(?:\\s)?)?(?:(?:(?:\\d+(?:<IT>)?\\w?(?:</IT>)?)|(?:(?:<IT>)?\\w(?:</IT>)?[,-\u2013]))*(?:(?:\\d+(?:<IT>)?\\w?(?:</IT>)?)|(?:(?:<IT>)?\\w?(?:</IT>)?)))?</REF>))(?-i))";
        //short but hangs : (?:\\d(?:\\d*(?:<IT>)?\\w?(?:</IT>)?[,-\u2013]?)*)?
        System.out.println("refSCIgeneral is:" + refSCIgeneral);
        String refFootnote= "<SUP TYPE=\"FOOTNOTE_MARKER\" ID=\"" + allowedAttrChars + "+?\"/>";
        //String atLeastOneRefSCI = "(" + refSCIgeneral + "+)";
        String atLeastOneRefSCI = "((?:" + refSCIgeneral + "+" + "\\)?)|" + refFootnote + "|\\))"; //there may be a bracket after the reference
        // <REF TYPE="P" (text="ref. 24a")? ID="cit24a">ref. 24<IT>a</IT></REF> <-- example SCIXML
        //String referenceSCI = "(<REF (?i)TYPE=\"\\w+?\"((\\stext=\"(?:(ref\\.(\\s)?)?(?:\\d+[,\u2013])*\\d+)?\")? ID=\"\\w+(\\s\\w+)*\"(?-i))?>(?:(ref\\.(\\s)?)?(?:\\d+[,\u2013])*\\d+)?</REF>)";
        String capturePunctuation = "(\\.|\\?|(?<!\\(\\w{1,15})\\!)";
        String abbreviations = "((?i)(\\(|\\[|\\s|>)(al|Am|Angew|approx|Biochim|Biophys|ca|cf|Chem|Co|conc|Dr|Ed|e\\.g|Engl|eq|eqns?|exp|Figs?|i\\.e|Inc|Int|Lett|Ltd|p|p\\.a|Phys|Prof|prot|refs?|Rev|sect|st|vs|(?-i)(?<!(?:</SB>|(?:\\d\\s?<IT>?)|<IT>))[A-Z]))";
        
        // moving all references inside the punctuation so that the sentence splitting is easier
        Pattern refSentence = Pattern.compile("(.*?)" +  capturePunctuation
                        + atLeastOneRefSCI);
        Pattern abbrevs = Pattern.compile(".*?(" + abbreviations + "(?:</IT>)?)" + "$");
        //System.out.println("resSentence is" + refSentence);
        Matcher refm = refSentence.matcher(clearString);
        StringBuffer swappedString = new StringBuffer();
        while (refm.find()) {
                String a = refm.group(1); //sentence
                String b = refm.group(2); //punctuation
                String c = refm.group(3); //reference
                System.out.printf("a: %s\n b: %s\n c: %s\n",a,b,c);
                Matcher abbrevm=abbrevs.matcher(a);
                //if (Pattern.matches("al</IT>?$",a)){
                if (abbrevm.find()){
                    String ab = abbrevm.group(1);
                	System.out.println("String" + " ends in an abbreviation: " + ab);
                	swappedString.append(a); // don't change order
                	swappedString.append(b);
                	swappedString.append(c);
                }else{
                swappedString.append(a); //sentence
                swappedString.append(c); //reference
                swappedString.append(b); //punctuation
               }
                
        }
        //System.out.println("got out of the while loop");
        // anything after the last swapped reference
        String endbit = clearString.substring(swappedString.length(),
                        clearString.length());
        swappedString.append(endbit);
        clearString = swappedString.toString();
        clearString = clearString.replaceAll(">\\.<", ">\\. <"); // 29/3/09 added 
        System.out.println("ClearString: " + clearString);

        
        String capitals = "[A-Z0-9]"; //caps and numbers may begin a sentence
        String punctuation = "(?:\\.|\\?|(?<!\\(\\w{1,15})\\!)"; // .?!          
        String optPunctuation = punctuation + "??";
        String endEquation = "</EQN>";
        String endPara = "(</P>|</ABSTRACT>)";
        String beginPara = "<P>";
        
        String optStartQuote = "['\"\u201C]?";// '"
        String optCloseQuote = "['\"\u201D]?"; // '"
        String optReferenceSCI = refSCIgeneral + "*";
        String beginFirstSentence = "(^<ABSTRACT>)";
        String openHeader = "(<HEADER(\\sHEADER_MARKER=\""+ allowedAttrChars + "+?\")?>|<HEADER/>)";
        String wholeHeader = "((<BODY>)?(<DIV(\\sDEPTH=\"\\d+\")?>)?(<HEADER(\\sHEADER_MARKER=\"" + allowedAttrChars + "+?\")?>.*?</HEADER>|<HEADER/>))";               
        String optOpenHeader = openHeader + "?";
        
        String eqn = "<EQN(\\sID=\"" + allowedAttrChars + "+?\")?(\\sTYPE=\"" + allowedAttrChars + "+?\")?>";
        String xref = "<XREF(\\sID=\"" + allowedAttrChars + "+?\")?(\\sTYPE=\"" + allowedAttrChars + "+?\")?>";
        //String manyStartTags = "(" + eqn + "|" + xref + "|<BODY>|<DIV(\\sDEPTH=\"\\d+\")?>|<P>|<B>|<IT>)*";
        String manyStartTags = "(" + eqn + "|" + xref + "|<BODY>|<DIV(\\sDEPTH=\"\\d+\")?>|<P>)*";
        //String atLeastOneStartTag = "(<IT>|" + eqn + "|" + xref + "|<BODY>|<DIV(\\sDEPTH=\"\\d+\")?>|<P>)+";
        String optEndTags = "(</XREF>|</BODY>|</DIV>|</P>|</ABSTRACT>|</HEADER>|<HEADER/>)?";
        String endTags = "(</IT>|</EQN>|</XREF>|</BODY>|</DIV>|</P>|</ABSTRACT>|</HEADER>|<HEADER/>)";
        String manyEndTags = endTags + "*";
        //String sentenceTerminator = "(?>" +endPara+ "|"+ endEquation +"|" + "(?<!(\\s|>)refs?)"+punctuation+")";
        String endParaOrEq = "(" + endPara + "|" + endEquation + ")\\s?";
        //String puncNoRef = "(?<!(\\s|>)refs?)"+punctuation + "\\s";
        //String abbreviations = "((?i)(\\(|\\s|>)(refs?|Figs?|vs|Prof|Dr|conc|e\\.g|i\\.e|prot|st|cf|exp|eqns?|Inc|Ltd|Co)(?-i))";
        String formatting="(<B>|<IT>|<SP>)";
        
        String puncNoAbbrv = "(?<!" + abbreviations + "(</IT>)?)"+ punctuation + "\\s"; //"(\\s|\\s?(?=" + formatting +"))"; //29/3/09 made the \s optional if followed by formatting
        //?> gives priority to para and equations so that they are treated first before resorting to simple punctuation
        //String sentenceTerminator = "(?>" + endParaOrEq + "|" + puncNoAbbrv + ")";
        String greekLetters = "[\u0370-\u03FF\u1F00-\u1FFF]";
        
        String sentenceCommencer = "(?>" + beginPara + "|" + "Fig(s)?\\." + "|" + capitals + "|" + formatting + "|" + "\\[|\\(|"+greekLetters+"|\u007C )";
        String equationCommencer = "(" + eqn + ".)";
        String commencer = "(" + sentenceCommencer + "|" + equationCommencer + ")";
        // For matching the beginning of the next sentence
        // ((space|(space?(starttag+|<HEADER>)))starttag*<HEADER>?\"?caps|para|fig)
        //String beginSentenceLookahead = "((\\s|(\\s?(" + atLeastOneStartTag
         //               + "|" + openHeader + ")))" + manyStartTags + optOpenHeader
         //               + optStartQuote + sentenceCommencer + ")";
        
        String noSpaceReqLookahead =  manyStartTags + optOpenHeader + optStartQuote + commencer;
        
        String nocapsParaLookAhead  = "(\\s?<P>)";
        
        String startSentence = manyStartTags + optStartQuote + commencer;
        
        // For matching the end of the previous sentence
        String sentenceFigLookbehind = "(?<=(?<!"+ abbreviations + punctuation + ")((" + endParaOrEq + ")|("+ puncNoAbbrv + ")|("
        + optPunctuation + optEndTags + endTags + "\\s?)))";
        
        //for matching the start of a sentence following a header
        String headerLookahead = "(?=(?:" + manyStartTags + optOpenHeader
                        + optStartQuote + commencer + "))";
        //(Headerstuff | ((normal sentence | firstsentence) sentenceContent, (punctuation|endEquation), optionalEndings, lookahead))
        
        
        
        Pattern sentence = Pattern.compile(
        		"(" + sentenceFigLookbehind + wholeHeader + headerLookahead + 
        			//lookbehind and start of a normal sentence, or a match for the first sentence (at the beginning of the abstract)
        			")|(((" + sentenceFigLookbehind + startSentence + 
        				")|" + beginFirstSentence + "|" + beginPara + ")" +
                    // The sentence content
        			"(.*?)(Fig(s)?\\..+?)*?" +
                    // punctuation that ends a sentence. Give prioriy to endEquation then puncNoRef
        			"(((?<!(" + endEquation + "\\s?|" + puncNoAbbrv + "\\s?|" + endPara + "\\s?))" 
        			    + "(?=(?:"  + nocapsParaLookAhead + ")))|" +
			   
			        "((?>" + endEquation + "\\s?|" + puncNoAbbrv + "\\s?|" + endPara + "\\s?)"
					+ optCloseQuote	+ optReferenceSCI + manyEndTags + "\\s?"
					+ "(?=(?:" + noSpaceReqLookahead + "|" + nocapsParaLookAhead + "|\\n|\\s*$)))))"
        			
        		
        							
                    // lookahead to beginning of next sentence
        			// end of line or end of whole string
        );
        
        System.out.println("Sentence Pattern: " + sentence + "\n\n");
        Matcher m = sentence.matcher(clearString);
        ArrayList<StringBuffer> sentences = new ArrayList<StringBuffer>();
        StringBuffer finalbuffer = new StringBuffer();
        int somethingFound = 0;
        // This bit splits the sentences
        while (m.find()) {
        		somethingFound = 1;
                sentences.add(new StringBuffer(m.group()));
                //System.out.println(m.group());
        }
        if(somethingFound == 0) {
        	System.out.println("No sentences matched");
        }
        Pattern refSentenceRev = Pattern.compile("(.*?)" + atLeastOneRefSCI
                        + capturePunctuation + "(\\s?(?:</P>)?)\\Z");
        System.out.println("reversal pattern: "+ refSentenceRev.toString());
        int count = 0;
        
        ArrayList<StringBuffer> newSentences = new ArrayList<StringBuffer>();
        // this adds the paper title to the ArrayList of sentences
       
        
        for (StringBuffer s : sentences) {
                System.out.println(count + ": "+  s + "\n");
                Matcher refmRev = refSentenceRev.matcher(s);
                // if sentence finishes with reference + punctuation, swap the two over
                if (refmRev.find()) {
                        String a = refmRev.group(1); //sentence
                        String b = refmRev.group(2); //reference or bracket
                        String c = refmRev.group(3); //punctuation
                        String d = refmRev.group(4); //space and/or </P>
                        System.out.printf("sentence "+count+": %s\n A: %s\n B: %s\n C: %s\n D: %s\n", s, a,b,c,d);
                        //System.out.printf("sentence "+count+": %s\n A: %s\n B: %s\n C: %s\n", s, a,b,c);
                        StringBuffer ns = new StringBuffer();
                        if (b.equals(")")) {
                        ns.append(a); //sentence
                        ns.append(b); //bracket
                        ns.append(c); //punctuation
                        ns.append(d); //space
                        } else{ 
                        	ns.append(a); //sentence
                            ns.append(c); //punctuation
                            ns.append(b); //reference
                            ns.append(d); //space
                        	
                        }
                        newSentences.add(ns);
                        
                        //System.out.println("Sentence found: " + count);
                        
                } else {
                	newSentences.add(s);
                }
                count++;
        }
        sentences = newSentences;
        // Post-processing sentence array to move XML tags outside the sentences
        // String ppSentenceTag = "(<s sid=\"\\d+\">)";
        String ppStartTags = "((?:<ABSTRACT>|<BODY>|<DIV(\\sDEPTH=\"\\d+\")?>|<P>)*)"; // added overall brackets and ?: 15/6/09 mal
        String ppSentence = "(.+?)";
        String ppEndTags = "((?:</ABSTRACT>|</BODY>|</DIV>|</P>)*\\s?)\\Z";
       
        Pattern pp = Pattern.compile(ppStartTags + ppSentence + ppEndTags);
        Pattern ppHeader = Pattern.compile(".*?<HEADER.+?");
        ArrayList<StringBuffer> nsentences = new ArrayList<StringBuffer>();
        if(cu.find()) {
        	//System.out.println("The title is: " + cu.group(0) + "end of title");
            nsentences.add(new StringBuffer("<TITLE>" + "<s sid=\"1\">" + cu.group(1) + "</s>" + "</TITLE>"));
        }
        else if (ti.find()){
        	//System.out.println("The title is: " + ti.group(0) + "end of title");
            nsentences.add(new StringBuffer("<TITLE>" + "<s sid=\"1\">" + ti.group(1) + "</s>" + "</TITLE>"));
        }
        
        int id = 2;
        
        for (StringBuffer s : sentences) {
                Matcher ppm = pp.matcher(s);
                if (ppm.matches()) {
                        //System.out.println("Sentence " + id+ " matches the post- processing tags");
                        String one = "";
                        if (ppm.group(1) != null)
                                one = ppm.group(1);
                        String two = ppm.group(3);
                        String three = "";
                        if (ppm.group(4) != null)
                                three = ppm.group(4);
                        //System.out.println("Sent id is: " + id+ " Group 1 is: " + ppm.group(1) + " Group 2 is: " + ppm.group(2) + "Group 3 is: " +ppm.group(3) + "Group 4 is: " + ppm.group(4));
                        
                        Matcher mHead = ppHeader.matcher(s);
                        if (!mHead.matches()) {
                                nsentences.add(new StringBuffer(one + "<s sid=\"" + id
                                                + "\">" + two + "</s>" + three));
                                //System.out.println("Sentence " + id + ": " + s);
                                id++;
                        } else {
                                // this is a header, mHead matches
                                nsentences.add(s);
                        }
                } else {
                        System.out.println("Sentence " + id + "-- " + s
                                        + " -- didn't match!");
                }
        }
        finalbuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        finalbuffer.append(replacementSCIstart);
        int countme = 0;
        for (StringBuffer s : nsentences) {
        		countme++;
                //System.out.println("Appending sentence " + countme + ":\n" + s);
                finalbuffer.append(s);
        }

        // fileString.replaceAll("<ABSTRACT>.+?</BODY>",
        // finalbuffer.toString());
        finalbuffer.append(replacementSCIend);
        fileString = finalbuffer.toString();
        //bit of a hack here to get open Ps back in
       // fileString = fileString.replaceAll("</P><s", "</P><P><s");
        Pattern xmlDeclaration = Pattern.compile("(<\\?xml.+?><PAPER>)(.*)\\Z");
        Matcher xmatch = xmlDeclaration.matcher(fileString);
        if (xmatch.find()) {
                String a = xmatch.group(1);
                String b = xmatch.group(2);
                a = a + "<mode2 name='" + name + "' hasDoc='yes'" + " version= '" + sapientVersion + "'/>";
                fileString = a + b;
        }
        // fileString.replace("(<\\?xml.+?><PAPER>)","")
        // System.out.println("finalbuffer" + finalbuffer);
        // System.out.println("fileString" + fileString);
        return fileString;
	}
}

