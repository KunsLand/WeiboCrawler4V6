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

	public MicroblogDB() {
		super();
	}

	public Set<String> getMids4CrawlingReposts() {
		Set<String> mids = new HashSet<String>();
		String sql = "select mid from repostcrawled where flag=0 and pnf!=1";
		try {
			Statement stmt = conn.createStatement();
			ResultSet result = stmt.executeQuery(sql);
			while (result.next()) {
				mids.add(result.getString(1));
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return mids;
	}

	public List<String> getURLs4CrawlingReposts() {
		List<String> urls = new ArrayList<String>();
		String sql = "select url from repostcrawled where flag=0 and pnf=0";
		try {
			Statement stmt = conn.createStatement();
			ResultSet result = stmt.executeQuery(sql);
			while (result.next()) {
				urls.add(result.getString(1));
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return urls;
	}

	public Map<String, String> getMidsAndURLs4CrawlingReposts() {
		Map<String, String> pairs = new HashMap<String, String>();
		String sql = "select mid,url from repostcrawled where flag=0";
		try {
			Statement stmt = conn.createStatement();
			ResultSet result = stmt.executeQuery(sql);
			while (result.next()) {
				pairs.put(result.getString(1), result.getString(2));
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return pairs;
	}

	public synchronized void updateRepostCrawled(String mid) {
		String sql = "update repostcrawled set flag=1 where mid=" + mid;
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public synchronized void updateMicroblogRelations(String parentMid,
			List<String> mids) {
		if (mids == null)
			return;
		try {
			updateRepostCrawled(parentMid);
			if (mids.isEmpty())
				return;
			String sql = "replace into microblogrelation(mid, parentid) values(?,?)";
			PreparedStatement stmt = conn.prepareStatement(sql);
			conn.setAutoCommit(false);
			for (String mid : mids) {
				stmt.setString(1, mid);
				stmt.setString(2, parentMid);
				stmt.addBatch();
			}
			stmt.executeBatch();
			conn.setAutoCommit(true);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public synchronized void pageNotFound(String url) {
		String sql = "update repostcrawled set pnf=1 where url='" + url + "'";
		Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public synchronized void zeroRepost(String mid) {
		String sql = "update repostcrawled set flag=1 where mid=" + mid;
		Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public synchronized void pageAvailable(String url) {
		String sql = "update repostcrawled set pnf=2 where url='" + url + "'";
		Statement stmt;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

}
