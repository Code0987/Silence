package com.ilusons.silence.data;

public class Message {

	public String SenderId;
	public String ReceiverId;
	public String Content;
	public long Timestamp;

	public Message() {
		Timestamp = System.currentTimeMillis();
	}

}
