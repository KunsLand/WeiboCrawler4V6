package weibo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import common.Out;

public class CrawlFollows {
	
	public static void main(String[] args)
			throws SQLException, IOException, JSONException {
		final MySQLDataBase mysql = new MySQLDataBase();
		Map<String, Integer> pairs = mysql.getFollowsUncrawledPairs();
		Map<String, WeiboAccount> accs = mysql.getBannedWeiboAccounts();
		WeiboClient weiboClient = new WeiboClient(accs);
		weiboClient.setExceptionHandler(mysql);
		for(String uid: pairs.keySet()){
			List<String> follows = weiboClient.getAllFollows(uid, pairs.get(uid));
			if(follows == null) continue;
			Out.println("=> " + follows.size());
			if(follows.size() > 0) mysql.stroreRelations(uid, follows);
		}
	}

}
