package com.ilusons.silence.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ilusons.silence.BuildConfig;
import com.ilusons.silence.ref.JavaEx;

import static android.content.Context.MODE_PRIVATE;

public final class DB {

	//region Firebase db

	public static FirebaseDatabase getFirebaseDatabase() {
		return FirebaseDatabase.getInstance(BuildConfig.DEBUG ? "testing" : "production");
	}

	//endregion

	//region Shared prefs

	public static String TAG_SPREF = "spref";

	public static SharedPreferences getSharedPreferences(final Context context) {
		SharedPreferences spref = context.getSharedPreferences(TAG_SPREF, MODE_PRIVATE);
		return spref;
	}

	public static String TAG_SPREF_FIRSTRUN = "first_run";

	public static boolean getFirstRun(final Context context) {
		return getSharedPreferences(context).getBoolean(TAG_SPREF_FIRSTRUN, false);
	}

	public static void setFirstRun(final Context context, boolean value) {
		SharedPreferences.Editor editor = getSharedPreferences(context).edit();
		editor.putBoolean(TAG_SPREF_FIRSTRUN, value);
		editor.apply();
	}

	//endregion

	//region Logic

	public static String TAG_SPREF_CURRENT_USER_ID = "current_user_id";

	public static String getCurrentUserId(final Context context) {
		return getSharedPreferences(context).getString(TAG_SPREF_CURRENT_USER_ID, null);
	}

	public static void setCurrentUserId(final Context context, String value) {
		SharedPreferences.Editor editor = getSharedPreferences(context).edit();
		editor.putString(TAG_SPREF_CURRENT_USER_ID, value);
		editor.apply();
	}

	public final static String KEY_USERS = "users";
	public final static String KEY_USERS_ID = "id";
	public final static String KEY_USERS_NAME = "name";

	public static void getUser(final String id, final JavaEx.ActionT<User> onUser, final JavaEx.ActionT<Throwable> onError) {
		getFirebaseDatabase().getReference()
				.child(KEY_USERS)
				.child(id)
				.addValueEventListener(new ValueEventListener() {
					@Override
					public void onDataChange(DataSnapshot dataSnapshot) {
						if (dataSnapshot != null) {
							try {
								User user = new User();
								user.Id = id;
								user.Name = dataSnapshot.child(KEY_USERS_NAME).getValue().toString();

								if (onUser != null)
									onUser.execute(user);
							} catch (Exception e) {
								e.printStackTrace();

								if (onError != null)
									onError.execute(e);
							}
						} else {
							if (onError != null)
								onError.execute(new Exception());
						}
					}

					@Override
					public void onCancelled(DatabaseError databaseError) {
						if (onError != null)
							onError.execute(new Exception(databaseError.getMessage()));
					}
				});
	}

	//endregion

}
