package com.nomzit.snique;

import static com.nomzit.snique.Utilities.extractBytesFromHexInString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.webkit.CacheManager;
import android.webkit.WebView;

public class SniqueActivity extends Activity {
	private WebView wv;
	private Messenger findMessageServiceMessenger;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		bindService(new Intent(this, LookForSniqueMessageService.class), new ServiceConnection()
		{
			
			public void onServiceDisconnected(ComponentName name)
			{
				findMessageServiceMessenger = null;
			}
			
			public void onServiceConnected(ComponentName name, IBinder service)
			{
				findMessageServiceMessenger = new Messenger(service);
				String url = "http://blog.nomzit.com/snique/";
				new NetworkTask().execute(url);
			}
		},
	            Context.BIND_AUTO_CREATE);

		this.setTitle(R.string.app_name);

		wv = (WebView) findViewById(R.id.webView1);
		wv.getSettings().setJavaScriptEnabled(true);
		SniqueWebViewClient wvc = new SniqueWebViewClient(this);
		wv.setWebViewClient(wvc);
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

	public void resetTitle()
	{
		this.setTitle(((WebView)findViewById(R.id.webView1)).getTitle());
	}

	public void decodeData(String html)
	{
		Log.d("SniqueActivity", "decodeData");
		Pattern findsrc = Pattern.compile("src\\s*=\\s*['\"]([^']*)['\"]");

		Matcher srcMatcher = findsrc.matcher(html);
		boolean hassrc = srcMatcher.find();
		if (!hassrc) {
			Log.d("SniqueActivity", "No src tags");
			return;
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
			byte newFragment[] = extractBytesFromHexInString(etag);
			if (newFragment.length > 0)
				message[messageIndex++] = newFragment;
		}
		CodedMessage coded = new CodedMessage(message);
		Message msg = Message.obtain(null, LookForSniqueMessageService.LOOK_FOR_SNIQUE_MESSAGE);
		Bundle bundle = new Bundle();
		coded.addToBundle(bundle);
		msg.setData(bundle);
		try
		{
			findMessageServiceMessenger.send(msg);
		}
		catch (RemoteException e)
		{
			Log.e("SniqueActivity","Remote Exception",e);
		}
	}

	protected class NetworkTask extends AsyncTask<String, Void, Void>
	{
		@Override
		protected Void doInBackground(String... params)
		{
			// Retrieve HTML source from URL being loaded
			String url = params[0];
			String html = null;
			String mimeType = "text/html";
			String charSet = "US-ASCII";
			try
			{
				HttpClient client = new DefaultHttpClient();
				String proxyString = Settings.Secure.getString(getApplicationContext().getContentResolver(),                                                     Settings.Secure.HTTP_PROXY);
				 
				if (proxyString != null)
				{      
				        String proxyAddress = proxyString.split(":")[0];
				        int proxyPort = Integer.parseInt(proxyString.split(":")[1]);
				        HttpHost proxy = new HttpHost(proxyAddress,proxyPort);
				        client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
				}
				HttpGet request = new HttpGet(url);
				Header acceptEncoding = new BasicHeader("Accept-Encoding","gzip;q=1.0, identity;q=0.5, deflate;q=0.1, *;q=0");
				request.setHeader(acceptEncoding);
				HttpResponse response = client.execute(request);
				Header headers[] = response.getHeaders("content-type");
				for (Header contentType: headers)
				{
					String value = contentType.getValue();
					
					Pattern findMime = Pattern.compile("\\s*([^;\\s]*)");
					Matcher mimeMatcher = findMime.matcher(value);
					boolean hasMime = mimeMatcher.find();
					if (hasMime)
					{
						mimeType = mimeMatcher.group(1);
					}
					else
					{
						Log.i("SniqueActivity", "No MIME type in response for url "+ url);
					}

					Pattern findCharset = Pattern.compile(";\\s*charset\\s*=\\s*([^;\\s]*)");
					Matcher charsetMatcher = findCharset.matcher(value);
					boolean hasCharset = charsetMatcher.find();
					if (hasCharset)
					{
						charSet = charsetMatcher.group(1);
					}
					else
					{
						Log.i("SniqueActivity", "No character set in response for url "+ url);
					}
				}
				
				boolean hasGzip = false;
				boolean hasDeflate = false;
				headers = response.getHeaders("content-encoding");
				for (Header contentEncoding: headers)
				{
					String value = contentEncoding.getValue();
					
					Pattern findGzip = Pattern.compile("\\s*(gzip)",Pattern.CASE_INSENSITIVE);
					Matcher gzipMatcher = findGzip.matcher(value);
					hasGzip = gzipMatcher.find();
					Log.i("SniqueActivity", "gzip content-encoding? "+ hasGzip);

					Pattern findDeflate = Pattern.compile("\\s*(deflate)",Pattern.CASE_INSENSITIVE);
					Matcher deflateMatcher = findDeflate.matcher(value);
					hasDeflate = deflateMatcher.find();
					Log.i("SniqueActivity", "deflate content-encoding? "+ hasDeflate);
					
					if (!(hasGzip || hasDeflate))
						Log.e("SniqueActivity", "Cannot process content-encoding: "+ value);
				}

				InputStream in = response.getEntity().getContent();
				if (hasGzip)
					in = new GZIPInputStream(in);
				else if (hasDeflate)
					in = new InflaterInputStream(in);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in,Charset.forName(charSet)));
				StringBuilder str = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					str.append(line);
				}
				in.close();
				html = str.toString();
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
			}

			if (html == null)
			{
				return null;
			}

			// Display page in WebView
			wv.loadDataWithBaseURL(url, html, mimeType, charSet, null);

			// Check for data to decode
			decodeData(html);
			return null;
		}
	}
}