package com.iflytek.mscdemo;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class CoobyUtils {
	private static final String punctuations = "。？！";

	private static boolean isPunctuation(char c) {
		return punctuations.indexOf(c) >= 0;
	}

	public static String removeLastPunctuation(String src) {
		// String s = "你是谁？";
		String result = src;
		char lastChar = src.charAt(src.length() - 1);
		if (isPunctuation(lastChar))
			result = src.substring(0, src.length() - 1);

		// System.out.println(s);
		return result;
	}
	public static String callCooby(String message) {
		message = removeLastPunctuation(message);

		// ALERT MESSAGE

		String messageValue = null;
		try {

			// URLEncode user defined data

			messageValue = URLEncoder.encode(message, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			// showTip("Fail");
			throw new RuntimeException(ex);
		}
		// Create http client object to send request to server

		HttpClient Client = new DefaultHttpClient();

		// Create URL string

		String URL = "http://cooby.dingjianghao.com/robot1/" + messageValue;

		// Log.i("httpget", URL);

		try {


			// Create Request to server and get response

			// HttpGet httpget = new HttpGet(URL);
			ResponseHandler<String> responseHandler = new BasicResponseHandler();
			// SetServerString = Client.execute(httpget, responseHandler);

			HttpGet httpGet = new HttpGet(URL);
			HttpParams httpParameters = new BasicHttpParams();
			// Set the timeout in milliseconds until a connection is
			// established.
			// The default value is zero, that means the timeout is not
			// used.
			int timeoutConnection = 3000;
			HttpConnectionParams.setConnectionTimeout(httpParameters,
					timeoutConnection);
			// Set the default socket timeout (SO_TIMEOUT)
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = 5000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

			DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
			// HttpResponse response = httpClient.execute(httpGet);

			String resp = Client.execute(httpGet, responseHandler);
			// Show response on activity
			// showTip(SetServerString);

			return resp;
		} catch (Exception ex) {
			
			throw new RuntimeException(ex);
		}

	}
}
