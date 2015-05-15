package weibo.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MicroblogDB extends MySQLDB {

	public MicroblogDB() throws SQLException {
		super();
	}
	public Set<String> getMids4CrawlingReposts() throws SQLException{
		Set<String> mids = new HashSet<String>();
		String sql = "select mid from repostcrawled where flag=0";
		Statement stmt = conn.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		while(result.next()){
			mids.add(result.getString(1));
		}
		stmt.close();
		return mids;
	}
	
	public List<String> getURLs4CrawlingReposts() throws SQLException{
		List<String> urls = new ArrayList<String>();
		String sql = "select url from repostcrawled where flag=0";
		Statement stmt = conn.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		while(result.next()){
			urls.add(result.getString(1));
		}
		stmt.close();
		return urls;
	}

	public Map<String, String> getMidsAndURLs4CrawlingReposts() throws SQLException{
		Map<String,String> pairs = new HashMap<String,String>();
		String sql = "select mid,url from repostcrawled where flag=0";
		Statement stmt = conn.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		while(result.next()){
			pairs.put(result.getString(1),result.getString(2));
		}
		stmt.close();
		return pairs;
	}
	
	public void updateRepostCrawled(String mid) throws SQLException{
		String sql = "update repostcrawled set flag=1 where mid="+mid;
		Statement stmt = conn.createStatement();
		stmt.execute(sql);
		stmt.close();
	}
	
	public synchronized void updateMicroblogRelations(String parentMid, List<String> mids) throws SQLException{
		if(mids==null || mids.isEmpty()) return;
		String sql = "replace into microblogrelation(mid, parentid) values(?,?)";
		PreparedStatement stmt = conn.prepareStatement(sql);
		conn.setAutoCommit(false);
		for(String mid: mids){
			stmt.setString(1, mid);
			stmt.setString(2, parentMid);
			stmt.addBatch();
		}
		stmt.executeBatch();
		updateRepostCrawled(parentMid);
		conn.setAutoCommit(true);
		stmt.close();
	}
}