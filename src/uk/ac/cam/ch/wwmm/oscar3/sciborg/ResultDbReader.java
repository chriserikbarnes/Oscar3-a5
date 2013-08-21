package uk.ac.cam.ch.wwmm.oscar3.sciborg;

import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;

public class ResultDbReader {

	private static ResultDbReader myInstance;
	private ResultDb rdb;
	
	private Pattern xmlStart = Pattern.compile("\\s*<");
	
	public static ResultDbReader getInstance() {
		try {
			myInstance = new ResultDbReader();
			return myInstance;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private ResultDbReader() {
		rdb = new ResultDb();
	}
	
	private Document queryToXML(String objectname, String statement, Object... args) {
		return queryToXML(null, objectname, statement, args);
	}
	
	private Document queryToXML(Element container, String objectname, String statement, Object... args) {
		try {
			PreparedStatement ps = rdb.prepareStatement(statement);
			for(int i=0;i<args.length;i++) {
				if(args[i] instanceof Integer) {
					ps.setInt(i+1, (Integer)args[i]);
				} else {
					ps.setString(i+1, args[i].toString());					
				}
			}
			ResultSet rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			while(rs.next()) {
				Element elem = new Element(objectname);
				for(int i=1;i<=rsmd.getColumnCount();i++) {
					String val = rs.getString(i);
					boolean xmlised = false;
					if(val != null && xmlStart.matcher(val).lookingAt()) {
						try {
							Document doc = new Builder().build(new StringReader(val));
							Element e = doc.getRootElement();
							doc.setRootElement(new Element("dummy"));
							elem.appendChild(e);
							xmlised = true;
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					if(!xmlised && val != null) elem.addAttribute(new Attribute(rsmd.getColumnName(i), val));
					if(rsmd.getColumnName(i).equals("oscarpath")) {
						String op = val;
						//String corpora = "/local/scratch/ptc24/sciborg/";
						String corpora = new File(Oscar3Props.getInstance().workspace, "corpora").toString();
						if(op.startsWith(corpora)) {
							String s = op.substring(corpora.length());
							if(s.startsWith("/")) s = s.substring(1);
							elem.addAttribute(new Attribute("shortpath", s));
						}
					}
				}
				if(container != null) {
					container.appendChild(elem);					
				} else if(rs.isLast()) {
					return new Document(elem);
				} else {
					return errorDoc(500, "Multiple results found unexpectedly");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return errorDoc(500, "SQL request threw exception");
		}
		if(container != null) {
			if(container.getDocument() == null) {
				return new Document(container);
			} else {
				return container.getDocument();				
			}
		} else {
			return errorDoc(404, "No results found");
		}
	}
	
	private Document errorDoc(int code, String error) {
		Element errorElem = new Element("error");
		errorElem.appendChild(error);
		errorElem.addAttribute(new Attribute("code", Integer.toString(code)));
		return new Document(errorElem);
	}
	
	private boolean isError(Document doc) {
		return doc.getRootElement().getLocalName().equals("error");
	}
	
	/*@SuppressWarnings("unchecked")
	public Map<String,Integer> getPointMap(int docId) {
		if(rdb == null) return null;
		try {
			PreparedStatement ps = rdb.prepareStatement("SELECT pointmap FROM document WHERE id = ?;");
			ps.setInt(1, docId);
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				InputStream is = rs.getBinaryStream(1);
				GZIPInputStream gzis = new GZIPInputStream(is);
				ObjectInputStream ois = new ObjectInputStream(gzis);
				Object o = ois.readObject();
				if(o instanceof Map) {
					return (Map<String,Integer>)o;					
				} else {
					return null;
				}
			} else {
				return null;
			}			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}*/
	
	public int getPaperId(File paper) {
		if(rdb == null) return -1;
		try {
			String canPath = paper.getCanonicalPath();
			PreparedStatement ps = rdb.prepareStatement("SELECT * FROM document WHERE oscarpath = ?;");
			ps.setString(1, canPath);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				return rs.getInt(1);
			}
			return -1;
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}
	}
	
	public Document getPapers() {
		if(rdb == null) return null;
		Element rootElement = new Element("papers");
		return queryToXML(rootElement, "paper", "SELECT id, title, oscarpath FROM document;");
	}
	
	public Document getPaperInfo(int paperId) {
		if(rdb == null) return null;
		Document doc = queryToXML("paper", "SELECT id, title, oscarpath FROM document WHERE id = ?;", paperId);
		if(isError(doc)) return doc;
		return queryToXML(doc.getRootElement(), "sentence", "SELECT id, content FROM sentence WHERE docid = ?;", paperId);
	}
	
	public Document getSentence(int sentenceId) {
		if(rdb == null) return null;
		Document doc = queryToXML("sentence", "SELECT id, content, docid, cfrom, cto FROM sentence WHERE id = ?;", sentenceId);
		if(isError(doc)) return doc;
		doc = queryToXML(doc.getRootElement(), "rmrs", "SELECT id, type FROM rmrs WHERE sentenceid = ?;", sentenceId);		
		if(isError(doc)) return doc;
		return queryToXML(doc.getRootElement(), "ne", "SELECT id, type, value, inchi, cfrom, cto, safid FROM neinstance WHERE sentenceid = ?;", sentenceId);
	}
	
	public Document getSentence(int cfrom, int cto, int docId) {
		if(rdb == null) return null;
		return queryToXML("sentence", "SELECT id, content FROM sentence WHERE cfrom <= ? AND cto >= ? AND docid = ?;", cfrom, cto, docId);
	}
		
	public Document getRmrs(int rmrsId) {
		if(rdb == null) return null;
		return queryToXML("rmrs", "SELECT id, type, content, sentenceid FROM rmrs WHERE id = ?;", rmrsId);
	}
	
	public Document getNe(int neId) {
		if(rdb == null) return null;
		return queryToXML("ne", "SELECT id, docid, type, value, inchi, cfrom, cto FROM neinstance WHERE id = ?;", neId);
	}
	
	public Document getNes(Collection<String> safIds, int docId) {
		if(rdb == null) return null;
		try {
			long time = System.currentTimeMillis();
			rdb.getDb().setAutoCommit(false);
			PreparedStatement ps = rdb.prepareStatement("CREATE TEMPORARY TABLE safids (safid TEXT) ON COMMIT DROP;");
			ps.executeUpdate();
			ps = rdb.prepareStatement("INSERT INTO safids VALUES (?)");
			for(String safId : safIds) {
				ps.setString(1, "f" + safId);
				ps.executeUpdate();
				//ps.addBatch();
			}
			//ps.executeBatch();
			Document doc = queryToXML(new Element("nes"), "ne", "SELECT id, docid, type, value, inchi FROM neinstance WHERE docid = ? AND safid IN (SELECT safid FROM safids)", docId);			
			//Document doc = queryToXML(new Element("sentences"), "sentence", "SELECT DISTINCT sentence.id, sentence.content FROM sentence JOIN neinstance ON (sentence.id = neinstance.sentenceid) WHERE neinstance.docid = ? AND safid IN (SELECT safid FROM safids)", docId);			
			//rdb.getDb().commit();
			rdb.getDb().setAutoCommit(true);
			System.out.println(System.currentTimeMillis() - time);
			return doc;
		} catch (Exception e) {
			//rdb.getDb().setAutoCommit(true);
			return errorDoc(500, "Eeep!");
		}
	}
	
	public Document getSentences(List<Integer> startOffsets, List<Integer> endOffsets, List<Integer> docIds) {
		if(rdb == null) return null;
		if(startOffsets == null ||
				endOffsets == null || 
				docIds == null || 
				startOffsets.size() != endOffsets.size() ||
				startOffsets.size() != endOffsets.size()) return errorDoc(500, "Bad input to getSentences");
		try {
			long time = System.currentTimeMillis();
			rdb.getDb().setAutoCommit(false);
			PreparedStatement ps = rdb.prepareStatement("CREATE TEMPORARY TABLE offsets (vcfrom INTEGER, vcto INTEGER, docid INTEGER) ON COMMIT DROP;");
			ps.executeUpdate();
			ps = rdb.prepareStatement("INSERT INTO offsets VALUES (?, ?, ?)");
			for(int i=0;i<startOffsets.size();i++) {	
				ps.setInt(1, startOffsets.get(i));
				ps.setInt(2, endOffsets.get(i));
				ps.setInt(3, docIds.get(i));
				ps.executeUpdate();
			}
			Document doc = queryToXML(new Element("sentences"), "sentence", "SELECT DISTINCT id, sentence.docid, sentence.content FROM sentence JOIN offsets ON (sentence.docid = offsets.docid AND sentence.vcfrom <= offsets.vcfrom AND sentence.vcto >= offsets.vcto);");			
			//Document doc = queryToXML(new Element("sentences"), "sentence", "SELECT DISTINCT sentence.id, sentence.content FROM sentence JOIN neinstance ON (sentence.id = neinstance.sentenceid) WHERE neinstance.docid = ? AND safid IN (SELECT safid FROM safids)", docId);			
			//rdb.getDb().commit();
			rdb.getDb().setAutoCommit(true);
			System.out.println(System.currentTimeMillis() - time);
			return doc;
		} catch (Exception e) {
			//rdb.getDb().setAutoCommit(true);
			return errorDoc(500, "Eeep!");
		}

	}
	
}
