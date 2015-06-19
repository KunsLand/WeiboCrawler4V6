package weibo.client;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.json.JSONException;

import common.Out;
import common.TimeUtils;

public class AccountQueue implements AccountManager {
	private int current;
	private List<WeiboAccount> accounts;
	private CookieStorage cookieStorage;
	private FaceNameStorage faceName;

	public AccountQueue(CookieStorage cookieStorage, FaceNameStorage faceName) {
		this.accounts = cookieStorage.getAccountList();
		this.cookieStorage = cookieStorage;
		this.faceName = faceName;
		checkCookies();
		current = 0;
	}

	private void checkCookies() {
		Out.println("Number of accounts in queue: " + accounts.size());
		for (int i = 0; i < accounts.size(); i++) {
			Map<String, String> cookie = accounts.get(i).COOKIES;
			if (cookie == null || cookie.isEmpty()) {
				Out.println("Empty cookie: " + accounts.get(i).USERNAME);
				refreshCookie(accounts.get(i));
			}
		}
		Out.println("Number of accounts in queue: " + accounts.size());
	}

	@Override
	public synchronized boolean refreshCookie(WeiboAccount account) {
		Map<String, String> cookie = null;
		while (true) {
			try {
				cookie = Account.newCookie(account.USERNAME, account.PASSWORD);
				break;
			} catch (IOException | JSONException e) {
				Out.println(e.getMessage());
				TimeUtils.Pause(RequestConfig.TIME_REQUEST_GAP);
			}
		}
		if (cookie == null) {
			removeAccount(account);
			return false;
		} else {
			account.COOKIES = cookie;
			cookieStorage.updateCookie(account);
			return true;
		}
	}

	@Override
	public synchronized WeiboAccount getNextAccount() {
		if (isEmpty())
			return null;
		current = ++current % accounts.size();
		return accounts.get(current);
	}

	@Override
	public synchronized boolean removeAccount(WeiboAccount account) {
		if (!isEmpty()) {
			Out.println("Remove account from queue: " + account.USERNAME);
			accounts.remove(account);
			cookieStorage.invalidateCookie(account);
			if (current >= accounts.size()) {
				current = 0;
			}
			Out.println("Number of accounts in queue: " + accounts.size());
			return true;
		}
		return false;
	}

	@Override
	public synchronized boolean isEmpty() {
		return accounts.isEmpty();
	}

	@Override
	public synchronized WeiboAccount peekAccount() {
		if (!isEmpty()) {
			return accounts.get(current);
		}
		return null;
	}

	@Override
	public synchronized boolean unfreezeAccount(WeiboAccount account) {
		if(Unfreeze.unfreezeAccount(account, faceName)){
			cookieStorage.updateCookie(account);
			return true;
		}
		return false;
	}

}
