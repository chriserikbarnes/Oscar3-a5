package uk.ac.cam.ch.wwmm.oscar3.indexersearcher;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.StringSource;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Token;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequenceSource;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.Tokeniser;
import uk.ac.cam.ch.wwmm.oscar3.recogniser.document.TokenSequence;
import uk.ac.cam.ch.wwmm.ptclib.string.StringTools;

/**Experimental code to generate a concordance based on SciXML files and the index.
 * 
 * @author ptc24
 *
 */
public final class Concordance {

	private static String interleave(String left, String right, boolean compareLeft) {
		StringBuffer sb = new StringBuffer();
		left = left.toLowerCase();
		right = right.toLowerCase();
		if(left.length() > right.length()) {
			right += StringTools.multiplyString(" ", left.length() - right.length());
		} else if(right.length() > left.length()) {
			left = StringTools.multiplyString(" ", right.length() - left.length()) + left;
		}
		for(int i=0;i<left.length();i++) {
			if(compareLeft) {
				sb.append(left.substring(left.length()-i-1,left.length()-i));
				sb.append(right.substring(i,i+1));
			} else {
				sb.append(right.substring(i,i+1));
				sb.append(left.substring(left.length()-i-1,left.length()-i));				
			}
		}
		return sb.toString();
	}

	/**Generates a KWIC concordance. The available modes control how the entries
	 * are sorted:<ul>
	 * <li>"left": sort by mirror image of the left side</li>
	 * <li>"right": sort by right hand side</li>
	 * <li>"interleave": sort on a character from the left, then
	 * from the right, then from the left, then...</li>
	 * <li>"random": leave unsorted</li>
	 * </ul>
	 * 
	 * @param files The files from which the concordance should be made.
	 * @param word The key word.
	 * @param width The width of the concordance, in characters.
	 * @param mode The mode.
	 * @return The concordance, as a list of entries.
	 */
	public static List<ConcordanceEntry> makeConcordance(Collection<File> files, String word, int width, String mode) {
		final Map<ConcordanceEntry,String> concordance = new HashMap<ConcordanceEntry,String>();
		
		int allowableWidth = width - word.length() - 2;
		int beforeWidth = allowableWidth / 2;
		int afterWidth = allowableWidth / 2;
		/*if("left".equals(mode)) {
			beforeWidth = (2 * allowableWidth) / 3;
			afterWidth = allowableWidth / 3;
		} else if("right".equals(mode)) {
			beforeWidth = allowableWidth / 3;
			afterWidth = (2 * allowableWidth) / 3;			
		}*/
		
		//StringSource ss = new StringSource(files, false);
		TokenSequenceSource ts = new TokenSequenceSource(files);
		boolean doSort = true;
		for(TokenSequence t : ts) {
			//s = s.replaceAll("\\s+", " ");
			//Tokeniser t = new Tokeniser(null);
			//t.tokenise(s);
			int sstart = t.getOffset();
			int send = t.getSourceString().length() + sstart;
			for(Token token : t.getTokens()) {
				if(token.getValue().equalsIgnoreCase(word)) {
					int wstart = token.getStart();
					int wend = token.getEnd();
					String before = t.getStringAtOffsets(Math.max(sstart, wstart-beforeWidth), wstart);
					before = before.replaceAll("\\s+", " ");
					if(before.startsWith(" ")) before = "." + before.substring(1);
					if(before.length() < beforeWidth) {
						before = StringTools.multiplyString(".", beforeWidth - before.length()) + before;
					}
					//if(wstart < beforeWidth) {
					//	before = StringTools.multiplyString(" ", beforeWidth - wstart) + t.getStringAtOffsets(0, wstart);
					//} else {
					//	before = t.getStringAtOffsets(wstart - beforeWidth, wstart);
					//}
					String after = t.getStringAtOffsets(wend, Math.min(send, wend + afterWidth));
					after = after.replaceAll("\\s+", " ");
					//if(s.length() - wend > afterWidth) {
					//	after = s.substring(wend, wend + afterWidth);
					//} else {
					//	after = s.substring(wend);
					//}
					String display = before + "  " + token.getValue() + "  " + after;
					String sort = "";
					if("interleave".equals(mode)) {
						sort = interleave(before, after, true);						
					} else if("left".equals(mode)) {
						StringBuffer sb = new StringBuffer(before.toLowerCase());
						sb.reverse();
						sort = sb + "\n" + after.toLowerCase();
					} else if("right".equals(mode)) {
						sort = after.toLowerCase() + "\n" + before.toLowerCase();						
					} else {
						doSort = false;
					}
					ConcordanceEntry entry = new ConcordanceEntry();
					entry.text = display;
					entry.start = token.getStartXPoint();
					entry.end = token.getEndXPoint();
					entry.file = ts.getCurrentFile().getAbsolutePath();
					concordance.put(entry, sort);
				}
			}
		}
		if(!doSort) {
			return new ArrayList<ConcordanceEntry>(concordance.keySet());
		}
		List<ConcordanceEntry> entries = new ArrayList<ConcordanceEntry>(concordance.keySet());
		Collections.sort(entries, Collections.reverseOrder(new Comparator<ConcordanceEntry>() {
			public int compare(ConcordanceEntry o1, ConcordanceEntry o2) {
				// TODO Auto-generated method stub
				return concordance.get(o1).compareTo(concordance.get(o2));
			}
		}));
		return entries;
	}

