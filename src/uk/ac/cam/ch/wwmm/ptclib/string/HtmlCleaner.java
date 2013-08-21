package uk.ac.cam.ch.wwmm.ptclib.string;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts HTML to plain text. Does so by stripping tags (and converting some to
 * whitespace), and turning entities into Unicode.
 * 
 * @author ptc24
 *
 */
public final class HtmlCleaner {
	
	private static HtmlCleaner myInstance = new HtmlCleaner();
	private HashMap<String, Integer> entDict;
	
	private HtmlCleaner() {
		entDict = new HashMap<String, Integer>();
		entDict.put("", 55);
		entDict.put("nbsp", 160);
		entDict.put("iexcl", 161);
		entDict.put("cent", 162);
		entDict.put("pound", 163);
		entDict.put("curren", 164);
		entDict.put("yen", 165);
		entDict.put("brvbar", 166);
		entDict.put("sect", 167);
		entDict.put("uml", 168);
		entDict.put("copy", 169);
		entDict.put("ordf", 170);
		entDict.put("laquo", 171);
		entDict.put("not", 172);
		entDict.put("shy", 173);
		entDict.put("reg", 174);
		entDict.put("macr", 175);
		entDict.put("deg", 176);
		entDict.put("plusmn", 177);
		entDict.put("sup2", 178);
		entDict.put("sup3", 179);
		entDict.put("acute", 180);
		entDict.put("micro", 181);
		entDict.put("para", 182);
		entDict.put("middot", 183);
		entDict.put("cedil", 184);
		entDict.put("sup1", 185);
		entDict.put("ordm", 186);
		entDict.put("raquo", 187);
		entDict.put("frac14", 188);
		entDict.put("frac12", 189);
		entDict.put("frac34", 190);
		entDict.put("iquest", 191);
		entDict.put("Agrave", 192);
		entDict.put("Aacute", 193);
		entDict.put("Acirc", 194);
		entDict.put("Atilde", 195);
		entDict.put("Auml", 196);
		entDict.put("Aring", 197);
		entDict.put("AElig", 198);
		entDict.put("Ccedil", 199);
		entDict.put("Egrave", 200);
		entDict.put("Eacute", 201);
		entDict.put("Ecirc", 202);
		entDict.put("Euml", 203);
		entDict.put("Igrave", 204);
		entDict.put("Iacute", 205);
		entDict.put("Icirc", 206);
		entDict.put("Iuml", 207);
		entDict.put("ETH", 208);
		entDict.put("Ntilde", 209);
		entDict.put("Ograve", 210);
		entDict.put("Oacute", 211);
		entDict.put("Ocirc", 212);
		entDict.put("Otilde", 213);
		entDict.put("Ouml", 214);
		entDict.put("times", 215);
		entDict.put("Oslash", 216);
		entDict.put("Ugrave", 217);
		entDict.put("Uacute", 218);
		entDict.put("Ucirc", 219);
		entDict.put("Uuml", 220);
		entDict.put("Yacute", 221);
		entDict.put("THORN", 222);
		entDict.put("szlig", 223);
		entDict.put("agrave", 224);
		entDict.put("aacute", 225);
		entDict.put("acirc", 226);
		entDict.put("atilde", 227);
		entDict.put("auml", 228);
		entDict.put("aring", 229);
		entDict.put("aelig", 230);
		entDict.put("ccedil", 231);
		entDict.put("egrave", 232);
		entDict.put("eacute", 233);
		entDict.put("ecirc", 234);
		entDict.put("euml", 235);
		entDict.put("igrave", 236);
		entDict.put("iacute", 237);
		entDict.put("icirc", 238);
		entDict.put("iuml", 239);
		entDict.put("eth", 240);
		entDict.put("ntilde", 241);
		entDict.put("ograve", 242);
		entDict.put("oacute", 243);
		entDict.put("ocirc", 244);
		entDict.put("otilde", 245);
		entDict.put("ouml", 246);
		entDict.put("divide", 247);
		entDict.put("oslash", 248);
		entDict.put("ugrave", 249);
		entDict.put("uacute", 250);
		entDict.put("ucirc", 251);
		entDict.put("uuml", 252);
		entDict.put("yacute", 253);
		entDict.put("thorn", 254);
		entDict.put("yuml", 255);
		entDict.put("fnof", 402);
		entDict.put("Alpha", 913);
		entDict.put("Beta", 914);
		entDict.put("Gamma", 915);
		entDict.put("Delta", 916);
		entDict.put("Epsilon", 917);
		entDict.put("Zeta", 918);
		entDict.put("Eta", 919);
		entDict.put("Theta", 920);
		entDict.put("Iota", 921);
		entDict.put("Kappa", 922);
		entDict.put("Lambda", 923);
		entDict.put("Mu", 924);
		entDict.put("Nu", 925);
		entDict.put("Xi", 926);
		entDict.put("Omicron", 927);
		entDict.put("Pi", 928);
		entDict.put("Rho", 929);
		entDict.put("Sigma", 931);
		entDict.put("Tau", 932);
		entDict.put("Upsilon", 933);
		entDict.put("Phi", 934);
		entDict.put("Chi", 935);
		entDict.put("Psi", 936);
		entDict.put("Omega", 937);
		entDict.put("alpha", 945);
		entDict.put("beta", 946);
		entDict.put("gamma", 947);
		entDict.put("delta", 948);
		entDict.put("epsilon", 949);
		entDict.put("zeta", 950);
		entDict.put("eta", 951);
		entDict.put("theta", 952);
		entDict.put("iota", 953);
		entDict.put("kappa", 954);
		entDict.put("lambda", 955);
		entDict.put("mu", 956);
		entDict.put("nu", 957);
		entDict.put("xi", 958);
		entDict.put("omicron", 959);
		entDict.put("pi", 960);
		entDict.put("rho", 961);
		entDict.put("sigmaf", 962);
		entDict.put("sigma", 963);
		entDict.put("tau", 964);
		entDict.put("upsilon", 965);
		entDict.put("phi", 966);
		entDict.put("chi", 967);
		entDict.put("psi", 968);
		entDict.put("omega", 969);
		entDict.put("thetasym", 977);
		entDict.put("upsih", 978);
		entDict.put("piv", 982);
		entDict.put("bull", 8226);
		entDict.put("hellip", 8230);
		entDict.put("prime", 8242);
		entDict.put("Prime", 8243);
		entDict.put("oline", 8254);
		entDict.put("frasl", 8260);
		entDict.put("weierp", 8472);
		entDict.put("image", 8465);
		entDict.put("real", 8476);
		entDict.put("trade", 8482);
		entDict.put("alefsym", 8501);
		entDict.put("larr", 8592);
		entDict.put("uarr", 8593);
		entDict.put("rarr", 8594);
		entDict.put("darr", 8595);
		entDict.put("harr", 8596);
		entDict.put("crarr", 8629);
		entDict.put("lArr", 8656);
		entDict.put("uArr", 8657);
		entDict.put("rArr", 8658);
		entDict.put("dArr", 8659);
		entDict.put("hArr", 8660);
		entDict.put("forall", 8704);
		entDict.put("part", 8706);
		entDict.put("exist", 8707);
		entDict.put("empty", 8709);
		entDict.put("nabla", 8711);
		entDict.put("isin", 8712);
		entDict.put("notin", 8713);
		entDict.put("ni", 8715);
		entDict.put("prod", 8719);
		entDict.put("sum", 8721);
		entDict.put("minus", 8722);
		entDict.put("lowast", 8727);
		entDict.put("radic", 8730);
		entDict.put("prop", 8733);
		entDict.put("infin", 8734);
		entDict.put("ang", 8736);
		entDict.put("and", 8743);
		entDict.put("or", 8744);
		entDict.put("cap", 8745);
		entDict.put("cup", 8746);
		entDict.put("int", 8747);
		entDict.put("there4", 8756);
		entDict.put("sim", 8764);
		entDict.put("cong", 8773);
		entDict.put("asymp", 8776);
		entDict.put("ne", 8800);
		entDict.put("equiv", 8801);
		entDict.put("le", 8804);
		entDict.put("ge", 8805);
		entDict.put("sub", 8834);
		entDict.put("sup", 8835);
		entDict.put("nsub", 8836);
		entDict.put("sube", 8838);
		entDict.put("supe", 8839);
		entDict.put("oplus", 8853);
		entDict.put("otimes", 8855);
		entDict.put("perp", 8869);
		entDict.put("sdot", 8901);
		entDict.put("lceil", 8968);
		entDict.put("rceil", 8969);
		entDict.put("lfloor", 8970);
		entDict.put("rfloor", 8971);
		entDict.put("lang", 9001);
		entDict.put("rang", 9002);
		entDict.put("loz", 9674);
		entDict.put("spades", 9824);
		entDict.put("clubs", 9827);
		entDict.put("hearts", 9829);
		entDict.put("diams", 9830);
		entDict.put("quot", 34);
		entDict.put("amp", 38);
		entDict.put("lt", 60);
		entDict.put("gt", 62);
		entDict.put("OElig", 338);
		entDict.put("oelig", 339);
		entDict.put("Scaron", 352);
		entDict.put("scaron", 353);
		entDict.put("Yuml", 376);
		entDict.put("circ", 710);
		entDict.put("tilde", 732);
		entDict.put("ensp", 8194);
		entDict.put("emsp", 8195);
		entDict.put("thinsp", 8201);
		entDict.put("zwnj", 8204);
		entDict.put("zwj", 8205);
		entDict.put("lrm", 8206);
		entDict.put("rlm", 8207);
		entDict.put("ndash", 8211);
		entDict.put("mdash", 8212);
		entDict.put("lsquo", 8216);
		entDict.put("rsquo", 8217);
		entDict.put("sbquo", 8218);
		entDict.put("ldquo", 8220);
		entDict.put("rdquo", 8221);
		entDict.put("bdquo", 8222);
		entDict.put("dagger", 8224);
		entDict.put("Dagger", 8225);
		entDict.put("permil", 8240);
		entDict.put("lsaquo", 8249);
		entDict.put("rsaquo", 8250);
		entDict.put("euro", 8364);
	}
	
