package uk.ac.cam.ch.wwmm.oscar3.sciborg;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**This is utterly vile. Replace this before too long!
 * 
 * @author ptc24
 *
 */
public class PTCSafDoc {

	public static class RMRS {
		String type;
		String id;
		String content;
	}
	
	private Connection db;
	
    protected static String quote_ident(String s) {	
    	StringBuffer str = new StringBuffer();
    	str.append('"');

    	int len = (s != null) ? s.length() : 0;
    	for (int i = 0; i < len; i++) {
    		char ch = s.charAt(i);
    		switch (ch) {
    		case '"': {
    			str.append("\\\"");
    			break;
    		}
    		default: {
    			str.append(ch);
    		}
    		}
    	}

    	str.append('"');
    	return str.toString();
    }

	
	public PTCSafDoc(Connection db, String schema) {
		this.db = db;
		System.out.println(schema);
	   	String setSearchPath = "SET search_path TO " + quote_ident(schema);
		try {
			Statement sql = db.createStatement();
			sql.executeUpdate(setSearchPath);
		}
		catch (SQLException ex) {
			ex.printStackTrace();
		}
	}
	
	public List<String> getSentenceIDs() throws SQLException {
		List<String> sentences = new ArrayList<String>();
		
		PreparedStatement ps = db.prepareStatement("SELECT id FROM annot WHERE annot.type = 'sentence';");
    	
    	ResultSet rs = ps.executeQuery();
    	while(rs.next()) {
    		sentences.add(rs.getString(1));
    	}
    	return sentences;
	}

	public List<String> getRMRSSentenceIDs() throws SQLException {
		List<String> sentences = new ArrayList<String>();
		
		PreparedStatement ps = db.prepareStatement("SELECT a.id FROM annot a JOIN annot b ON (a.from = b.from AND a.to = b.to) WHERE a.type = 'sentence' AND (b.type='pet_mrs' OR b.type='mrs');");
    	
    	ResultSet rs = ps.executeQuery();
    	while(rs.next()) {
    		sentences.add(rs.getString(1));
    	}
    	return sentences;
	}
	
	public String getSentenceString(String id) throws SQLException {
		PreparedStatement ps = db.prepareStatement("SELECT val FROM annot JOIN content ON (annot.content = content.id) WHERE annot.id=?;");
		ps.setString(1, id);

		ResultSet rs = ps.executeQuery();
		if(!rs.next()) {
			return null;
		} else {
			String ss = rs.getString(1);
			if(rs.next()) throw new RuntimeException("id is not unique!");
			return ss;
		}
	}
	
	public List<String> getRMRSIDs(String sid) throws SQLException {
		List<String> ids = new ArrayList<String>();
		
		PreparedStatement ps = db.prepareStatement("SELECT annot.from, annot.to FROM annot WHERE id=?;");
		ps.setString(1, sid);
		
		ResultSet rs = ps.executeQuery();
		if(!rs.next()) {
			return ids;
		} else {
			int from = rs.getInt(1);
			int to = rs.getInt(2);
			if(rs.next()) throw new RuntimeException("id is not unique!");
			
			ps = db.prepareStatement("SELECT id FROM annot WHERE annot.from = ? AND annot.to = ? AND (annot.type='pet_mrs' OR annot.type='mrs');");
			ps.setInt(1, from);
			ps.setInt(2, to);
			rs = ps.executeQuery();
			while(rs.next()) {
				ids.add(rs.getString(1));
			}
		}
		return ids;
	}

	public String getRMRS(String id) throws SQLException {
		PreparedStatement ps = db.prepareStatement("SELECT val FROM annot JOIN content ON (annot.content = content.id) WHERE annot.id=?;");
		ps.setString(1, id);

		ResultSet rs = ps.executeQuery();
		if(!rs.next()) {
			return null;
		} else {
			String ss = rs.getString(1);
			if(rs.next()) throw new RuntimeException("id is not unique!");
			return ss;
		}
	}
	
	
	public List<RMRS> getRMRSes(int cfrom, int cto) throws SQLException {
		List<RMRS> rmrses = new ArrayList<RMRS>();
		rmrses.addAll(getRMRSes(cfrom, cto, "pet_mrs"));
		rmrses.addAll(getRMRSes(cfrom, cto, "mrs"));
		return rmrses;
	}
		
	public List<RMRS> getRMRSes(int cfrom, int cto, String type) throws SQLException {
		List<RMRS> rmrses = new ArrayList<RMRS>();
		PreparedStatement ps = db.prepareStatement("SELECT annot.id, content.val FROM annot JOIN content ON (annot.content = content.id) WHERE annot.from=? AND annot.to=? AND annot.type=?;");
		ps.setInt(1, cfrom);
		ps.setInt(2, cto);
		ps.setString(3, type);
		
		
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			RMRS rmrs = new RMRS();
			rmrs.type = type;
			rmrs.id = rs.getString(1);
			rmrs.content = rs.getString(2);
			rmrses.add(rmrs);
		}
		//System.out.println(rmrses.size() + " RMRSes");
		return rmrses;
	}
	
	public List<Sentence> getSentences() throws SQLException {
		List<Sentence> sentences = new ArrayList<Sentence>();
		
		PreparedStatement ps = db.prepareStatement("SELECT annot.id, annot.from, annot.to, content.val FROM annot JOIN content ON (annot.content = content.id) WHERE annot.type = 'sentence' ORDER BY annot.from;");
    	
    	ResultSet rs = ps.executeQuery();
    	while(rs.next()) {
    		Sentence sentence = new Sentence();
    		sentence.id = rs.getString(1);
    		sentence.cfrom = rs.getInt(2);
    		sentence.cto = rs.getInt(3);
    		sentence.content = rs.getString(4);
    		sentences.add(sentence);
    	}
    	return sentences;
	}
	
	public List<OscarNE> getOscarNEs() throws SQLException {
		List<OscarNE> nes = new ArrayList<OscarNE>();
		
		PreparedStatement ps = db.prepareStatement("SELECT annot.from, annot.to, annot.content, annot.id FROM annot WHERE annot.type = 'fspp_oscar';");
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			OscarNE ne = new OscarNE();
			ne.cfrom = rs.getInt(1);
			ne.cto = rs.getInt(2);
			int content = rs.getInt(3);
			ne.setSafId(rs.getString(4));
			ps = db.prepareStatement("SELECT name, val FROM content WHERE id = ?;");
			ps.setInt(1, content);
			ResultSet innerRs = ps.executeQuery();
			ne.content = new HashMap<String, String>();
			while(innerRs.next()) {
				ne.content.put(innerRs.getString(1), innerRs.getString(2));
			}
			nes.add(ne);
		}
		Collections.sort(nes);
		
		return nes;
	}
	
	public Map<String,String> fooMap() throws SQLException {
		PreparedStatement ps = db.prepareStatement("SELECT a.from, a.to, b.from, b.to FROM annot a JOIN annot b ON (a.source=b.source AND a.target=b.target) WHERE (a.type='fspp_oscar' AND b.type='fspp_token') AND (a.from != b.from OR a.to != b.to);");
		ResultSet rs = ps.executeQuery();
		
		Map<String,String> fm = new HashMap<String,String>();
		while(rs.next()) {
			fm.put(rs.getInt(3) + "->" + rs.getInt(4), rs.getInt(1) + "->" + rs.getInt(2));
		}
		return fm;
	}
	
}
