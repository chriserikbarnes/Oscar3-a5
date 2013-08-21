package uk.ac.cam.ch.wwmm.oscar3.sciborg;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PTCSaf {

	private String dbName = "saf";
	private String host;
	private String userId;
	private String password;
    private Connection db;
    private PrintStream log = System.out;
    
    private void logln(String message) {
    	if(log!=null) {
    		log.println(message);
    		log.flush();
    	}
    }

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

    
    public PTCSaf(String userId, String password) throws Exception {
    	this.userId = userId;
    	this.password = password;

    	connect();
    }
    
    public void connect() {
    	if(this.dbName==null)
    		throw new RuntimeException("database name is not set");

    	// we use the postgresql driver
    	try {
    		Class.forName("org.postgresql.Driver");
    	} catch (ClassNotFoundException ex) {
    		logln(ex.getMessage());
    		ex.printStackTrace();
    		throw new RuntimeException("unable to load database driver");
    	}


    	// open connection
    	String url;
    	if (host==null) 
    		url="jdbc:postgresql:" + this.dbName;
    	else
    		url="jdbc:postgresql://" + this.host + "/" + this.dbName;
    	// todo: port
    	try {
    		db = DriverManager.getConnection(url , this.userId, this.password);
    	} catch (SQLException ex) {
    		// does the db referenced by url exist ?
    		//ex.printStackTrace();
    		throw new RuntimeException("unable to connect to SAF database " + url + ": " + ex.getMessage());
    	}

    	logln("connected to database " + url);
    }
    
    public List<String> getSchemas() throws SQLException {
    	List<String> results = new ArrayList<String>();
    	PreparedStatement ps = db.prepareStatement("SELECT schemaname FROM pg_tables WHERE tablename='annot'");
    	ResultSet rs = ps.executeQuery();
    	while(rs.next()) {
    		results.add(rs.getString(1));
    	}
    	return results;
    }
    
    public PTCSafDoc getSafDoc(String schema) {
    	return new PTCSafDoc(db, schema);
    }

}
