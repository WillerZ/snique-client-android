package com.nomzit.snique;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CacheManager;
import android.webkit.WebView;

public class SniqueActivity extends Activity {
	private WebView wv;

	private static final byte keyRaw[] = { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc,
			(byte) 0xdd, (byte) 0xee, (byte) 0xff };

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		this.setTitle(R.string.app_name);

		wv = (WebView) findViewById(R.id.webView1);
		wv.getSettings().setJavaScriptEnabled(true);
		SniqueWebViewClient wvc = new SniqueWebViewClient(this);
		wv.setWebViewClient(wvc);

		String url = "http://blog.nomzit.com/snique/";

		// Get HTML and decode outside of UI thread to avoid blocking UI.
		new NetworkTask().execute(url);
	}

	protected void pageLoading(SniqueWebViewClient wvc, Bitmap favicon) {
		try {
			Method getActionBarMethod = this.getClass().getMethod("getActionBar", (Class<?>) null);
			Object actionBar = getActionBarMethod.invoke(this, (Object) null);
			Method setIconMethod = actionBar.getClass().getMethod("setIcon", Drawable.class);
			setIconMethod.invoke(actionBar, new BitmapDrawable(favicon));
		} catch (SecurityException e) {
			Log.e("SniqueActivity", "Security exception getting getActionBar() method", e);
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
			Log.e("SniqueActivity", "Illegal argument exception calling getActionBar() method", e);
		} catch (IllegalAccessException e) {
			Log.e("SniqueActivity", "Illegal access exception calling getActionBar() method", e);
		} catch (InvocationTargetException e) {
			Log.e("SniqueActivity", "Invocation target exception calling getActionBar() method", e);
		}
	}

	private void displayData(String message) {
		NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		Notification notification = new Notification();
		notification.when = System.currentTimeMillis();
		notification.defaults = Notification.DEFAULT_ALL;
		notification.flags = Notification.FLAG_AUTO_CANCEL;
		notification.icon = R.drawable.ic_launcher;
		notification.setLatestEventInfo(getApplicationContext(), "snique", message, null);
		notificationManager.notify(1, notification);
	}

	public void resetTitle() {
		this.setTitle(R.string.app_name);
	}

	public String decodeData(String html) throws InvalidKeyException, NoMessageException, WillNeverWorkException
	{
		Log.d("SniqueActivity", "decodeData");
		Pattern findsrc = Pattern.compile("src\\s*=\\s*['\"]([^']*)['\"]");

		Matcher srcMatcher = findsrc.matcher(html);
		boolean hassrc = srcMatcher.find();
		if (!hassrc) {
			Log.d("SniqueActivity", "No src tags");
			return null;
		}

		List<String> urls = new ArrayList<String>();
		do
		{
			String srcUrl = srcMatcher.group(1);
			urls.add(srcUrl);
		}
		while (srcMatcher.find());

		List<String> etags = new ArrayList<String>();
		for (String imgUrl : urls)
		{
			Log.d("SniqueActivity", imgUrl);
			// CacheManager seems to work well enough to be used for retrieving image data successfully.
			CacheManager.CacheResult imgCr = CacheManager.getCacheFile(imgUrl, null);
			String ETag = null;
			if (imgCr != null)
			{
				ETag = imgCr.getETag();
			}
			else
			{
				// However, if we get here we couldn't find the image in the cache.
				// At least we only need to get the headers, not the entire image.
				Log.d("SniqueActivity", "Image " + imgUrl + " not in cache, downloading headers");
				try
				{
					HttpURLConnection con = (HttpURLConnection) new URL(imgUrl).openConnection();
					con.connect();
					ETag = con.getHeaderField("ETag");
				}
				catch (MalformedURLException e)
				{
					Log.e("SniqueActivity", "MalformedURLException", e);
				} 
				catch (IOException e) 
				{
					Log.e("SniqueActivity", "IOException", e);
				}
			}

			if (ETag == null)
				continue;
			Log.d("SniqueActivity", ETag);
			if ((ETag.charAt(0) == 'W') || (ETag.charAt(0) == 'w'))
				ETag = ETag.substring(2, ETag.length() - 1);
			else
				ETag = ETag.substring(1, ETag.length() - 1);
			etags.add(ETag);
		}
		urls = null;
		Log.d("SniqueActivity", "etags are" + etags);
		byte message[][] = new byte[etags.size()][];
		int messageIndex = 0;
		for (String etag:etags)
		{
			byte fragment[] = new byte[etag.length()/2 + 1];
			int hexIndex = 0;
			for (int index = 0, i = 0; index < etag.length(); ++index) {
				char hexChar = etag.charAt(index);
				switch (hexChar)
				{
				case 'F': case 'f':
				case 'E': case 'e':
				case 'D': case 'd':
				case 'C': case 'c':
				case 'B': case 'b':
				case 'A': case 'a':
				case '9':
				case '8':
				case '7':
				case '6':
				case '5':
				case '4':
				case '3':
				case '2':
				case '1':
				case '0':
					break;
				default:
					continue;
				}
				if ((hexIndex & 1) == 0) {
					switch (hexChar) {
					case 'F':
					case 'f':
						fragment[i] = (byte) (0xf << 4);
						break;
					case 'E':
					case 'e':
						fragment[i] = (byte) (0xe << 4);
						break;
					case 'D':
					case 'd':
						fragment[i] = (byte) (0xd << 4);
						break;
					case 'C':
					case 'c':
						fragment[i] = (byte) (0xc << 4);
						break;
					case 'B':
					case 'b':
						fragment[i] = (byte) (0xb << 4);
						break;
					case 'A':
					case 'a':
						fragment[i] = (byte) (0xa << 4);
						break;
					case '9':
						fragment[i] = (byte) (0x9 << 4);
						break;
					case '8':
						fragment[i] = (byte) (0x8 << 4);
						break;
					case '7':
						fragment[i] = 0x7 << 4;
						break;
					case '6':
						fragment[i] = 0x6 << 4;
						break;
					case '5':
						fragment[i] = 0x5 << 4;
						break;
					case '4':
						fragment[i] = 0x4 << 4;
						break;
					case '3':
						fragment[i] = 0x3 << 4;
						break;
					case '2':
						fragment[i] = 0x2 << 4;
						break;
					case '1':
						fragment[i] = 0x1 << 4;
						break;
					}
				} else {
					switch (hexChar) {
					case 'F':
					case 'f':
						fragment[i] |= 0xf;
						break;
					case 'E':
					case 'e':
						fragment[i] |= 0xe;
						break;
					case 'D':
					case 'd':
						fragment[i] |= 0xd;
						break;
					case 'C':
					case 'c':
						fragment[i] |= 0xc;
						break;
					case 'B':
					case 'b':
						fragment[i] |= 0xb;
						break;
					case 'A':
					case 'a':
						fragment[i] |= 0xa;
						break;
					case '9':
						fragment[i] |= 0x9;
						break;
					case '8':
						fragment[i] |= 0x8;
						break;
					case '7':
						fragment[i] |= 0x7;
						break;
					case '6':
						fragment[i] |= 0x6;
						break;
					case '5':
						fragment[i] |= 0x5;
						break;
					case '4':
						fragment[i] |= 0x4;
						break;
					case '3':
						fragment[i] |= 0x3;
						break;
					case '2':
						fragment[i] |= 0x2;
						break;
					case '1':
						fragment[i] |= 0x1;
						break;
					}
					++i;
				}
				++hexIndex;
			}
			int fragmentByteCount = hexIndex >>> 1;
			byte newFragment[] = new byte[fragmentByteCount];
			for (int i = 0; i < newFragment.length; ++i)
				newFragment[i] = fragment[i];
			if (newFragment.length > 0)
				message[messageIndex++] = newFragment;
		}
		SniqueMessageDecoder decoder = new SniqueMessageDecoder(keyRaw);
		return decoder.decodeMessage(message);
	}

	protected class NetworkTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... params) {
			// Retrieve HTML source from URL being loaded
			String url = params[0];
			String html = null;
			try {
				HttpClient client = new DefaultHttpClient();
				HttpGet request = new HttpGet(url);
				HttpResponse response = client.execute(request);

				InputStream in = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				StringBuilder str = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					str.append(line);
				}
				in.close();
				html = str.toString();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}

			if (html == null) {
				return null;
			}

			// Display page in WebView
			// TODO: Remove hard-coded mime-type and encoding values!
			wv.loadDataWithBaseURL(url, html, "text/html", "US-ASCII", null);

			// Check for data to decode
			String data = null;
			try
			{
				data = decodeData(html);
			}
			catch (InvalidKeyException e)
			{
				Log.e("SniqueActivity - Network Task", "Invalid Key", e);
			}
			catch (NoMessageException e)
			{
				Log.d("SniqueActivity - Network Task", "No Message");
			}
			catch (WillNeverWorkException e)
			{
				Log.e("SniqueActivity - Network Task", "Decoding will never work", e);
			}
			return data;
		}

		@Override
		protected void onPostExecute(String message) {
			if (message != null) {
				// We have a decoded message, return to UI thread
				displayData(message);
			} else {
				// No message detected, so reset the title of the activity
				resetTitle();
			}
		}
	}
}