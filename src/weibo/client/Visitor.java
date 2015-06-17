package weibo.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;

import common.Out;
import common.TimeUtils;

public class Visitor {

	public static List<Map<String, String>> newVisitorCookies(int n)
			throws IOException, JSONException {
		List<Map<String, String>> cookies = new ArrayList<Map<String, String>>();
		for (int i = 0; i < n; i++) {
			Map<String, String> cookie = newVisitorCookie();
			if (cookie == null)
				continue;
			Out.println(cookie.toString());
			cookies.add(cookie);
			TimeUtils.Pause(RequestConfig.TIME_REQUEST_GAP);
		}
		Out.println("VALID VISITOR NUM: " + cookies.size());
		return cookies;
	}

	public static Map<String, String> newVisitorCookie() throws IOException {
		Response res = Jsoup
				.connect("http://passport.weibo.com/visitor/genvisitor")
				.data("cb", "gen_callback").ignoreContentType(true)
				.method(Method.POST).followRedirects(true).execute();
		String msg = res.body();
		String tid = null;
		try {
			JSONObject obj = new JSONObject(msg.substring(msg.indexOf("{"),
					msg.lastIndexOf("}") + 1));
			tid = obj.getJSONObject("data").getString("tid");
		} catch (JSONException e) {
			Out.println(e.getMessage());
		}

		if (tid == null)
			return null;

		res = Jsoup.connect("http://passport.weibo.com/visitor/visitor")
				.data("a", "incarnate").data("t", tid).cookie("tid", tid)
				.ignoreContentType(true).followRedirects(true).execute();
		return res.cookies();
	}
}
