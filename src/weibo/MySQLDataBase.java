package weibo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.Out;
import common.TimeUtils;

public class MySQLDataBase {
	private Connection conn = null;

	public MySQLDataBase() throws SQLException {
		String dburl = "jdbc:mysql://localhost:3306/sinamicroblog?"
				+ "user=root&password=root";
		conn = DriverManager.getConnection(dburl);
	}

	public Map<String, WeiboAccount> getBannedWeiboAccounts()
			throws SQLException {
		Map<String, WeiboAccount> accs = new HashMap<String, WeiboAccount>();
		String sql = "select account,password,cookie from account"
				+ " where banned=true";
		Statement stmt = conn.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		while (result.next()) {
			WeiboAccount acc = new WeiboAccount();
			acc.UN = result.getString(1);
			acc.PSWD = result.getString(2);
			acc.COOKIES = cookieString2Map(result.getString(3));
			accs.put(acc.UN, acc);
		}
		stmt.close();
		return accs;
	}

	public Map<String, WeiboAccount> getAvailableWeiboAccounts()
			throws SQLException {
		Map<String, WeiboAccount> accs = new HashMap<String, WeiboAccount>();
		Statement stmt = conn.createStatement();
		String verifycode_time = TimeUtils.format2Minute(new Date(System
				.currentTimeMillis() - 3600 * 24 * 1000));
		String sql = "select account, password, cookie from account"
				+ " where banned=false and (verifycode_time is null or"
				+ " verifycode_time <= '" + verifycode_time + "')";
		ResultSet result = stmt.executeQuery(sql);
		while (result.next()) {
			WeiboAccount acc = new WeiboAccount();
			acc.UN = result.getString(1);
			acc.PSWD = result.getString(2);
			acc.COOKIES = cookieString2Map(result.getString(3));
			accs.put(acc.UN, acc);
		}
		return accs;
	}

	public Map<String, String> cookieString2Map(String cookie) {
		if (cookie == null || cookie.isEmpty())
			return null;
		Map<String, String> map = new HashMap<String, String>();
		cookie = cookie.substring(1, cookie.indexOf("}"));
		cookie.replaceAll(" ", "");
		String[] kvs = cookie.split(",");
		for (String s : kvs) {
			String[] kv = s.split("=");
			map.put(kv[0], kv[1]);
		}
		return map;
	}

	public Map<String, Integer> getFollowsUncrawledPairs() throws SQLException {
		String sql = "select uid,follows from user_index_page"
				+ " where nickname is not null and follows > 0"
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

	public void setAccountVerifyCodeTime(String account) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "update account set verifycode_time = '"
					+ TimeUtils.format2Minute(new Date()) + "' where account='"
					+ account + "'";
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			Out.println(e.getMessage() + " => " + account);
		}
	}

	public void setAccountFreezed(String account) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "update account set freeze = true where account='"
					+ account + "'";
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			Out.println(e.getMessage() + " => " + account);
		}
	}

	public void setUserNotAvailable(String uid) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "update user_index_page set available = false"
					+ " where uid='" + uid + "'";
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			Out.println(e.getMessage() + " => " + uid);
		}
	}

	public void stroreRelations(String uid, List<String> follows)
			throws SQLException {
		String sql = "insert into userrelation_extra(uid,fansid) values(?,?);";
		PreparedStatement ps = conn.prepareStatement(sql);
		for (String u : follows) {
			ps.setString(1, u);
			ps.setString(2, uid);
			ps.addBatch();
		}
		if (follows.size() > 0) {
			ps.execute();
			Statement stmt = conn.createStatement();
			String sql2 = "update user_index_page set follows_crawled=true where uid='"
					+ uid + "'";
			stmt.execute(sql2);
			stmt.close();
		}
		ps.close();
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
		for(UserIndexPage uip: uips){
			ps.setString(1, uip.UID);
			ps.setString(2, uip.NICK_NAME);
			ps.setBoolean(3, uip.VERIFIED);
			ps.setString(4, uip.LAST_POST);
			ps.setInt(5, uip.FOLLOWS);
			ps.setInt(6, uip.FANS);
			ps.setInt(7, uip.BLOGS);
			ps.addBatch();
		}
		ps.executeBatch();
		ps.close();
	}
}
