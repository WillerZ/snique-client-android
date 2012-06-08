package com.nomzit.snique;

import java.util.Iterator;

public class CodedMessage implements Iterable<byte[]>
{
	private int[] indexes;
	private byte bytes[];
	
	public CodedMessage(byte fragments[][])
	{
		super();
		indexes = new int[fragments.length];
		bytes = new byte[0];
		int length = 0;
		for (byte[] arr: fragments)
			length += arr.length;
		bytes = new byte[length];
		int indexesIndex = 0;
		int index = 0;
		for (byte[] arr: fragments)
		{
			indexes[indexesIndex++] = index;
			for (int i = 0; i < arr.length; ++i,++index)
				bytes[index] = arr[i]; 
		}
	}
	
	public Iterator<byte[]> iterator()
	{
		return new Iterator<byte[]>()
		{
			private int indexesIndex = 0;
			public boolean hasNext()
			{
				return indexesIndex < indexes.length;
			}
			public byte[] next()
			{
				byte rv[] = new byte[bytes.length - indexes[indexesIndex]];
				for (int i = 0, j = indexes[indexesIndex]; i < rv.length; ++i, ++j)
					rv[i] = bytes[j];
				++indexesIndex;
				return rv;
			}
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}
}
