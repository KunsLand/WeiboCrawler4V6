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
import java.util.Map;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import common.Out;
import weibo.login.CookieManager;

public class CrawlUserIndexPage {
	private CookieManager cm = null;
	private ExceptionHandler handler = null;

	public CrawlUserIndexPage(CookieManager cm) {
		this.cm = cm;
	}
	
	public void setExceptionHandler(ExceptionHandler eh){
		this.handler = eh;
	}

	public UserIndexPage crawl(String uid){
		try {
			String url = "http://www.weibo.com/u/" + uid;
			Map<String, String> cookie = cm.getCookie();
			if(cookie == null) {
				Out.println("No available account for crawling.");
				System.exit(0);
			}
			Document doc = Jsoup.connect(url)
					.cookies(cookie).get();
			doc = WeiboPageUtils.getFullHtml(doc);
			if(!doc.select("div.veriyfycode").isEmpty()){
				cm.handleVerifycodeException(cookie.get("un"));
				return null;
			}
			if(!doc.select("div.page_error").isEmpty()) {
				handler.userNotAvailable(uid);
				return null;
			}
			Elements counters = doc.select("table.tb_counter strong");
			if(counters.isEmpty()) return null;
			UserIndexPage uip = new UserIndexPage();
			uip.UID = uid;
			uip.FOLLOWS = Integer.valueOf(counters.get(0).ownText());
			uip.FANS = Integer.valueOf(counters.get(1).ownText());
			uip.BLOGS = Integer.valueOf(counters.get(2).ownText());
			uip.NICK_NAME = doc.select("span.username").text();
			Elements posts =  doc.select("div[action-type="
							+ "feed_list_item]:not([feedtype=top])"
							+ " div.WB_detail > div[class=WB_from S_txt2] a");
			uip.LAST_POST = posts.isEmpty()? null : posts.first().attr("title");
			uip.VERIFIED = !doc.select(
					"div.verify_area a[class~=icon_verify_(co_)?v]").isEmpty();
			Out.println(uip.toString());
			return uip;
		} catch (IOException | JSONException | SQLException e){
			Out.println(e.getMessage() + " => " + uid);
		}
		return null;
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

	public static void main(String[] args)
			throws SQLException, JSONException, IOException {
		String dburl = "jdbc:mysql://localhost:3306/sinamicroblog?"
				+ "user=root&password=root";
		final Connection conn = DriverManager.getConnection(dburl);
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
		List<UserIndexPage> uips = new ArrayList<UserIndexPage>();
		CookieManager cm = new CookieManager(dburl, "account");
		CrawlUserIndexPage cuip = new CrawlUserIndexPage(cm);
		
		cuip.setExceptionHandler(new ExceptionHandler(){
			@Override
			public void userNotAvailable(String uid) {
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
		});
		sql = "replace into user_index_page(uid,nickname,verified,"
				+ "lastpost,follows,fans,blogs) values(?,?,?,?,?,?,?);";
		PreparedStatement ps = conn.prepareStatement(sql);
		
		int count = 0;
		for(String uid: uids){
			UserIndexPage uip = cuip.crawl(uid);
			if(uip == null) continue;
			uips.add(uip);
			ps.setString(1, uip.UID);
			ps.setString(2, uip.NICK_NAME);
			ps.setBoolean(3, uip.VERIFIED);
			ps.setString(4, uip.LAST_POST);
			ps.setInt(5, uip.FOLLOWS);
			ps.setInt(6, uip.FANS);
			ps.setInt(7, uip.BLOGS);
			ps.addBatch();
			if(++count == 50) {
				Out.println("50 batches saving ...");
				ps.executeBatch();
				count = 0;
			}
		}
		if(count > 0) ps.executeBatch();
		ps.close();
	}

}
