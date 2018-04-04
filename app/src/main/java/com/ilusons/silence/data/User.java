package com.ilusons.silence.data;

import com.ilusons.silence.ref.RandomEx;

import java.util.Arrays;
import java.util.UUID;

public class User {

	public String Id;
	public String Name;

	public User() {
		Id = (UUID.randomUUID()).toString();
		Name = RandomEx.generateRandomString("_", Arrays.asList(RandomEx.COLORS, RandomEx.COOL_WORDS, RandomEx.ADJECTIVES));
	}

	@Override
	public boolean equals(Object obj) {
		User other = (User) obj;

		if (other == null)
			return false;

		if (Id.equals(other.Id))
			return true;

		return false;
	}

}
