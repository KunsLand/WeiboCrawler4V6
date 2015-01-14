package weibo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import common.Out;
import weibo.login.CookieManager;

public class CrawlUserIndexPage {
	private CookieManager cm = null;
	private int timeout = 60*1000;

	public CrawlUserIndexPage(CookieManager cm) {
		this.cm = cm;
	}

	public UserIndexPage crawl(String uid) throws IOException, JSONException,
			SQLException {
		String url = "http://weibo.com/u/" + uid;
		Document doc = Jsoup.connect(url)
				.cookies(cm.getCookie()).timeout(timeout).get();
		doc = WeiboPageUtils.getFullHtml(doc);
		if(!doc.select("div.page_error").isEmpty()) return null;
		UserIndexPage uip = new UserIndexPage();
		Elements counters = doc.select("table.tb_counter strong");
		uip.UID = uid;
		uip.FOLLOWS = Integer.valueOf(counters.get(0).ownText());
		uip.FANS = Integer.valueOf(counters.get(1).ownText());
		uip.BLOGS = Integer.valueOf(counters.get(2).ownText());
		uip.NICK_NAME = doc.select("span.username").text();
		Elements posts =  doc.select("div[action-type="
						+ "feed_list_item]:not([feedtype=top])"
						+ " div[class=WB_from S_txt2] a");
		uip.LAST_POST = posts.isEmpty()? null : posts.first().attr("title");
		uip.VERIFIED = !doc.select(
				"div.verify_area a[class~=icon_verify_(co_)?v]").isEmpty();
		Out.println(uip.toString());
		return uip;
	}

	public class UserIndexPage {
		public String UID = null, NICK_NAME = null, LAST_POST = null;
		public int FOLLOWS = 0, FANS = 0, BLOGS = 0;
		public boolean VERIFIED = false;

		@Override
		public String toString() {
			return "{ UID: " + UID + ", NICK_NAME: " + NICK_NAME + ", VERIFIED: "
					+ VERIFIED + ", LAST_POST: " + LAST_POST + ", FOLLOWS: "
					+ FOLLOWS + ", FANS: " + FANS + ", BLOGS: " + BLOGS + "}";
		}
	}

	public static void main(String[] args) throws SQLException, IOException, JSONException {
		String dburl = "jdbc:mysql://localhost:3306/sinamicroblog?"
				+ "user=root&password=root";
		Connection conn = DriverManager.getConnection(dburl);
		Statement stmt = conn.createStatement();
		String sql = "select uid from user_index_page where nickname is null limit 100000";
		ResultSet result = stmt.executeQuery(sql);
		List<String> uids = new ArrayList<String>();
		while (result.next()) {
			uids.add(result.getString(1));
		}
		stmt.close();
		List<UserIndexPage> uips = new ArrayList<UserIndexPage>();
		CookieManager cm = new CookieManager(dburl, "account");
		CrawlUserIndexPage cuip = new CrawlUserIndexPage(cm);
		sql = "replace into user_index_page(nickname,verified,"
				+ "lastpost,follows,fans,blogs) values(?,?,?,?,?,?)"
				+ " where uid=?";
		PreparedStatement ps = conn.prepareStatement(sql);
		int count = 0;
		for(String uid: uids){
			Out.println("Crawling uid: " + uid);
			UserIndexPage uip = cuip.crawl(uid);
			if(uip == null) continue;
			uips.add(uip);
			ps.setString(1, uip.NICK_NAME);
			ps.setBoolean(2, uip.VERIFIED);
			ps.setString(3, uip.LAST_POST);
			ps.setInt(4, uip.FOLLOWS);
			ps.setInt(5, uip.FANS);
			ps.setInt(6, uip.BLOGS);
			ps.setString(7, uip.UID);
			ps.addBatch();
			if(++count == 100) {
				Out.println("100 batches saving ...");
				ps.executeBatch();
				count = 0;
			}
		}
		if(count > 0) ps.executeBatch();
		ps.close();
	}

}
