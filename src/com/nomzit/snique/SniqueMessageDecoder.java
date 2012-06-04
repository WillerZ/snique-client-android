package com.nomzit.snique;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import android.util.Log;

public class SniqueMessageDecoder
{
	SecretKeySpec keySpec;
	Cipher cipher;
	int blockSize;
	
	private static final byte eyecatcher[] = { (byte) 0xFA, (byte) 0xCE, (byte) 0xF0, 0x0D };

	public SniqueMessageDecoder(byte key[]) throws WillNeverWorkException
	{
		keySpec = new SecretKeySpec(key, "AES");
		try
		{
			cipher = Cipher.getInstance("AES/CBC/NoPadding");
		}
		catch (NoSuchAlgorithmException e)
		{
			Log.e("SniqueMessageDecoder", "No AES/CBC algorithm", e);
			throw new WillNeverWorkException(e);
		}
		catch (NoSuchPaddingException e)
		{
			Log.e("SniqueMessageDecoder", "No NoPadding padding", e);
			throw new WillNeverWorkException(e);
		}

		blockSize = key.length;
		Log.d("SniqueMessageDecoder", "Block size is " + blockSize);
	}
	
	private String decodeMessage(byte[] ivRaw, byte[] coded) throws NoMessageException, WillNeverWorkException, InvalidKeyException
	{
		try
		{
			IvParameterSpec ivspec = new IvParameterSpec(ivRaw);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivspec);
			byte[] decrypted = cipher.doFinal(coded);
			if ((decrypted[0] == eyecatcher[0]) && (decrypted[1] == eyecatcher[1]) && (decrypted[2] == eyecatcher[2]) && (decrypted[3] == eyecatcher[3]))
			{
				int length = 0;
				Log.d("SniqueMessageDecoder", "Eyecatcher matched");
				length = (decrypted[4] << 24) | (decrypted[5] << 16) | (decrypted[6] << 8) | decrypted[7];
				Log.d("SniqueMessageDecoder", "Length is " + length);
				Log.d("SniqueMessageDecoder", "Decrypted message length is " + (decrypted.length - 8));
				if ((length + 8) < decrypted.length)
				{
					byte decodedMessage[] = new byte[length];
					for (int i = 0; i < decodedMessage.length; ++i)
						decodedMessage[i] = decrypted[8 + i];
					String theMessage = new String(decodedMessage);
					Log.d("SniqueMessageDecoder", "Hidden message is " + theMessage);

					// Return decoded message back to main thread.
					return theMessage;
				}
			}
			return null;
		}
		catch (InvalidAlgorithmParameterException e)
		{
			Log.e("SniqueMessageDecoder", "Bad algorithm parameter(s)", e);
			throw new WillNeverWorkException(e);
		}
		catch (IllegalBlockSizeException e)
		{
			Log.e("SniqueMessageDecoder", "Bad block size", e);
			throw new WillNeverWorkException(e);
		}
		catch (BadPaddingException e)
		{
			Log.e("SniqueMessageDecoder", "Bad padding", e);
			throw new WillNeverWorkException(e);
		}
	}
	
	private static byte[][] arrayByRemovingFirstItem(byte[][] arr)
	{
		byte out[][] = new byte[arr.length - 1][];
		for (int i = 1; i<arr.length; ++i)
			out[i-1] = arr[i];
		return out;
	}
	
	private static byte[] flattenArray(byte[][] arr, int multipleOf)
	{
		int total = 0;
		for (byte[] a : arr)
			total += a.length;
		total /= multipleOf;
		total *= multipleOf;
		byte out[] = new byte[total];
		int outIndex = 0;
		for (byte[] a : arr)
			for (int i=0; i<a.length && outIndex < out.length;++i,++outIndex)
				out[outIndex] = a[i];
		return out;
	}

	public String decodeMessage(byte[][] coded) throws NoMessageException, WillNeverWorkException, InvalidKeyException
	{
		byte ivRaw[] = new byte[blockSize];
		outer:
		while (coded.length > 0)
		{
			int ivIndex = 0;
			byte remCoded[][] = coded;
			coded = arrayByRemovingFirstItem(coded);
			while (ivIndex < blockSize)
			{
				byte first[] = remCoded[0];
				remCoded = arrayByRemovingFirstItem(remCoded);
				if (remCoded.length == 0)
					break outer;
				for (int i = 0; i < first.length && ivIndex < blockSize; ++i, ++ivIndex)
					ivRaw[ivIndex] = first[i];
			}
			byte flat[] = flattenArray(remCoded,blockSize);
			if (flat.length == 0)
				break outer;
			String decoded = decodeMessage(ivRaw,flat);
			if (decoded != null)
				return decoded;
		}
		throw new NoMessageException();
	}
}
