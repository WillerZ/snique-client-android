package com.nomzit.snique;

public class SniqueMessage
{
	int id;
	String message; 
	public SniqueMessage(int id, String message)
	{
		super();
		this.id = id;
		this.message = message;
	}
	
	public int getId()
	{
		return id;
	}
	
	public String getMessage()
	{
		return message;
	}
}