	/**Generates a concordance with two key words. The available modes control
	 * how the entries are sorted, where the left is the string before the first
	 * key word, the middle is between the key words, and the right is after
	 * the second key word:<ul>
	 * <li>"leftmost": sort by the mirror image of the left, then the middle,
	 * then the right</li>
	 * <li>"left": sort by the middle, then the mirror image of the left,
	 * then the right</li>
	 * <li>"right": sort by the mirror image of the middle, then the right, then
	 * the mirror image of the left</li>
	 * <li>"rightmost": sort by the right, then the mirror image of the middle,
	 * then the mirror image of the left</li>
	 * <li>"interleave": sort my the middle, then a character from the left, then
	 * from the right, then from the left, then...</li>
	 * <li>"random": leave unsorted</li>
	 * </ul>
	 * 
	 * @param files The files from which the concordance should be made.
	 * @param word1 The key word on the left.
	 * @param word2 The key word on the right.
	 * @param width The width of the concordance, in characters.
	 * @param mode The mode.
	 * @return The concordance, as a list of entries.
	 */
	public static List<String> biConcordance(Collection<File> files, String word1, String word2, int width, String mode) {
		if(mode == null) mode = "random";
		
		Map<String,String> concordance = new HashMap<String,String>();
		
		int allowableWidth = width - word1.length() - word2.length() - 4;
		int beforeWidth = allowableWidth / 3;
		int afterWidth = allowableWidth / 3;
		int middleWidth = allowableWidth / 3;
		
		StringSource ss = new StringSource(files, false);
		boolean doSort = true;
		for(String s : ss) {
			s = s.replaceAll("\\s+", " ");
			TokenSequence t = Tokeniser.getInstance().tokenise(s);
			for(int i=0;i<t.size();i++) {
				Token token = t.getToken(i);
				if(token.getValue().equalsIgnoreCase(word1)) {
					for(int j=i+1;j<t.size() && j<i+10;j++) {
						Token token2 = t.getToken(j);
						if(token2.getValue().equalsIgnoreCase(word2)) {
							int wstart1 = token.getStart();
							int wend1 = token.getEnd();
							
							int wstart2 = token2.getStart();
							int wend2 = token2.getEnd();
							
							String before = null;
							if(wstart1 < beforeWidth) {
								before = StringTools.multiplyString(" ", beforeWidth - wstart1) + s.substring(0, wstart1);
							} else {
								before = s.substring(wstart1 - beforeWidth, wstart1);
							}
							
							String after = null;
							if(s.length() - wend2 > afterWidth) {
								after = s.substring(wend2, wend2 + afterWidth);
							} else {
								after = s.substring(wend2);
							}
							
							String middle = s.substring(wend1, wstart2);
							String middleDisplay = middle;
							if(middle.length() > middleWidth) continue;
							if(middle.length() < middleWidth) {
								if(mode.startsWith("left")) {
									middleDisplay += StringTools.multiplyString(" ", middleWidth - middle.length());																		
								} else if(mode.startsWith("right")) {
									middleDisplay = StringTools.multiplyString(" ", middleWidth - middle.length()) + middleDisplay;									
								} else {
									int toPad = middleWidth - middle.length();
									int left = toPad / 2;
									int right = toPad - left;
									middleDisplay = StringTools.multiplyString(" ", left) +
										middleDisplay + StringTools.multiplyString(" ", right);
								}
							}
							
							String display = before + "  " + token.getValue() + "  " + middleDisplay + " " + token2.getValue() + " " + after;
							String sort = "";
							if("interleave".equals(mode)) {
								sort = middleDisplay.toLowerCase() + interleave(before, after, true);						
							} else if("left".equals(mode)) {
								StringBuffer sb = new StringBuffer(before.toLowerCase());
								sb.reverse();
								sort = middle.toLowerCase() + "\n" + sb + "\n" + after.toLowerCase();
							} else if("right".equals(mode)) {
								StringBuffer sb = new StringBuffer(middle.toLowerCase());
								sb.reverse();
								sort = sb + "\n" + after.toLowerCase() + "\n" + before.toLowerCase();						
							} else if("leftmost".equals(mode)) {
								StringBuffer sb = new StringBuffer(before.toLowerCase());
								sb.reverse();
								sort = sb + "\n" + middle.toLowerCase() + "\n" + after.toLowerCase();
							} else if("rightmost".equals(mode)) {
								StringBuffer sb = new StringBuffer(middle.toLowerCase());
								sb.reverse();
								sort = after.toLowerCase() + "\n" + sb + "\n" + before.toLowerCase();						
							} else {
								doSort = false;
							}
							concordance.put(display, sort);
							
						}
					}
				}
			}
		}
		if(!doSort) {
			return new ArrayList<String>(concordance.keySet());
		}
		return StringTools.getSortedList(concordance);
	}
	
}
