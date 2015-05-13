package weibo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import weibo.client.WeiboClient4Follows;
import weibo.database.AccountDB;
import weibo.database.UserRelationDB;
import weibo.objects.WeiboAccount;
import common.Out;

public class CrawlFollows {
	
	public static void main(String[] args)
			throws SQLException, IOException, JSONException {
		UserRelationDB mysql = new UserRelationDB();
		AccountDB accDB = new AccountDB();
		Map<String, Integer> pairs = mysql.getFollowsUncrawledPairs();
		Map<String, WeiboAccount> accs = accDB.getBannedWeiboAccounts();
		WeiboClient4Follows weiboClient = new WeiboClient4Follows(accs);
		weiboClient.setAccountExceptionHandler(accDB);
		for(String uid: pairs.keySet()){
			List<String> follows = weiboClient.getAllFollows(uid, pairs.get(uid));
			if(follows == null) continue;
			Out.println("=> " + follows.size());
			if(follows.size() > 0) mysql.stroreRelations(uid, follows);
		}
	}

}
