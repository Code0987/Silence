package com.ilusons.silence.data;

import com.firebase.geofire.GeoLocation;
import com.ilusons.silence.ref.RandomEx;

import java.util.Arrays;
import java.util.UUID;

/***
 * Java Object mapping of a user's data.
 */
public class User {

	public String Id;

	public long LastAccessed;

	public User() {
		Id = RandomEx.generateRandomString("_", Arrays.asList(RandomEx.COOL_WORDS, RandomEx.COLORS));
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

	public static String getAvatarUrl(String name) {
		return "https://api.adorable.io/avatars/128/" + name + ".png";
	}

}
