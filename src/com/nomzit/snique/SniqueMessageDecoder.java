package com.nomzit.snique;

import static com.nomzit.snique.Utilities.subArrayOfArrayFromIndexClippedToMultiple;
import static com.nomzit.snique.Utilities.subArrayOfArrayToIndex;

import java.io.UnsupportedEncodingException;
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

		blockSize = 16;
	}
	
	private SniqueMessage decodeMessage(byte[] ivRaw, byte[] coded) throws NoMessageException, WillNeverWorkException, InvalidKeyException
	{
		try
		{
			IvParameterSpec ivspec = new IvParameterSpec(ivRaw);
			byte[] decrypted = null;
			synchronized(cipher)
			{
				cipher.init(Cipher.DECRYPT_MODE, keySpec, ivspec);
				decrypted = cipher.doFinal(coded);
			}
			if ((decrypted[0] == eyecatcher[0]) && (decrypted[1] == eyecatcher[1]) && (decrypted[2] == eyecatcher[2]) && (decrypted[3] == eyecatcher[3]))
			{
				int length = 0;
//				Log.d("SniqueMessageDecoder", "Eyecatcher matched");
				length = (decrypted[4] << 24) | (decrypted[5] << 16) | (decrypted[6] << 8) | decrypted[7];
//				Log.d("SniqueMessageDecoder", "Length is " + length);
//				Log.d("SniqueMessageDecoder", "Decrypted message length is " + (decrypted.length - 8));
				if ((length + 8) < decrypted.length)
				{
					byte decodedMessage[] = new byte[length];
					for (int i = 0; i < decodedMessage.length; ++i)
						decodedMessage[i] = decrypted[8 + i];
					String theMessage;
					try
					{
						theMessage = new String(decodedMessage,"UTF-8");
					}
					catch (UnsupportedEncodingException e)
					{
						throw new WillNeverWorkException(e);
					}
//					Log.d("SniqueMessageDecoder", "Hidden message is " + theMessage);
					int id = ivRaw[0];
					id <<= 8;
					id |= ivRaw[1];
					id <<= 8;
					id |= ivRaw[2];
					id <<= 8;
					id |= ivRaw[3];
					// Return decoded message back to main thread.
					return new SniqueMessage(id,theMessage);
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
	
	public SniqueMessage decodeMessage(CodedMessage coded) throws NoMessageException, WillNeverWorkException, InvalidKeyException
	{
		for (byte candidate[]:coded)
		{
			if (candidate.length < blockSize * 2)
				break;
			byte ivRaw[] = subArrayOfArrayToIndex(candidate, blockSize);
			byte flat[] = subArrayOfArrayFromIndexClippedToMultiple(candidate, blockSize, blockSize);
			SniqueMessage decoded = decodeMessage(ivRaw,flat);
			if (decoded != null)
				return decoded;
		}
		throw new NoMessageException();
	}
}
