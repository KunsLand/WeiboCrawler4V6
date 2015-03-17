package weibo.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import common.Out;
import weibo.WeiboAccount;
import weibo.WeiboClient;

public class Test {

	public static void main(String[] args) throws IOException, JSONException{
		String url = "http://gov.weibo.com/attention/attsList.php?"
				+ "action=1&uid=1834627797&page=1";
		Map<String, WeiboAccount> accounts = WeiboClient.getVisitorAccounts();
		WeiboClient.setProxy("localhost", "1080");
		List<String> uns = new ArrayList<String>(accounts.keySet());
		Response res = Jsoup.connect(url)
				.cookies(accounts.get(uns.get(0)).COOKIES).execute();
		Out.println(res.url().toString());
		Out.println(WeiboClient.getFullHtml(res.parse())
				.select("a[namecard=true]").toString());
	}

}
