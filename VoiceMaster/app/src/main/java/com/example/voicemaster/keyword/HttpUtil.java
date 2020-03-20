package com.example.voicemaster.keyword;

import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

/**
 * Http Client 工具类
 */
public class HttpUtil {

	private static final String TAG = "Cypress";

	/**
	 * 发送post请求，根据 Content-Type 返回不同的返回值
	 * 
	 * @param url
	 * @param header
	 * @param body
	 * @return
	 */
	public static Map<String, Object> doPost2(String url, Map<String, String> header, String body) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		PrintWriter out = null;
		try {
			// 设置 url
			URL realUrl = new URL(url);
			URLConnection connection = realUrl.openConnection();
			HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
			// 设置 header
			for (String key : header.keySet()) {
				httpURLConnection.setRequestProperty(key, header.get(key));
			}
			// 设置请求 body
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setDoInput(true);
			out = new PrintWriter(httpURLConnection.getOutputStream());
			// 保存body
			out.print(body);
			// 发送body
			out.flush();
			if (HttpURLConnection.HTTP_OK != httpURLConnection.getResponseCode()) {
				System.out.println("Http 请求失败，状态码：" + httpURLConnection.getResponseCode());
				return null;
			}
			// 获取响应header
			String responseContentType = httpURLConnection.getHeaderField("Content-Type");
			if ("audio/mpeg".equals(responseContentType)) {
				// 获取响应body
				byte[] bytes = toByteArray(httpURLConnection.getInputStream());
				resultMap.put("Content-Type", "audio/mpeg");
				resultMap.put("sid", httpURLConnection.getHeaderField("sid"));
				resultMap.put("body", bytes);
				return resultMap;
			} else {
				// 设置请求 body
				BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
				String line;
				String result = "";
				while ((line = in.readLine()) != null) {
					result += line;
				}
				resultMap.put("Content-Type", "text/plain");
				resultMap.put("body", result);

				return resultMap;
			}
		} catch (Exception e) {
			return null;
		}
	}


	
	/**
	 * 发送post请求
	 * 
	 * @param url
	 * @param header
	 * @param body
	 * @return
	 */
	public static String doPost1(String url, Map<String, String> header, String body) {
		String result = "";
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			// 设置 url
			URL realUrl = new URL(url);
			URLConnection connection = realUrl.openConnection();
			HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
			// 设置 header
			for (String key : header.keySet()) {
				httpURLConnection.setRequestProperty(key, header.get(key));
			}
			// 设置请求 body
			httpURLConnection.setDoOutput(true);
			httpURLConnection.setDoInput(true);
			Log.d(TAG, "doPost1: 到此");
			out = new PrintWriter(httpURLConnection.getOutputStream());
			Log.d(TAG, "doPost1: 到此1");
			// 保存body
			out.print(body);
			Log.d(TAG, "doPost1: 到此2");
			// 发送body
			out.flush();
			if (HttpURLConnection.HTTP_OK != httpURLConnection.getResponseCode()) {
				Log.d(TAG, "doPost1: " + "Http 请求失败，状态码：" + httpURLConnection.getResponseCode());
				return null;
			}

			// 获取响应body
			in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		} catch (Exception e) {
			Log.d(TAG, "doPost1: 读取失败 " + e.getMessage() +" stringIs: " + e.toString());
			return null;
		}
		return result;
	}

	/**
	 * 流转二进制数组
	 * 
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private static byte[] toByteArray(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024 * 4];
		int n = 0;
		while ((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
		return out.toByteArray();
	}
}
