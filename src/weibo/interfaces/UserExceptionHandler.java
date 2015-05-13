package weibo.interfaces;

public interface UserExceptionHandler {
	
	public void userNotAvailable(String uid);
	
	public void enterpriseUser(String uid);
}
