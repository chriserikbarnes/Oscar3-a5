package uk.ac.cam.ch.wwmm.oscar3.sciborg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import uk.ac.cam.ch.wwmm.oscar3.Oscar3Props;

public class ResultDb {

	private Connection db;
	Map<String,PreparedStatement> psCache;

	public ResultDb() {
	   	try {
    		Class.forName("org.postgresql.Driver");
    	} catch (ClassNotFoundException ex) {
    		throw new RuntimeException(ex);
    	}
    	// open connection
    	String url="jdbc:postgresql:" + "papers";
    	// todo: port
    	try {
    		db = DriverManager.getConnection(url , Oscar3Props.getInstance().safdbusername, Oscar3Props.getInstance().safdbpasswd);
    	} catch (SQLException ex) {
    		throw new RuntimeException(ex);
    	}
	}
	
	public Connection getDb() {
		return db;
	}
	
	public PreparedStatement prepareStatement(String s) throws SQLException {
		if(psCache == null) psCache = new HashMap<String,PreparedStatement>();
		if(psCache.containsKey(s)) return psCache.get(s);
		PreparedStatement ps = db.prepareStatement(s);
		psCache.put(s, ps);
		return ps;
	}
	
}
