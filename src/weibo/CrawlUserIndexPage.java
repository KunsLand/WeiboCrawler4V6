package weibo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import common.Out;

public class CrawlUserIndexPage {
	public static void main(String[] args)
			throws SQLException, JSONException, IOException {
		final MySQLDataBase mysql = new MySQLDataBase();
		List<String> uids = mysql.getIndexPageNotCrawledUIDs();
		Map<String, WeiboAccount> accs = mysql.getAvailableWeiboAccounts();
		WeiboClient weiboClient = new WeiboClient(accs,new ExceptionHandler(){
			@Override
			public void userNotAvailable(String uid) {
				mysql.setUserNotAvailable(uid);
			}

			@Override
			public void verifycodeException(String account) {
				mysql.setAccountVerifyCodeTime(account);
			}

			@Override
			public void freezeException(String account) {
				mysql.setAccountFreezed(account);
			}
		});
		
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
