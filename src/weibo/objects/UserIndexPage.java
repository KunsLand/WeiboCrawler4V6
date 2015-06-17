package weibo.objects;

public class UserIndexPage {
	public String UID = null, NICK_NAME = null, LAST_POST = null;
	public int FOLLOWS = 0, FANS = 0, BLOGS = 0, VERIFIED = 0;

	@Override
	public String toString() {
		return "{ UID: " + UID + ", NICK_NAME: " + NICK_NAME + ", VERIFIED: "
				+ VERIFIED + ", LAST_POST: " + LAST_POST + ", FOLLOWS: "
				+ FOLLOWS + ", FANS: " + FANS + ", BLOGS: " + BLOGS + " }";
	}
}