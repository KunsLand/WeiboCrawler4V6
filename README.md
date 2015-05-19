This project is a [Sina Weibo] web crawler which provides two main functions: crawl user index page information and user follows based on known user ids.

# Recommended Java Version
[JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
[What's new in JDK 8](http://www.oracle.com/technetwork/java/javase/8-whats-new-2157071.html)
[Java 8 的新特性和改进总览](http://www.oschina.net/translate/everything-about-java-8)

# NOTICE
This project is just a toy web crawling program. It does not leak any personal information of users involved in the Sina Weibo. Neither does it provide a way to hack the website server. It is, technically, a program which follows strictly the HTTP rules and implemented with Jsoup API. Any request sent by the program can be visited and viewed mannualy through any normal browser.

# How to use this crawler?
#### How to crawl user index page information?
You may think you need a valid, regular, limitless Weibo account to access the user index page. But a critical problem here is that your account will be limited to access the recources from the website server if you send too many requests during a short range of time(see the access [rate-limiting]). This project figured out how to access the user index page as a __VISITOR__ and with no access rate-limiting.

See more in [CrawlUserIndexPage].

#### How to crawl user relation?
Sina Weibo has a strict access rate limiting to prevent any web crawler sending too many requests to the website server. And also if you mannually visit the follows list or fans list of any others, you can only view about 200 of all follows or fans.

This project figured out how to overcome these limitations of accessing the follows list of any accessible user. One thing different from crawling user index page information is that you need a group of registered and valid Sina Weibo accounts and your accounts are highly risk of being __BANNED FOREVER__.

See more in [CrawlFollows].

### Where is the initial user ids from?
The user ids come from the [Hall of Fame]. How did I get all of these user ids? You should figure out by yourself.

[Sina Weibo]:http://weibo.com
[rate-limiting]:http://open.weibo.com/wiki/%E6%8E%A5%E5%8F%A3%E8%AE%BF%E9%97%AE%E9%A2%91%E6%AC%A1%E6%9D%83%E9%99%90
[CrawlUserIndexPage]:https://github.com/KunsLand/WeiboCrawler4V6/blob/master/src/weibo/CrawlUserIndexPage.java
[CrawlFollows]:https://github.com/KunsLand/WeiboCrawler4V6/blob/master/src/weibo/CrawlFollows.java
[Hall of Fame]:http://verified.weibo.com/