	private String replaceEntities(String HTML) {
		Pattern p = Pattern.compile("&.*?;");
		Matcher m = p.matcher(HTML);
		StringBuffer sb = new StringBuffer();
		int prevEnd = 0;
		while(m.find()) {
			sb.append(HTML.substring(prevEnd, m.start()));
			sb.append(replaceEntity(m.group()));
			prevEnd = m.end();
		}
		sb.append(HTML.substring(prevEnd));
		return sb.toString();
	}
	
	private static String codePointToString(int codePoint) {
		//System.out.println(codePoint);
		int [] cpa = { codePoint };
		return new String(cpa, 0, 1);
	}
	
	private String replaceEntity(String entity) {
		if(entity.startsWith("&#x")) {
			/* Hexadecimal */
			int i = Integer.parseInt(entity.substring(3,entity.length()-1), 16);
			return codePointToString(i);
		} else if(entity.startsWith("&#")) {
			int i = Integer.parseInt(entity.substring(2,entity.length()-1));
			return codePointToString(i);			
		} else {
			String key = entity.substring(1,entity.length()-1);
			if(entDict.containsKey(key)) {
				int i = entDict.get(key);
				return codePointToString(i);
			} else {
				return entity;
			}
		}
	}
	
	private static String normaliseWhitespace(String HTML) {
		return HTML.replaceAll("\\s+", " ");
	}
	
