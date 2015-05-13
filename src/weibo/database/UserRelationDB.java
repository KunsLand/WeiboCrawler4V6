package weibo.database;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class UserRelationDB extends UserDB {
	private String userrelation_table = "userrelation_extra";

	public UserRelationDB() throws SQLException {
		super();
	}
	
	public void setTable(String table){
		userrelation_table = table;
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

}
