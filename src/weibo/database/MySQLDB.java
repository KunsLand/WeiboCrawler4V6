package weibo.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MySQLDB {
	protected Connection conn = null;

	public MySQLDB() {
		String dburl = "jdbc:mysql://localhost:3306/sinamicroblog?"
				+ "user=root&password=root";
		try {
			conn = DriverManager.getConnection(dburl);
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void setTable(String table) {
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
}
