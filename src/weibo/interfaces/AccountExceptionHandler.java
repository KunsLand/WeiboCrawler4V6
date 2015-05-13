package weibo.interfaces;

import weibo.objects.WeiboAccount;

public interface AccountExceptionHandler {
	
	public void verifycodeException(String account);
	
	public void freezeException(String account);
	
	public void updateCookie(WeiboAccount account);

}
