package com.nomzit.snique;

import java.io.IOException;
import java.io.InputStream;
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

import android.graphics.Bitmap;
import android.util.Log;
import android.webkit.CacheManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SniqueWebViewClient extends WebViewClient {

	Pattern findsrc;
	Pattern findidpix;
	SniqueActivity activity;
	static final byte keyRaw[] =
		{
		0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77, (byte) 0x88, (byte) 0x99, (byte) 0xaa, (byte) 0xbb, (byte) 0xcc, (byte) 0xdd, (byte) 0xee, (byte) 0xff
		};
	static final byte ivRaw[] =
		{
		(byte) 0xff, (byte) 0xee, (byte) 0xdd, (byte) 0xcc, (byte) 0xbb, (byte) 0xaa, (byte) 0x99, (byte) 0x88, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11, 0x00
		};
	static final byte eyecatcher[] = 
		{
		(byte) 0xFA,(byte) 0xCE,(byte) 0xF0,0x0D
		};
	public SniqueWebViewClient(SniqueActivity act)
	{
		super();
		activity = act;
		findsrc = Pattern.compile("src\\s*=\\s*['\"]([^']*)['\"]");
		findidpix = Pattern.compile("id\\s*=\\s*['\"]\\s*pix\\s*['\"]");
	}

	@Override
	public void onPageFinished(WebView view, String url)
	{
		Log.d("SniqueWebViewClient", "onPageFinished");
		CacheManager.CacheResult cr = CacheManager.getCacheFile(url, null);
		String encoding = "UTF-8";
		List<String> urls = new ArrayList<String>();
		try
		{
			InputStream ins = cr.getInputStream();
			int bufferLength = ins.available();
			byte buffer[] = new byte[bufferLength];
			cr.getInputStream().read(buffer);
			String content = new String(buffer,encoding);
			Matcher idpixMatcher = findidpix.matcher(content);
			boolean haspix = idpixMatcher.find();
			if (!haspix)
				return;
			Matcher srcMatcher = findsrc.matcher(content);
			boolean hassrc = srcMatcher.find(idpixMatcher.start());
			if (!hassrc)
				return;
			do
			{
				String srcUrl = srcMatcher.group(1);
				urls.add(srcUrl);
			}
			while (srcMatcher.find());
		}
		catch (IOException e)
		{
			Log.e("SniqueWebViewClient", "Cannot read cached content for " + url + " because " + e.getMessage() + "\n" + e.toString());
		}
		StringBuilder concealedHex = new StringBuilder();
		for (String imgUrl : urls)
		{
			CacheManager.CacheResult imgCr = CacheManager.getCacheFile(imgUrl, null);
			if (imgCr == null)
				continue;
			String ETag = imgCr.getETag();
			if (ETag == null)
				continue;
			if ((ETag.charAt(0) == 'W') || (ETag.charAt(0) == 'w'))
				ETag = ETag.substring(2, ETag.length() - 1);
			else
				ETag = ETag.substring(1, ETag.length() - 1);
			concealedHex.append(ETag);
		}
		urls = null;
		Log.d("SniqueWebViewClient", "Concealed hex is " + concealedHex);
		byte message[] = new byte[(concealedHex.length() >>> 1) + 1];
		int hexIndex = 0;
		for (int index = 0, i = 0; index < concealedHex.length(); ++index)
		{
			char hexChar = concealedHex.charAt(index);
			switch (hexChar)
			{
			case 'F': case 'f': case 'E': case 'e': case 'D': case 'd': case 'C': case 'c':
			case 'B': case 'b': case 'A': case 'a': case '9': case '8': case '7': case '6':
			case '5': case '4': case '3': case '2': case '1': case '0': break;
			default: continue;
			}
			if ((hexIndex & 1) == 0)
			{
				switch (hexChar)
				{
				case 'F': case 'f': message[i] = (byte) (0xf << 4); break;  
				case 'E': case 'e': message[i] = (byte) (0xe << 4); break;  
				case 'D': case 'd': message[i] = (byte) (0xd << 4); break;  
				case 'C': case 'c': message[i] = (byte) (0xc << 4); break;  
				case 'B': case 'b': message[i] = (byte) (0xb << 4); break;  
				case 'A': case 'a': message[i] = (byte) (0xa << 4); break;
				case '9': message[i] = (byte) (0x9 << 4); break;
				case '8': message[i] = (byte) (0x8 << 4); break;
				case '7': message[i] = 0x7 << 4; break;
				case '6': message[i] = 0x6 << 4; break;
				case '5': message[i] = 0x5 << 4; break;
				case '4': message[i] = 0x4 << 4; break;
				case '3': message[i] = 0x3 << 4; break;
				case '2': message[i] = 0x2 << 4; break;
				case '1': message[i] = 0x1 << 4; break;
				}
			}
			else
			{
				switch (hexChar)
				{
				case 'F': case 'f': message[i] |= 0xf; break;  
				case 'E': case 'e': message[i] |= 0xe; break;  
				case 'D': case 'd': message[i] |= 0xd; break;  
				case 'C': case 'c': message[i] |= 0xc; break;  
				case 'B': case 'b': message[i] |= 0xb; break;  
				case 'A': case 'a': message[i] |= 0xa; break;
				case '9': message[i] |= 0x9; break;
				case '8': message[i] |= 0x8; break;
				case '7': message[i] |= 0x7; break;
				case '6': message[i] |= 0x6; break;
				case '5': message[i] |= 0x5; break;
				case '4': message[i] |= 0x4; break;
				case '3': message[i] |= 0x3; break;
				case '2': message[i] |= 0x2; break;
				case '1': message[i] |= 0x1; break;
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

				try
				{
					Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
					SecretKeySpec keyspec = new SecretKeySpec(keyRaw, "AES");
					IvParameterSpec ivspec = new IvParameterSpec(ivRaw);
					cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
					byte[] decrypted = cipher.doFinal(message);
					if ((decrypted[0] == eyecatcher[0]) &&
							(decrypted[1] == eyecatcher[1]) &&
							(decrypted[2] == eyecatcher[2]) &&
							(decrypted[3] == eyecatcher[3]))
					{
						int length = 0;
						length = (decrypted[4] << 24) |
								(decrypted[5] << 16) |
								(decrypted[6] << 8) |
								decrypted[7];
						if ((length + 8) < decrypted.length)
						{
							byte decodedMessage[] = new byte[length];
							for (int i = 0; i < decodedMessage.length; ++i)
								decodedMessage[i] = decrypted[8+i];
							String theMessage = new String(decodedMessage);
							activity.didDecodeMessage(this, theMessage);
						}
					}
				}
				catch (NoSuchAlgorithmException e)
				{
					Log.e("SniqueWebViewClient", "No AES/CBC algorithm", e);
				}
				catch (NoSuchPaddingException e)
				{
					Log.e("SniqueWebViewClient", "No NoPadding padding", e);
				}
				catch (InvalidKeyException e)
				{
					Log.e("SniqueWebViewClient", "Invalid key", e);
				}
				catch (InvalidAlgorithmParameterException e)
				{
					Log.e("SniqueWebViewClient", "Bad algorithm parameter(s)", e);
				}
				catch (IllegalBlockSizeException e)
				{
					Log.e("SniqueWebViewClient", "Bad block size", e);
				}
				catch (BadPaddingException e)
				{
					Log.e("SniqueWebViewClient", "Bad padding", e);
				}
	}

	@Override
	public void onPageStarted (WebView view, String url, Bitmap favicon)
	{
		activity.pageLoading(this,favicon);
	}
}
