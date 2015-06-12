package weibo.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import common.Out;
import weibo.client.WeiboClient;
import weibo.objects.WeiboAccount;

public class Test {
	
	public static void test4Visitor() throws IOException, JSONException{
		String url = "http://passport.weibo.com/visitor/genvisitor";
		Response res = Jsoup.connect(url).data("cb", "gen_callback")
				.ignoreContentType(true).method(Method.POST)
				.followRedirects(true).execute();
		String msg = res.body();
		System.out.println(msg);
		JSONObject obj = new JSONObject(msg.substring(msg.indexOf("{"),
				msg.lastIndexOf("}") + 1));
		String tid = obj.getJSONObject("data").getString("tid");
		System.out.println(tid);

		url = "http://passport.weibo.com/visitor/visitor";
		res = Jsoup.connect(url).data("a", "incarnate").data("t", tid).cookie("tid", tid)
				.ignoreContentType(true).followRedirects(true).execute();
		System.out.println(res.cookies());
		
		url = "http://weibo.com/1875034341/CgY1SjF2J";
		res = Jsoup.connect(url).cookies(res.cookies()).followRedirects(true).execute();
		System.out.println(res.body());
	}
	
	public static void test4AttsList() throws IOException, JSONException{
		String url = "http://gov.weibo.com/attention/attsList.php?"
				+ "action=1&uid=1834627797&page=1";
		Map<String, WeiboAccount> accounts = WeiboClient.getVisitorAccounts(10);
		List<String> uns = new ArrayList<String>(accounts.keySet());
		Response res = Jsoup.connect(url)
				.cookies(accounts.get(uns.get(0)).COOKIES).execute();
		Out.println(res.url().toString());
		Out.println(WeiboClient.getFullHtml(res.parse())
				.select("a[namecard=true]").toString());
	}

	public static void main(String[] args) throws IOException, JSONException{
		test4Visitor();
	}

}
