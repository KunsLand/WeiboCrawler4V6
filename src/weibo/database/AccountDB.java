package weibo.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import weibo.interfaces.AccountExceptionHandler;
import weibo.objects.WeiboAccount;
import common.Out;
import common.TimeUtils;

public class AccountDB extends MySQLDB implements AccountExceptionHandler {

	public AccountDB() throws SQLException {
		super();
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

	public Map<String, WeiboAccount> getAvailableWeiboAccounts(int n)
			throws SQLException {
		Map<String, WeiboAccount> accs = new HashMap<String, WeiboAccount>();
		Statement stmt = conn.createStatement();
		String verifycode_time = TimeUtils.format2Minute(new Date(System
				.currentTimeMillis() - 3600 * 24 * 1000));
		String sql = "select account, password, cookie from account"
				+ " where banned=false and freeze=false and ("
				+ "verifycode_time is null or verifycode_time <= '"
				+ verifycode_time + "') limit " + n;
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
}
