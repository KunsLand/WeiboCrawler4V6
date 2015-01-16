package weibo;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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

import org.json.JSONException;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import common.Out;
import weibo.login.CookieManager;

public class CrawlFollows {
	private CookieManager cm = null;
	private ExceptionHandler handler = null;
	
	public CrawlFollows(CookieManager cm){
		this.cm = cm;
	}
	
	public void setExceptionHandler(ExceptionHandler eh){
		handler = eh;
	}
	
	public List<String> crawl(String uid, int follows){
		Out.println(uid + " => " + follows);
		try{
			Set<String> fol = new HashSet<String>();
			int pages = follows/20 + 1;
			for(int i=1; i <= pages; i++){
				String url = "http://gov.weibo.com/attention/attsList.php?action=1&uid="
						+ uid + "&page=" + i;
				Map<String, String> cookie = cm.getCookie();
				Response res = Jsoup.connect(url).cookies(cookie).execute();
				String redirected_url = res.url().toString();
				if(!redirected_url.startsWith("http://gov.")){
					Out.println("ENTERPRISE => " + uid);
					return null;
				}
				Document doc = WeiboPageUtils.getFullHtml(res.parse());
				if(!doc.select("div.veriyfycode").isEmpty()){
					cm.handleVerifycodeException(cookie.get("un"));
					return null;
				}
				if(!doc.select("div.page_error").isEmpty()) {
					handler.userNotAvailable(uid);
					return null;
				}
				else if(!doc.select("div#pl_common_unloginbase").isEmpty())
					cm.refreshCookie(cookie.get("un"));
				else if(redirected_url.contains("/signup/signup.php")
						|| redirected_url.contains("http://passport")){
					cm.refreshCookie(cookie.get("un"));
					return null;
				}
				Elements el = doc.select("a[namecard=true]");
//				Out.println(el.attr("uid"));
				for(Element e:el) fol.add(e.attr("uid"));
			}
			return new ArrayList<String>(fol);
		}catch(IOException | JSONException | SQLException e){
			Out.println(e.getMessage() + " => " + uid);
		}
		return null;
	}

	public static void main(String[] args)
			throws SQLException, IOException, JSONException {
		String dburl = "jdbc:mysql://localhost:3306/sinamicroblog?"
				+ "user=root&password=root";
		final Connection conn = DriverManager.getConnection(dburl);
		Statement stmt = conn.createStatement();
		String sql = "select uid,follows from user_index_page"
				+ " where nickname is not null and follows > 0"
				+ " and available != false and follows_crawled != true"
				+ " and lastpost > '2013-12-31 23:59' limit 100000";
		ResultSet result = stmt.executeQuery(sql);
		Map<String,Integer> pairs = new HashMap<String, Integer>();
		while (result.next()){
			pairs.put(result.getString(1), result.getInt(2));
		}
		stmt.close();
		CookieManager cm = new CookieManager(dburl, "account");
		CrawlFollows cf = new CrawlFollows(cm);
		
		cf.setExceptionHandler(new ExceptionHandler(){
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
		sql = "insert into userrelation_extra(uid,fansid) values(?,?);";
		PreparedStatement ps = conn.prepareStatement(sql);
		for(String uid: pairs.keySet()){
			List<String> follows = cf.crawl(uid, pairs.get(uid));
			if(follows == null) continue;
			Out.println("=> " + follows.size());
			for(String u: follows){
				ps.setString(1, u);
				ps.setString(2, uid);
				ps.addBatch();
			}
			if(follows.size() > 0){
				ps.execute();
				stmt = conn.createStatement();
				String sql2 = "update user_index_page set follows_crawled=true where uid='"
						+ uid + "'";
				stmt.execute(sql2);
				stmt.close();
			}
		}
		ps.close();
	}

}
