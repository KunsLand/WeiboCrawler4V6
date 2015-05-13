package weibo.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import weibo.interfaces.UserExceptionHandler;
import weibo.objects.UserIndexPage;
import common.Out;

public class UserDB extends MySQLDB implements UserExceptionHandler{
	private String user_table = "user_index_page";

	public UserDB() throws SQLException {
		super();
	}
	
	public void setTable(String table){
		user_table = table;
	}

	public Map<String, Integer> getFollowsUncrawledPairs() throws SQLException {
		String sql = "select uid,follows from " + user_table + " where"
				+ " nickname is not null and follows > 0 and verified != 2"
				+ " and available != false and follows_crawled != true"
				+ " and lastpost > '2013-12-31 23:59' limit 100000";
		Statement stmt = conn.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		Map<String, Integer> pairs = new HashMap<String, Integer>();
		while (result.next()) {
			pairs.put(result.getString(1), result.getInt(2));
		}
		stmt.close();
		return pairs;
	}

	public void setUserNotAvailable(String uid) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "update " + user_table + " set available = false"
					+ " where uid='" + uid + "'";
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			Out.println(e.getMessage() + " => " + uid);
		}
	}
	
	public List<String> getIndexPageNotCrawledUIDs() throws SQLException {
		Statement stmt = conn.createStatement();
		String sql = "select uid from user_index_page"
				+ " where nickname is null and available != false"
				+ " and follows_crawled != true limit 100000";
		ResultSet result = stmt.executeQuery(sql);
		List<String> uids = new ArrayList<String>();
		while (result.next()) {
			uids.add(result.getString(1));
		}
		stmt.close();
		return uids;
	}

	public void updateUserIndexPageTable(List<UserIndexPage> uips)
			throws SQLException {
		String sql = "replace into user_index_page(uid,nickname,verified,"
				+ "lastpost,follows,fans,blogs) values(?,?,?,?,?,?,?);";
		PreparedStatement ps = conn.prepareStatement(sql);
		conn.setAutoCommit(false);
		for (UserIndexPage uip : uips) {
			ps.setString(1, uip.UID);
			ps.setString(2, uip.NICK_NAME);
			ps.setInt(3, uip.VERIFIED);
			ps.setString(4, uip.LAST_POST);
			ps.setInt(5, uip.FOLLOWS);
			ps.setInt(6, uip.FANS);
			ps.setInt(7, uip.BLOGS);
			ps.addBatch();
		}
		conn.commit();
		conn.setAutoCommit(true);
		ps.executeBatch();
		ps.close();
	}

	@Override
	public void userNotAvailable(String uid) {
		setUserNotAvailable(uid);
	}


	@Override
	public void enterpriseUser(String uid) {
		String sql = "update " + user_table + " set verified=2"
				+ " where uid='" + uid + "'";
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			Out.println(e.getMessage() + " => " + uid);
		}
	}
}
