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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.CacheManager;
import android.webkit.WebView;
import android.widget.Toast;

public class SniqueActivity extends Activity {
	private WebView wv;

	private static final byte keyRaw[] = { 0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc,
			(byte) 0xdd, (byte) 0xee, (byte) 0xff };
	private static final byte ivRaw[] = { (byte) 0xff, (byte) 0xee, (byte) 0xdd, (byte) 0xcc, (byte) 0xbb, (byte) 0xaa, (byte) 0x99, (byte) 0x88, 0x77, 0x66,
			0x55, 0x44, 0x33, 0x22, 0x11, 0x00 };
	private static final byte eyecatcher[] = { (byte) 0xFA, (byte) 0xCE, (byte) 0xF0, 0x0D };

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
		this.setTitle(message);
		Toast.makeText(getApplicationContext(), "Hidden message: " + message, Toast.LENGTH_LONG).show();
	}

	public void resetTitle() {
		this.setTitle(R.string.app_name);
	}

	public String decodeData(String html) {
		Log.d("SniqueActivity", "decodeData");
		Pattern findsrc = Pattern.compile("src\\s*=\\s*['\"]([^']*)['\"]");
		Pattern findidpix = Pattern.compile("id\\s*=\\s*['\"]\\s*pix\\s*['\"]");
		Matcher idpixMatcher = findidpix.matcher(html);

		boolean haspix = idpixMatcher.find();
		if (!haspix) {
			Log.d("SniqueActivity", "No picture div");
			return null;
		}
		/*
		 * On some early versions of Android, if we call Matcher.find(offset), and then call Matcher.find(), the second call starts at the beginning of the
		 * input character sequence, rather than continuing from where it found the previous match. This means that we find the first image in the pix div, and
		 * then start finding all the src elements from the top of the file.
		 * 
		 * I think this is due to this issue:
		 * 
		 * http://code.google.com/p/android/issues/detail?id=19308
		 * 
		 * To work around this, we are going to limit the input we provide to the matcher.
		 * 
		 * TODO: Do we want to figure out where the div ends as well? Currently we append any src tags in the rest of the file to our list of URLs, regardless
		 * of whether they came after the close of the div tag.
		 */
		Matcher srcMatcher = findsrc.matcher(html.substring(idpixMatcher.start()));
		boolean hassrc = srcMatcher.find();
		if (!hassrc) {
			Log.d("SniqueActivity", "No src tags in picture div");
			return null;
		}

		List<String> urls = new ArrayList<String>();
		do {
			String srcUrl = srcMatcher.group(1);
			urls.add(srcUrl);
		} while (srcMatcher.find());

		StringBuilder concealedHex = new StringBuilder();
		for (String imgUrl : urls) {
			Log.d("SniqueActivity", imgUrl);
			// CacheManager seems to work well enough to be used for retrieving image data successfully.
			CacheManager.CacheResult imgCr = CacheManager.getCacheFile(imgUrl, null);
			String ETag = null;
			if (imgCr != null) {
				ETag = imgCr.getETag();
			} else {
				// However, if we get here we couldn't find the image in the cache.
				// At least we only need to get the headers, not the entire image.
				Log.d("SniqueActivity", "Image " + imgUrl + " not in cache, downloading headers");
				try {
					HttpURLConnection con = (HttpURLConnection) new URL(imgUrl).openConnection();
					con.connect();
					ETag = con.getHeaderField("ETag");
				} catch (MalformedURLException e) {
					Log.e("SniqueActivity", "MalformedURLException", e);
				} catch (IOException e) {
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
			concealedHex.append(ETag);
		}
		urls = null;
		Log.d("SniqueActivity", "Concealed hex is " + concealedHex);
		byte message[] = new byte[(concealedHex.length() >>> 1) + 1];
		int hexIndex = 0;
		for (int index = 0, i = 0; index < concealedHex.length(); ++index) {
			char hexChar = concealedHex.charAt(index);
			switch (hexChar) {
			case 'F':
			case 'f':
			case 'E':
			case 'e':
			case 'D':
			case 'd':
			case 'C':
			case 'c':
			case 'B':
			case 'b':
			case 'A':
			case 'a':
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
					message[i] = (byte) (0xf << 4);
					break;
				case 'E':
				case 'e':
					message[i] = (byte) (0xe << 4);
					break;
				case 'D':
				case 'd':
					message[i] = (byte) (0xd << 4);
					break;
				case 'C':
				case 'c':
					message[i] = (byte) (0xc << 4);
					break;
				case 'B':
				case 'b':
					message[i] = (byte) (0xb << 4);
					break;
				case 'A':
				case 'a':
					message[i] = (byte) (0xa << 4);
					break;
				case '9':
					message[i] = (byte) (0x9 << 4);
					break;
				case '8':
					message[i] = (byte) (0x8 << 4);
					break;
				case '7':
					message[i] = 0x7 << 4;
					break;
				case '6':
					message[i] = 0x6 << 4;
					break;
				case '5':
					message[i] = 0x5 << 4;
					break;
				case '4':
					message[i] = 0x4 << 4;
					break;
				case '3':
					message[i] = 0x3 << 4;
					break;
				case '2':
					message[i] = 0x2 << 4;
					break;
				case '1':
					message[i] = 0x1 << 4;
					break;
				}
			} else {
				switch (hexChar) {
				case 'F':
				case 'f':
					message[i] |= 0xf;
					break;
				case 'E':
				case 'e':
					message[i] |= 0xe;
					break;
				case 'D':
				case 'd':
					message[i] |= 0xd;
					break;
				case 'C':
				case 'c':
					message[i] |= 0xc;
					break;
				case 'B':
				case 'b':
					message[i] |= 0xb;
					break;
				case 'A':
				case 'a':
					message[i] |= 0xa;
					break;
				case '9':
					message[i] |= 0x9;
					break;
				case '8':
					message[i] |= 0x8;
					break;
				case '7':
					message[i] |= 0x7;
					break;
				case '6':
					message[i] |= 0x6;
					break;
				case '5':
					message[i] |= 0x5;
					break;
				case '4':
					message[i] |= 0x4;
					break;
				case '3':
					message[i] |= 0x3;
					break;
				case '2':
					message[i] |= 0x2;
					break;
				case '1':
					message[i] |= 0x1;
					break;
				}
				++i;
			}
			++hexIndex;
		}
		int messageByteCount = hexIndex >>> 1;
		messageByteCount = (messageByteCount >>> 4) << 4;
		{
			byte newMessage[] = new byte[messageByteCount];
			for (int i = 0; i < newMessage.length; ++i)
				newMessage[i] = message[i];
			message = newMessage;
		}

		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
			SecretKeySpec keyspec = new SecretKeySpec(keyRaw, "AES");
			IvParameterSpec ivspec = new IvParameterSpec(ivRaw);
			cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
			byte[] decrypted = cipher.doFinal(message);
			if ((decrypted[0] == eyecatcher[0]) && (decrypted[1] == eyecatcher[1]) && (decrypted[2] == eyecatcher[2]) && (decrypted[3] == eyecatcher[3])) {
				int length = 0;
				length = (decrypted[4] << 24) | (decrypted[5] << 16) | (decrypted[6] << 8) | decrypted[7];
				if ((length + 8) < decrypted.length) {
					byte decodedMessage[] = new byte[length];
					for (int i = 0; i < decodedMessage.length; ++i)
						decodedMessage[i] = decrypted[8 + i];
					String theMessage = new String(decodedMessage);
					Log.d("SniqueActivity", theMessage);

					// Return decoded message back to main thread.
					return theMessage;
				}
			}
		} catch (NoSuchAlgorithmException e) {
			Log.e("SniqueActivity", "No AES/CBC algorithm", e);
		} catch (NoSuchPaddingException e) {
			Log.e("SniqueActivity", "No NoPadding padding", e);
		} catch (InvalidKeyException e) {
			Log.e("SniqueActivity", "Invalid key", e);
		} catch (InvalidAlgorithmParameterException e) {
			Log.e("SniqueActivity", "Bad algorithm parameter(s)", e);
		} catch (IllegalBlockSizeException e) {
			Log.e("SniqueActivity", "Bad block size", e);
		} catch (BadPaddingException e) {
			Log.e("SniqueActivity", "Bad padding", e);
		}
		return null;
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
			String data = decodeData(html);
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