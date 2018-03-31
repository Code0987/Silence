package com.ilusons.silence.data;

import java.util.ArrayList;

public class Conversation {
	private ArrayList<Message> Messages;

	public ArrayList<Message> getMessages() {
		return Messages;
	}

	public Conversation() {
		Messages = new ArrayList<>();
	}
}
