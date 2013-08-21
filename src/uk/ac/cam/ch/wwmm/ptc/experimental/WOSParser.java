package uk.ac.cam.ch.wwmm.ptc.experimental;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

public class WOSParser {

	public static Element makeRecord(String title, String abstr, List<String> authors,
			String journal, String fpage, String lpage, int vol, int year, String issue, String fileno) {
		Element elem = new Element("PAPER");
		Element metadata = new Element("METADATA");
		elem.appendChild(metadata);
		
		if(!fileno.equals("")) {
			Element fileNoElem = new Element("FILENO");
			fileNoElem.appendChild(fileno);
			metadata.appendChild(fileNoElem);
		}
		
		Element journalElem = new Element("JOURNAL");
		metadata.appendChild(journalElem);
		
		Element journalNameElem = new Element("NAME");
		journalNameElem.appendChild(journal);
		journalElem.appendChild(journalNameElem);
		
		if(year != -1) {
			Element journalYearElem = new Element("YEAR");
			journalYearElem.appendChild(year + "");
			journalElem.appendChild(journalYearElem);			
		}
		if(vol != -1) {
			Element journalVolElem = new Element("VOLUME");
			journalVolElem.appendChild(vol + "");
			journalElem.appendChild(journalVolElem);			
		}
		if(!issue.equals("")) {
			Element journalIssueElem = new Element("ISSUE");
			journalIssueElem.appendChild(issue);
			journalElem.appendChild(journalIssueElem);			
		}
		if(!"".equals(fpage)) {
			Element journalPagesElem = new Element("PAGES");
			if(!"".equals(lpage)) {
				journalPagesElem.appendChild(fpage + "-" + lpage);
 			} else {
				journalPagesElem.appendChild(fpage + ""); 				
 			}
			journalElem.appendChild(journalPagesElem);			
		}
		if(authors.size() > 0) {
			Element authorsElem = new Element("CURRENT_AUTHORLIST");
			elem.appendChild(authorsElem);
			for(String author : authors) {
				Element authorElem = new Element("CURRENT_AUTHOR");
				Element nameElem = new Element("NAME");
				nameElem.appendChild(author);
				authorElem.appendChild(nameElem);
				authorsElem.appendChild(authorElem);
			}
		}
		Element titleElem = new Element("CURRENT_TITLE");
		titleElem.appendChild(title);
		elem.appendChild(titleElem);
		if(!abstr.equals("")) {
			Element abstractElem = new Element("ABSTRACT");
			abstractElem.appendChild(abstr);
			elem.appendChild(abstractElem);
		}

		
		return elem;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		File outDir = new File(new File(Oscar3Props.getInstance().workspace, "corpora"), "dcc");
		if(!outDir.exists()) outDir.mkdir();
		
		File f = new File("/home/ptc24/Desktop/savedrecs.txt");
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line = br.readLine();
		String currentField = "";
		String abstr = "";
		String title = "";
		String journal = "";
		int year = -1;
		String fpage = "";
		String lpage = "";
		int vol = -1;
		String issue = "";
		String pt = "";
		String issn = "";
		String ut = "";
		List<String> authorsFull = new ArrayList<String>();
		List<String> authorsShort = new ArrayList<String>();
		
		Set<String> issns = new HashSet<String>();
		
		while(line != null) {
			//System.out.println(line);
			if(line.length() > 2) {
				String prefix = line.substring(0,2);
				String afterPrefix = line.substring(3).trim();
				if(!prefix.equals("  ")) currentField = prefix;
				if(prefix.equals("TI")) {
					title = afterPrefix;
				} else if(prefix.equals("AB")) {
					abstr = afterPrefix; 
				} else if(prefix.equals("SO")) {
					journal = afterPrefix; 
				} else if(prefix.equals("AF")) {
					authorsFull.add(afterPrefix); 
				} else if(prefix.equals("AU")) {
					authorsShort.add(afterPrefix); 
				} else if(prefix.equals("PY")) {
					year = Integer.parseInt(afterPrefix); 
				} else if(prefix.equals("BP")) {
					//System.out.println(afterPrefix);
					fpage = afterPrefix; 
				} else if(prefix.equals("EP")) {
					if(!afterPrefix.equals("+")) lpage = afterPrefix; 
				} else if(prefix.equals("VL")) {
					vol = Integer.parseInt(afterPrefix); 
				} else if(prefix.equals("IS")) {
					issue = afterPrefix; 
				} else if(prefix.equals("PT")) {
					pt = afterPrefix; 
				} else if(prefix.equals("SN")) {
					issn = afterPrefix; 
				} else if(prefix.equals("UT")) {
					ut = afterPrefix; 
				} else if(prefix.equals("  ")) {
					if(currentField.equals("AB")) {
						abstr += " " + afterPrefix;
					} else if(currentField.equals("TI")) {
						title += " " + afterPrefix;
					} else if(currentField.equals("AF")) {
						authorsFull.add(afterPrefix);
					} else if(currentField.equals("AU")) {
						authorsShort.add(afterPrefix);
					}
				}
			} else if(line.equals("ER")) {
				if("J".equals(pt) && !"".equals(ut)) {
					Element sciXML = makeRecord(title, abstr, authorsFull.size() > 0 ? authorsFull : authorsShort, 
							journal, fpage, lpage, vol, year, issue, ut);
					Document doc = new Document(sciXML);
					
					File paperDir = new File(outDir, ut.replaceAll(":", "-"));
					paperDir.mkdir();
					File outFile = new File(paperDir, "source.xml");
					Serializer ser = new Serializer(new FileOutputStream(outFile));
					ser.write(doc);
					
					/*Serializer ser = new Serializer(System.out);
					ser.setIndent(2);
					ser.write(doc);*/
				} 
				/*System.out.println(title);
				if(authorsFull.size() > 0) {
					System.out.println(authorsFull);
				} else if(authorsShort.size() > 0) {
					System.out.println(authorsShort);					
				}
				System.out.print(journal);
				if(vol != -1) System.out.print(" " + vol);
				if(!issue.equals("")) System.out.print(" (" + issue + ")");
				if(fpage != -1 && lpage != -1) {
					System.out.print(" " + fpage + "-" + lpage);
				} else if(fpage != -1) {
					System.out.print(" " + fpage);
				}
				if(year != -1) System.out.print(" " + year);
				System.out.println();
				System.out.println(abstr);
				System.out.println();*/
				currentField = "";
				abstr = "";
				title = "";
				journal = "";
				year = -1;
				fpage = "";
				lpage = "";
				vol = -1;
				issue = "";
				pt = "";
				issn = "";
				ut = "";
				authorsFull = new ArrayList<String>();
				authorsShort = new ArrayList<String>();
			}			
			line = br.readLine();
		} 
	}

}
