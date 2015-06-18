package weibo.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import weibo.client.CookieStorage;
import weibo.client.WeiboAccount;
import common.TimeUtils;

public class AccountDB extends MySQLDB implements CookieStorage {

	public AccountDB() {
		super();
	}

	public List<WeiboAccount> getBannedWeiboAccounts() throws SQLException {
		List<WeiboAccount> accs = new ArrayList<WeiboAccount>();
		String sql = "select account,password,cookie from account where banned=true";
		Statement stmt = conn.createStatement();
		ResultSet result = stmt.executeQuery(sql);
		while (result.next()) {
			WeiboAccount acc = new WeiboAccount();
			acc.USERNAME = result.getString(1);
			acc.PASSWORD = result.getString(2);
			acc.COOKIES = cookieString2Map(result.getString(3));
			accs.add(acc);
		}
		stmt.close();
		return accs;
	}

	public List<WeiboAccount> getAvailableWeiboAccounts(int... args) {
		if (args.length > 1) {
			throw new IllegalArgumentException();
		}
		List<WeiboAccount> accs = new ArrayList<WeiboAccount>();
		try {
			Statement stmt = conn.createStatement();
			String sql = "select account, password, cookie from account where banned=false";
			if (args.length == 1) {
				sql += " order by rand() limit " + args[0];
			}
			ResultSet result = stmt.executeQuery(sql);
			while (result.next()) {
				WeiboAccount acc = new WeiboAccount();
				acc.USERNAME = result.getString(1);
				acc.PASSWORD = result.getString(2);
				acc.COOKIES = cookieString2Map(result.getString(3));
				accs.add(acc);
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
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
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void freezeAccount(String account) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "update account set freeze = true where account='"
					+ account + "'";
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void invalidateCookie(String account) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "update account set cookie = null where account='"
					+ account + "'";
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void banAccount(String account) {
		try {
			Statement stmt = conn.createStatement();
			String sql = "update account set banned = true where account='"
					+ account + "'";
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
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
			e.printStackTrace();
			System.exit(0);
		}
	}

	@Override
	public void updateCookie(WeiboAccount account) {
		updateAccountCookie(account.USERNAME, account.COOKIES.toString());
	}

	@Override
	public WeiboAccount getAccount() {
		return getAvailableWeiboAccounts(1).get(0);
	}

	@Override
	public List<WeiboAccount> getAccountList() {
		return getAvailableWeiboAccounts();
	}

	@Override
	public void invalidateCookie(WeiboAccount account) {
		freezeAccount(account.USERNAME);
	}

	@Override
	public void banAccount(WeiboAccount account) {
		banAccount(account.USERNAME);
	}
}
