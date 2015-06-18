package weibo.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import common.Out;

public class VisitorQueue implements AccountManager {
	private int current;
	private List<WeiboAccount> visitors;

	public VisitorQueue(int n) {
		List<Map<String, String>> cookies = Visitor.newVisitorCookies(n);
		visitors = new ArrayList<WeiboAccount>();
		for (int i = 0; i < cookies.size(); i++) {
			WeiboAccount acc = new WeiboAccount();
			acc.USERNAME = "Visitor-" + i;
			acc.COOKIES = cookies.get(i);
			visitors.add(acc);
		}
		current = 0;
	}

	@Override
	public synchronized boolean refreshCookie(WeiboAccount visitor) {
		Map<String, String> cookie = null;
		try {
			cookie = Visitor.newVisitorCookie();
		} catch (IOException e) {
			Out.println(e.getMessage());
		}
		if (cookie == null) {
			removeAccount(visitor);
			return false;
		} else {
			visitor.COOKIES = cookie;
			return true;
		}
	}

	@Override
	public synchronized WeiboAccount getNextAccount() {
		if (isEmpty()) {
			return null;
		}
		current = ++current % visitors.size();
		return visitors.get(current);
	}

	@Override
	public synchronized boolean removeAccount(WeiboAccount visitor) {
		if (!isEmpty()) {
			visitors.remove(visitor);
			if (current >= visitors.size()) {
				current = 0;
			}
			return true;
		}
		return false;
	}

	@Override
	public synchronized boolean isEmpty() {
		return visitors.isEmpty();
	}

	@Override
	public synchronized WeiboAccount peekAccount() {
		if (!isEmpty()) {
			return visitors.get(current);
		}
		return null;
	}

}
