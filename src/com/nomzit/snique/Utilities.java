package com.nomzit.snique;

public class Utilities
{	
	public static final byte[] subArrayOfArrayToIndex(byte src[], int index)
	{
		int length = src.length - index;
		if (length < 0)
			throw new ArrayIndexOutOfBoundsException(index);
		byte dst[] = new byte[index];
		for (int i=0;i < dst.length; ++i)
			dst[i] = src[i];
		return dst;
	}

	public static final byte[] subArrayOfArrayFromIndexClippedToMultiple(byte src[], int index, int multiple)
	{
		int length = src.length - index;
		if (length < 0)
			throw new ArrayIndexOutOfBoundsException(index);
		length = (length / multiple) * multiple;
		byte dst[] = new byte[length];
		for (int i=0,j=index;i < dst.length; ++i,++j)
			dst[i] = src[j];
		return dst;
	}
	
	public static final byte[] extractBytesFromHexInString(String string)
	{
		byte fragment[] = new byte[string.length()/2 + 1];
		int hexIndex = 0;
		for (int index = 0, i = 0; index < string.length(); ++index) {
			char hexChar = string.charAt(index);
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
		return newFragment;
	}
}
