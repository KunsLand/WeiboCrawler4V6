package com.cloudsightapi;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class CloudSight {

	public static String solveCaptcha(String captchaUrl, Object... args)
			throws ClientProtocolException, IOException, JSONException,
			InterruptedException {
		if (args.length > 1
				|| (args.length == 1 && !(args[0] instanceof HashMap))) {
			throw new IllegalArgumentException();
		}
		String url = "https://api.cloudsightapi.com/image_requests";
		CloseableHttpClient httpClient = HttpClients.createDefault();

		HttpGet getCaptcha = new HttpGet(captchaUrl);
		if (args.length == 1) {
			getCaptcha.addHeader(
					"Cookie",
					args[0].toString().replaceAll(",\\s", "; ")
							.replaceAll("{|}", ""));
		}
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.addTextBody("image_request[locale]", "en-US");
		entityBuilder.addTextBody("image_request[language]", "en-US");
		entityBuilder.addBinaryBody("image_request[image]",
				httpClient.execute(getCaptcha).getEntity().getContent(),
				ContentType.MULTIPART_FORM_DATA, "captcha.png");

		HttpPost httpPost = new HttpPost(url);
		httpPost.setEntity(entityBuilder.build());
		httpPost.addHeader("Authorization", "CloudSight amZd_zG32VK-AoSz05JLIA");
		CloseableHttpResponse res = httpClient.execute(httpPost);

		JSONObject obj = new JSONObject(EntityUtils.toString(res.getEntity()));
		String token = obj.getString("token");

		int count = 0;
		String code = "";
		while (true) {
			count++;
			HttpGet httpGet = new HttpGet(
					"https://api.cloudsightapi.com/image_responses/" + token);
			httpGet.addHeader("Authorization",
					"CloudSight amZd_zG32VK-AoSz05JLIA");
			res = httpClient.execute(httpGet);
			// System.out.println(res.getStatusLine());
			obj = new JSONObject(EntityUtils.toString(res.getEntity()));
			String status = obj.getString("status");
			if (status.equalsIgnoreCase("completed")) {
				code = obj.getString("name");
				break;
			} else if (count >= 30) {
				break;
			} else {
				Thread.sleep(2 * 1000);
			}
		}
		httpClient.close();
		return code
				.replaceAll(
						"cap(t?)cha|verification|code|red|blue|pink|image|logo|\\s",
						"");
	}

	public static void main(String[] args) throws ClientProtocolException,
			IOException, JSONException, InterruptedException {
		String url = "http://login.sina.com.cn/cgi/pin.php";
		for (int i = 0; i < 100; i++)
			System.out.println(solveCaptcha(url));
	}

}