	private static String reNormaliseWhitespace(String HTML) {
		Pattern p = Pattern.compile("\\s+");
		Matcher m = p.matcher(HTML);
		StringBuffer sb = new StringBuffer();
		int prevEnd = 0;
		while(m.find()) {
			sb.append(HTML.substring(prevEnd, m.start()));
			if(m.group().matches("\\s*\n\\s*")) {
				sb.append("\n\n");
			} else {
				sb.append(" ");
			}
			prevEnd = m.end();
		}
		sb.append(HTML.substring(prevEnd));
		return sb.toString();
	}
	
	/**Converts HTML to plain text, by throwing out tags, resolving entities,
	 * and adjusting the whitespace.
	 * 
	 * @param html The source HTML.
	 * @return The resulting plain text string.
	 */
	public static String cleanHTML(String html) {
		if(html.startsWith("<body><pre>") && html.endsWith("</pre></body></html>")) {
			Matcher m = Pattern.compile("(is?)<pre>(.*)</pre>").matcher(html);
			m.find();
			return m.group(1);
		}
		
		Pattern p = Pattern.compile("<body.*?>(.*?)</body>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(html);
		if(m.find()) html = m.group(1);
		html = normaliseWhitespace(html);

		html = html.replaceAll("(?is)</?h\\d.*?>", "\n");
		html = html.replaceAll("(?is)<br.*?>", "\n");
		html = html.replaceAll("(?is)</?p.*?>", "\n");
		html = html.replaceAll("(?is)</?div.*?>", "\n");
		html = html.replaceAll("(?is)</?tr.*?>", "\n");
		html = html.replaceAll("(?is)</?li.*?>", "\n");

		html = html.replaceAll("(?is)</?td.*?>", " ");
		html = html.replaceAll("(?is)</?th.*?>", " ");

		html = html.replaceAll("(?is)<style.*?>.*?</style.*?>", "");
		html = html.replaceAll("(?is)<script.*?>.*?</script.*?>", "");

		html = html.replaceAll("(?is)<.*?>", "");
		html = myInstance.replaceEntities(html);
		html = reNormaliseWhitespace(html);
		return html;
	}
	
}
