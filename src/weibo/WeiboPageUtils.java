package weibo;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WeiboPageUtils {
	public static Document getFullHtml(Document doc) throws JSONException{
		Elements scripts = doc.select("script");
		StringBuilder sb = new StringBuilder(doc.body().toString());
		for(Element e: scripts){
			String json = e.toString();
			if(!json.contains("\"html\":\"")) continue;
			json = json.substring(json.indexOf("{"),json.lastIndexOf("}") + 1);
			JSONObject obj = new JSONObject(json);
			sb.append(obj.get("html"));
		}
		return Jsoup.parse(sb.toString());
	}
}
