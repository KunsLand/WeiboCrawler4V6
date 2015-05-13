package weibo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;

import weibo.client.WeiboClient4User;
import weibo.database.UserDB;
import weibo.objects.UserIndexPage;
import common.Out;

public class CrawlUserIndexPage {
	public static void main(String[] args)
			throws SQLException, JSONException, IOException {
		final UserDB mysql = new UserDB();
		List<String> uids = mysql.getIndexPageNotCrawledUIDs();
		WeiboClient4User weiboClient = new WeiboClient4User();
		weiboClient.setUserExceptionHandler(mysql);
		
		int count = 0;
		List<UserIndexPage> uips = new ArrayList<UserIndexPage>();
		for(String uid: uids){
			Out.println("uid => " + uid);
			UserIndexPage uip = weiboClient.getUserIndexPage(uid);
			if(uip == null) continue;
			uips.add(uip);
			if(++count == 50) {
				Out.println("50 batches saving ...");
				mysql.updateUserIndexPageTable(uips);
				uips.clear();
				count = 0;
			}
		}
		if(count > 0) mysql.updateUserIndexPageTable(uips);
	}

}
