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

public class MySQLDataBase implements ExceptionHandler {
	private Connection conn = null;
	private String user_table = "user_index_page";
	private String userrelation_table = "userrelation_extra";

	public MySQLDataBase() throws SQLException {
		String dburl = "jdbc:mysql://localhost:3306/sinamicroblog?"
				+ "user=root&password=root";
		conn = DriverManager.getConnection(dburl);
	}

	public void setUserTable(String user_table) {
		this.user_table = user_table;
	}

	public Map<String, WeiboAccount> getBannedWeiboAccounts()
			throws SQLException {
		Map<String, WeiboAccount> accs = new HashMap<String, WeiboAccount>();
		String verifycode_time = TimeUtils.format2Minute(new Date(System
				.currentTimeMillis() - 3600 * 24 * 1000));
		String sql = "select account,password,cookie from account"
				+ " where banned=true and (verifycode_time is null"
				+ " or verifycode_time <= '" + verifycode_time + "')";
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
				+ " where banned=false and freeze=false and ("
				+ "verifycode_time is null or verifycode_time <= '"
				+ verifycode_time + "')";
		ResultSet result = stmt.executeQuery(sql);
		while (result.next()) {
			WeiboAccount acc = new WeiboAccount();
			acc.UN = result.getString(1);
			acc.PSWD = result.getString(2);
			acc.COOKIES = cookieString2Map(result.getString(3));
			accs.put(acc.UN, acc);
		}
		stmt.close();
		Out.println("NUM OF AVAILABLE ACCOUNTS => " + accs.size());
		return accs;
	}

	public Map<String, WeiboAccount> getFirstAvailableWeiboAccount()
			throws SQLException {
		Map<String, WeiboAccount> accs = new HashMap<String, WeiboAccount>();
		Statement stmt = conn.createStatement();
		String verifycode_time = TimeUtils.format2Minute(new Date(System
				.currentTimeMillis() - 3600 * 24 * 1000));
		String sql = "select account, password, cookie from account"
				+ " where banned=false and freeze=false and ("
				+ "verifycode_time is null or verifycode_time <= '"
				+ verifycode_time + "') limit 1";
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

	public List<Map<String, String>> getVisitorCookie() throws SQLException {
		List<Map<String, String>> cookies = new ArrayList<Map<String, String>>();
		String sql = "select cookie from visitor";
		Statement stmt = conn.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		while (result.next())
			cookies.add(cookieString2Map(result.getString(1)));
		stmt.close();
		return cookies;
	}

	public void setAccountVerifyCodeTime(String account) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "update account set cookie=null, verifycode_time = '"
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

	public void updateAccountCookie(String account, String cookie) {
		String sql = "update account set verifycode_time=null, cookie='"
				+ cookie + "'" + " where account='" + account + "'";
		Statement stmt = null;
		try {
			stmt = conn.createStatement();
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			Out.println(e.getMessage() + " => " + account);
		}
	}

	public void storeVisitorCookies(Map<String, String> cookie)
			throws SQLException {
		String sql = "insert into visitor values('" + cookie.toString() + "')";
		Statement stmt = conn.createStatement();
		stmt.execute(sql);
		stmt.close();
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

	public void stroreRelations(String uid, List<String> follows)
			throws SQLException {
		String sql = "insert ignore into " + userrelation_table
				+ "(uid,fansid) values(?,?)";
		PreparedStatement ps = conn.prepareStatement(sql);
		conn.setAutoCommit(false);
		for (String u : follows) {
			ps.setString(1, u);
			ps.setString(2, uid);
			ps.addBatch();
		}
		if (follows.size() > 0) {
			ps.executeBatch();
			Statement stmt = conn.createStatement();
			String sql2 = "update user_index_page set follows_crawled=true where uid='"
					+ uid + "'";
			stmt.execute(sql2);
			stmt.close();
		}
		conn.commit();
		conn.setAutoCommit(true);
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
	public void verifycodeException(String account) {
		setAccountVerifyCodeTime(account);
	}

	@Override
	public void freezeException(String account) {
		setAccountFreezed(account);
	}

	@Override
	public void updateCookie(WeiboAccount account) {
		updateAccountCookie(account.UN, account.COOKIES.toString());
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
