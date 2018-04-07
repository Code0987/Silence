package com.ilusons.silence.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.geofire.util.GeoUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
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

	public static GeoFire getGeoFireDatabase() {
		DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("geo");
		GeoFire geoFire = new GeoFire(databaseReference);
		return geoFire;
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
		String userId = getSharedPreferences(context).getString(TAG_SPREF_CURRENT_USER_ID, null);
		if (TextUtils.isEmpty(userId)) {
			// Assign a random user Id, app will handle it automatically, no server side update needed
			userId = (new User()).Id;

			setCurrentUserId(context, userId);
		}
		return userId;
	}

	public static void setCurrentUserId(final Context context, String value) {
		SharedPreferences.Editor editor = getSharedPreferences(context).edit();
		editor.putString(TAG_SPREF_CURRENT_USER_ID, value);
		editor.apply();
	}

	public static String TAG_SPREF_CURRENT_USER_LOCATION = "current_user_location";

	public static Location getCurrentUserLocation(final Context context) {
		float lat = getSharedPreferences(context).getFloat(TAG_SPREF_CURRENT_USER_LOCATION + "_lat", 0);
		float lng = getSharedPreferences(context).getFloat(TAG_SPREF_CURRENT_USER_LOCATION + "_lng", 0);

		Location location = new Location("");

		location.setLatitude(lat);
		location.setLongitude(lng);

		return location;
	}

	public static void setCurrentUserLocation(final Context context, Location location) {
		SharedPreferences.Editor editor = getSharedPreferences(context).edit();
		editor.putFloat(TAG_SPREF_CURRENT_USER_LOCATION + "_lat", (float) location.getLatitude());
		editor.putFloat(TAG_SPREF_CURRENT_USER_LOCATION + "_lng", (float) location.getLongitude());
		editor.apply();
	}

	public final static String KEY_USERS = "users";
	public final static String KEY_USERS_ID = "id";

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

	public static void setUserLocation(Context context, String userId, Location location) {
		GeoLocation geoLocation = new GeoLocation(location.getLatitude(), location.getLongitude());

		GeoFire geoFire = getGeoFireDatabase();

		geoFire.setLocation(userId, geoLocation, new GeoFire.CompletionListener() {
			@Override
			public void onComplete(String key, DatabaseError error) {
				Log.d("geoFire/setLocation", key + "\n" + error);
			}
		});
	}

	private static GeoQuery geoQueryForAllUsers;

	public static GeoQuery getGeoQueryForAllUsers() {
		if (geoQueryForAllUsers == null) {
			GeoFire geoFire = getGeoFireDatabase();

			geoQueryForAllUsers = geoFire.queryAtLocation(new GeoLocation(0, 0), 8587);

			geoQueryForAllUsers.addGeoQueryEventListener(new GeoQueryEventListener() {
				@Override
				public void onKeyEntered(String key, GeoLocation location) {
					Log.d("geoFire", String.format("Key %s entered the search area at [%f,%f]", key, location.latitude, location.longitude));


				}

				@Override
				public void onKeyExited(String key) {

				}

				@Override
				public void onKeyMoved(String key, GeoLocation location) {

				}

				@Override
				public void onGeoQueryReady() {

				}

				@Override
				public void onGeoQueryError(DatabaseError error) {

				}
			});
		}

		return geoQueryForAllUsers;
	}

	//endregion

}
