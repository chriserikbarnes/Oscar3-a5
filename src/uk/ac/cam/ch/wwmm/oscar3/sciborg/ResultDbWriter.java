package uk.ac.cam.ch.wwmm.oscar3.sciborg;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Nodes;
import uk.ac.cam.ch.wwmm.ptclib.datastruct.CacheMap;
import uk.ac.cam.ch.wwmm.ptclib.scixml.ValueCharPoint2XMLCharPoint;
import uk.ac.cam.ch.wwmm.ptclib.scixml.Xpoint2Charpoint;

public class ResultDbWriter {

	ResultDb rdb;
	CacheMap<EP,Integer> epToID;
	int rmrseps;
	PreparedStatement insertRmrsEp;
	int epidseq;
	int neidseq;
	int rmrsidseq;
	int docidseq;
	int sentidseq;
	
	public ResultDbWriter() {
		rdb = new ResultDb();
	}
	
	public void makeTables() {
		try {
			Statement s = rdb.getDb().createStatement();
			/*s.execute("DROP TABLE IF EXISTS document;");
			s.execute("DROP TABLE IF EXISTS sentence");
			s.execute("DROP TABLE IF EXISTS rmrs");			
			s.execute("DROP TABLE IF EXISTS eptype");			
			s.execute("DROP TABLE IF EXISTS rmrsep");
			s.execute("DROP TABLE IF EXISTS neinstance");
			
			s.execute("DROP SEQUENCE IF EXISTS docid;");
			s.execute("DROP SEQUENCE IF EXISTS sentenceid;");
			s.execute("DROP SEQUENCE IF EXISTS rmrsid;");
			s.execute("DROP SEQUENCE IF EXISTS eptypeid");
			s.execute("DROP SEQUENCE IF EXISTS neinstanceid");
			
			s.execute("DROP INDEX IF EXISTS eptype_index");*/
			
			s.execute("CREATE SEQUENCE docid;");
			s.execute("CREATE SEQUENCE sentenceid;");
			s.execute("CREATE SEQUENCE rmrsid;");
			s.execute("CREATE SEQUENCE eptypeid");
			s.execute("CREATE SEQUENCE neinstanceid");
			s.execute("CREATE TABLE document " +
					//"(id INTEGER PRIMARY KEY, " +
					"(id INTEGER, " +
					"path TEXT NOT NULL, " +
					"oscarpath TEXT NOT NULL, " +
					"title TEXT);");
			s.execute("CREATE TABLE sentence " +
					//"(id INTEGER PRIMARY KEY, " +
					"(id INTEGER, " +
					"docid INTEGER, " +
					"cfrom INTEGER, " +
					"cto INTEGER, " +
					"vcfrom INTEGER, " + 
					"vcto INTEGER, " +
					"content TEXT);");
			s.execute("CREATE TABLE rmrs " +
					//"(id INTEGER PRIMARY KEY, " +
					"(id INTEGER, " +
					"sentenceid INTEGER, " +
					"type TEXT, " +
					"content TEXT);");
			s.execute("CREATE TABLE eptype " +
					//"(id INTEGER PRIMARY KEY, " +
					"(id INTEGER, " +
					"lemma TEXT, " +
					"pos TEXT, " +
					"sense TEXT, " +
					"gpred TEXT, " +
					"carg TEXT);");
			s.execute("CREATE TABLE rmrsep " +
					"(rmrsid INTEGER, " +
					"epid INTEGER," +
					"neid INTEGER);");
			s.execute("CREATE TABLE neinstance " +
					//"(id INTEGER PRIMARY KEY, " +
					"(id INTEGER, " +
					"sentenceid INTEGER, " +
					"docid INTEGER, " +
					"safid TEXT, " +
					"type TEXT, " +
					"value TEXT, " +
					"inchi TEXT," +
					"cfrom INTEGER," +
					"cto INTEGER);");
			s.execute("CREATE INDEX eptype_index ON eptype (lemma, pos, sense, gpred, carg);");
			
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
		
	public void decantSafDb(File oscarPaperRoot) throws Exception {
		epToID = new CacheMap<EP, Integer>(100000);
		rmrseps = 0;
		epidseq = 0;
		neidseq = 0;
		docidseq = 0;
		sentidseq = 0;
		PTCSaf ptcSaf = new PTCSaf("ptc24", "ptc24");
		rdb.getDb().setAutoCommit(false);
		for(String paperStr : ptcSaf.getSchemas()/*.subList(0, 3)*/) {
			int docId = insertDoc(paperStr, oscarPaperRoot);
			
			//if(true) continue;
			PTCSafDoc doc = ptcSaf.getSafDoc(paperStr);
			
			File f = new File(paperStr);
			String name = f.getName();
			name = name.substring(0, name.indexOf(".xml"));
			File dir = new File(oscarPaperRoot, name);
			File source = new File(dir, "source.xml");
			Map<Integer,Integer> vcpxcp = ValueCharPoint2XMLCharPoint.valueCharPoint2XMLCharPoint(source);

			//List<PTCSafDoc.OscarNE> nes = new ArrayList<PTCSafDoc.OscarNE>();
			//Map<String,String> fm = new HashMap<String,String>();
					
			List<Sentence> sentences = doc.getSentences();
			for(Sentence sentence : sentences) {
				int sentenceId = insertSentence(docId, sentence, vcpxcp);
				for(PTCSafDoc.RMRS rmrs : doc.getRMRSes(sentence.cfrom, sentence.cto)) {
					insertRMRS(sentenceId, rmrs.type, rmrs.content);
					//insertRMRS(sentenceId, rmrs.type, rmrs.content, nes, fm);
				}
				//System.out.println(sentence);
			}
			List<OscarNE> nes = doc.getOscarNEs();
			for(OscarNE ne : nes) {
				//System.out.println(ne);
				//System.out.println();
				Sentence sentence = findSentence(ne, sentences);
				int id = insertNeInstance(ne, sentence, docId);
				ne.setId(id);
			}
			Map<String,String> fm = doc.fooMap();
			long time = System.currentTimeMillis();
			rdb.getDb().commit();
			//System.out.println("Commit time: " + (System.currentTimeMillis() - time));			
		}
		rdb.getDb().setAutoCommit(true);
	}
	
	public Sentence findSentence(OscarNE ne, List<Sentence> sentences) {
		int idx = Collections.binarySearch(sentences, ne);
		if(idx > -1) return sentences.get(idx);
		return null;
	}
	
	public int insertDoc(String paperStr, File oscarDir) {
		try {

			//db.setAutoCommit(false);
			//Statement s = db.createStatement();
			//ResultSet rs = s.executeQuery("SELECT nextval('docid');");
			//rs.next();
			//int id = rs.getInt(1);
			int id = ++docidseq;
			String title = null;
			
			//System.out.println(paperStr);
			File f = new File(paperStr);
			String name = f.getName();
			name = name.substring(0, name.indexOf(".xml"));
			//System.out.println(name);
			File dir = new File(oscarDir, name);
			File source = new File(dir, "source.xml");
			File markedup = new File(dir, "markedup.xml");
			//System.out.println(source.isFile());
			//System.out.println(markedup.isFile());
			String canPath = null;
			try {
				Document sourceDoc = new Builder().build(source);
				Nodes nn = sourceDoc.query("//CURRENT_TITLE");
				if(nn.size() == 1) title = nn.get(0).getValue();
				canPath = dir.getCanonicalPath();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			PreparedStatement ps = rdb.prepareStatement("INSERT INTO document VALUES (?,?,?,?);");
			ps.setInt(1, id);
			ps.setString(2, paperStr);
			ps.setString(3, canPath);
			ps.setString(4, title);
			ps.executeUpdate();
			//System.out.println("Update: " + ps.executeUpdate());
			//db.commit();
			//db.setAutoCommit(true);
			return id;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
	
	public int insertSentence(int paperId, Sentence sentence, Map<Integer,Integer> vcpxcp) {
		try {
			//db.setAutoCommit(false);
			//Statement s = db.createStatement();
			//ResultSet rs = s.executeQuery("SELECT nextval('sentenceid');");
			//rs.next();
			//int id = rs.getInt(1);
			int id = ++sentidseq;
			PreparedStatement ps = rdb.prepareStatement("INSERT INTO sentence VALUES (?,?,?,?,?,?,?);");
			ps.setInt(1, id);
			ps.setInt(2, paperId);
			ps.setInt(3, sentence.cfrom);
			ps.setInt(4, sentence.cto);
			ps.setInt(5, vcpxcp.get(sentence.cfrom));
			ps.setInt(6, vcpxcp.get(sentence.cto));
			ps.setString(7, sentence.content);
			ps.executeUpdate();
			sentence.resultDbId = id;
			return id;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}

	public int insertRMRS(int sentenceId, String type, String content) {
		try {
			//db.setAutoCommit(false);
			//Statement s = db.createStatement();
			//ResultSet rs = s.executeQuery("SELECT nextval('rmrsid');");
			//rs.next();
			//int id = rs.getInt(1);
			int id = ++rmrsidseq;
			PreparedStatement ps = rdb.prepareStatement("INSERT INTO rmrs VALUES (?,?,?,?);");
			ps.setInt(1, id);
			ps.setInt(2, sentenceId);
			ps.setString(3, type);
			ps.setString(4, content);
			ps.executeUpdate();
			return id;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}

	public void processRMRS(int id, String content, List<OscarNE> oscarNEs, Map<String,String> fm) {
		try {
			DigestRMRS drmrs = new DigestRMRS(content);
			List<List<Integer>> insertList = new ArrayList<List<Integer>>();
			for(EP ep : drmrs.getEps()) {
				int epid = insertOrFindEpType(ep);
				int neid = -1;
				
				if(true || "named_rel".equals(ep.gpred)) {
					OscarNE probe = new OscarNE();
					probe.cfrom = ep.cfrom;
					probe.cto = ep.cto;
					int p = Collections.binarySearch(oscarNEs, probe);
					if(p > -1) {
						//System.out.println(ep + "\t" + oscarNEs.get(p));
						neid = oscarNEs.get(p).getId();
					} else {
						String probeStr = probe.cfrom + "->" + probe.cto;
						String fmm = fm.get(probeStr);
						if(fmm != null) {
							String [] ss = fmm.split("->");
							probe.cfrom = Integer.parseInt(ss[0]);
							probe.cto = Integer.parseInt(ss[1]);
							p = Collections.binarySearch(oscarNEs, probe);
							if(p > -1) {
								//System.out.println(ep + "\tFOO\t" + oscarNEs.get(p));
								neid = oscarNEs.get(p).getId();
							} else {
								//System.out.println(ep + "\tNO MATCH AFTER TRANS");
							}
						} else {
							//System.out.println(ep + "\tNO MATCH");							
						}
					}					
				}

				List<Integer> l = new ArrayList<Integer>();
				l.add(id);
				l.add(epid);
				l.add(neid);
				/*if(true) {
					rmrseps++;
					//ps = makePreparedStatement("INSERT INTO rmrsep VALUES (?,?,?);");
					ps = insertRmrsEp;
					ps.setInt(1, id);
					ps.setInt(2, epid);
					if(neid > -1) {
						ps.setInt(3, neid);
					} else {
						ps.setNull(3, Types.INTEGER);
					}
					ps.executeUpdate();					
				}*/
				insertList.add(l);
			}
			//db.setAutoCommit(false);
			for(List<Integer> il : insertList) {
				rmrseps++;
				PreparedStatement ps = rdb.prepareStatement("INSERT INTO rmrsep VALUES (?,?,?);");
				//ps = insertRmrsEp;
				ps.setInt(1, il.get(0));
				ps.setInt(2, il.get(1));
				int neid = il.get(2);
				if(neid > -1) {
					ps.setInt(3, neid);
				} else {
					ps.setNull(3, Types.INTEGER);
				}
				ps.executeUpdate();					
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		
	}
	
	public int insertNeInstance(OscarNE ne, Sentence sentence, int docId) {
		//if(true) return 1;
 		try {
			//db.setAutoCommit(false);
			/*Statement s = db.createStatement();
			ResultSet rs = s.executeQuery("SELECT nextval('neinstanceid');");
			rs.next();
			int id = rs.getInt(1);*/
 			int id = ++neidseq;
			PreparedStatement ps = rdb.prepareStatement("INSERT INTO neinstance VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
			ps.setInt(1, id);
			if(sentence == null) {
				ps.setNull(2, Types.INTEGER);
			} else {
				ps.setInt(2, sentence.resultDbId);
			}
			ps.setInt(3, docId);
			ps.setString(4, ne.getSafId());
			ps.setString(5, ne.content.get("type"));
			ps.setString(6, ne.content.get("surface"));
			ps.setString(7, ne.content.get("InChI"));
			ps.setInt(8, ne.cfrom);
			ps.setInt(9, ne.cto);
			ps.executeUpdate();
			//db.setAutoCommit(true);
			return id;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}		

	}
	
	public int insertOrFindEpType(EP ep) {
		try {
			if(epToID.containsKey(ep)) {
				return epToID.get(ep);
			}
			//if(true) return 1;
			/*PreparedStatement ps = makePreparedStatement("SELECT id FROM eptype WHERE lemma IS NOT DISTINCT FROM ? AND pos IS NOT DISTINCT FROM ? AND sense IS NOT DISTINCT FROM ? AND gpred IS NOT DISTINCT FROM ? AND carg IS NOT DISTINCT FROM ?");
			ps.setString(1, ep.lemma);
			ps.setString(2, ep.pos);
			ps.setString(3, ep.sense);
			ps.setString(4, ep.gpred);
			ps.setString(5, ep.carg);*/
			List<String> args = new ArrayList<String>();
			String s = "SELECT id FROM eptype WHERE ";
			if(ep.lemma == null) {
				s += "lemma IS NULL AND ";
			} else {
				s += "lemma = ? AND ";
				args.add(ep.lemma);
			}
			if(ep.pos == null) {
				s += "pos IS NULL AND ";
			} else {
				s += "pos = ? AND ";
				args.add(ep.lemma);
			}
			if(ep.sense == null) {
				s += "sense IS NULL AND ";
			} else {
				s += "sense = ? AND ";
				args.add(ep.sense);
			}
			if(ep.gpred == null) {
				s += "gpred IS NULL AND ";
			} else {
				s += "gpred = ? AND ";
				args.add(ep.gpred);
			}
			if(ep.carg == null) {
				s += "carg IS NULL;";
			} else {
				s += "carg = ?;";
				args.add(ep.carg);
			}
			PreparedStatement ps = rdb.prepareStatement(s);
			for(int i=0;i<args.size();i++) {
				ps.setString(i+1, args.get(i));
			}
			
			ResultSet rs = ps.executeQuery();
			int id;
			if(rs.next()) {
				id = rs.getInt(1);
			} else {
			
			/*int id;
			if(true) {
				PreparedStatement ps;
				ResultSet rs;*/
				
				//db.setAutoCommit(false);
				Statement st = rdb.getDb().createStatement();
				/*rs = st.executeQuery("SELECT nextval('eptypeid');");
				rs.next();
				id = rs.getInt(1);*/
				id = ++epidseq;
				ps = rdb.prepareStatement("INSERT INTO eptype VALUES (?,?,?,?,?,?);");
				ps.setInt(1, id);
				ps.setString(2, ep.lemma);
				ps.setString(3, ep.pos);
				ps.setString(4, ep.sense);
				ps.setString(5, ep.gpred);
				ps.setString(6, ep.carg);
				ps.executeUpdate();
				//System.out.println("Update: " + ps.executeUpdate());
				//db.commit();
				//db.setAutoCommit(true);
			}
			//if("named_rel".equals(ep.gpred)) {
			//	System.out.println(ep);
			//}
			epToID.put(ep, id);
			return id;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}				
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//File oscarDir = new File("/home/ptc24/newows/corpora/big_oscarresults");
		File oscarDir = new File(args[0]);

		ResultDbWriter rdb = new ResultDbWriter();
		rdb.makeTables();
		long time = System.currentTimeMillis();
		rdb.decantSafDb(oscarDir);
		System.out.println(System.currentTimeMillis() - time);
		System.out.println(DigestRMRS.totalTime);
		System.out.println(DigestRMRS.rargnames);
		//rdb.testTables();
		System.out.println(rdb.rmrseps);
	}

}
