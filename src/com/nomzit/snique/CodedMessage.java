package com.nomzit.snique;

import java.util.Iterator;

import android.os.Bundle;

public class CodedMessage implements Iterable<byte[]>
{
	private static final String MESSAGE_INDEXES = "indexes";
	private static final String MESSAGE_BYTES = "bytes";

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
	
	public CodedMessage(Bundle bundle)
	{
		super();
		byte bytes[] = bundle.getByteArray(MESSAGE_BYTES);
		int indexes[] = bundle.getIntArray(MESSAGE_INDEXES);
		this.bytes = bytes;
		this.indexes = indexes;
	}
	
	public void addToBundle(Bundle bundle)
	{
		bundle.putByteArray(MESSAGE_BYTES, bytes);
		bundle.putIntArray(MESSAGE_INDEXES, indexes);
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
