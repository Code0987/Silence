package com.ilusons.silence.core;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.ilusons.silence.data.DB;

public class LocationBroadcastService extends Service implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		LocationListener {

	private final String TAG = LocationBroadcastService.class.getName();

	private boolean currentlyProcessingLocation = false;
	private LocationRequest locationRequest;
	private GoogleApiClient googleApiClient;

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// if we are currently trying to get a location and the alarm manager has called this again,
		// no need to start processing a new location.
		if (!currentlyProcessingLocation) {
			currentlyProcessingLocation = true;
			startTracking();
		}

		return START_STICKY;
	}

	private void startTracking() {
		Log.d(TAG, "startTracking");

		if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

			googleApiClient = new GoogleApiClient.Builder(this)
					.addApi(LocationServices.API)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.build();

			if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
				googleApiClient.connect();
			}

		} else {
			Log.e(TAG, "unable to connect to google play services.");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onLocationChanged(Location location) {
		if (location != null) {
			Log.e(TAG, "position: " + location.getLatitude() + ", " + location.getLongitude() + " accuracy: " + location.getAccuracy());

			// we have our desired accuracy of 500 meters so lets quit this service,
			// onDestroy will be called and stop our location uodates
			//if (location.getAccuracy() < 500.0f) {
			//	stopLocationUpdates();
			DB.setUserLocation(this, DB.getCurrentUserId(this), location);

			DB.setCurrentUserLocation(this, location);
			//}
		}
	}

	private void stopLocationUpdates() {
		if (googleApiClient != null && googleApiClient.isConnected()) {
			googleApiClient.disconnect();
		}
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.d(TAG, "onConnected");

		locationRequest = LocationRequest.create();
		locationRequest.setInterval(1000); // milliseconds
		locationRequest.setFastestInterval(500); // the fastest rate in milliseconds at which your app can handle location updates
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			// TODO: Consider calling
			//    ActivityCompat#requestPermissions
			// here to request the missing permissions, and then overriding
			//   public void onRequestPermissionsResult(int requestCode, String[] permissions,
			//                                          int[] grantResults)
			// to handle the case where the user grants the permission. See the documentation
			// for ActivityCompat#requestPermissions for more details.
			return;
		}

		LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.e(TAG, "onConnectionFailed");

		stopLocationUpdates();
		stopSelf();
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.e(TAG, "GoogleApiClient connection has been suspend");
	}
}
