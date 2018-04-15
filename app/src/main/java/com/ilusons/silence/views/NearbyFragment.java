package com.ilusons.silence.views;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.ilusons.silence.ConversationActivity;
import com.ilusons.silence.R;
import com.ilusons.silence.data.DB;
import com.ilusons.silence.data.User;
import com.ilusons.silence.ref.JavaEx;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/***
 * Handles location related work.
 * Shows map of nearby users.
 * Sends current location updates of current user to cloud.
 */
public class NearbyFragment extends Fragment implements OnMapReadyCallback {

	private final String TAG = NearbyFragment.class.getName();

	public static final int REQUEST_LOCATION = 32;

	public static final int REQUEST_GOOGLE_PLAY_SERVICES = 42;

	private View view;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			checkRequirements();
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.nearby, container, false);

		return view;
	}

	@Override
	public void onDestroyView() {
		// Remove map
		try {
			SupportMapFragment fragment = ((SupportMapFragment) getChildFragmentManager().findFragmentById(
					R.id.map));
			FragmentTransaction ft = getChildFragmentManager().beginTransaction();
			ft.remove(fragment);
			ft.commit();
		} catch (Exception e) {
			Log.w(TAG, e);
		}

		removeLocationUpdates();

		super.onDestroyView();
	}

	@Override
	public void onStart() {
		super.onStart();

		checkRequirements();
	}

	@Override
	public void onResume() {
		super.onResume();

		checkRequirements();
	}

	@Override
	public void onPause() {
		super.onPause();

		removeLocationUpdates();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_GOOGLE_PLAY_SERVICES:
				if (resultCode == Activity.RESULT_OK) {
					Toast.makeText(getContext(), "Connected to Google Play services :)", Toast.LENGTH_LONG).show();
				}
				break;

			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
	}

	//region Initial

	private void checkPermissions() {
		final Context context = getContext();
		if (context == null)
			return;

		final FragmentActivity activity = getActivity();
		if (activity == null)
			return;

		if (ActivityCompat.checkSelfPermission(context,
				android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
				ActivityCompat.checkSelfPermission(context,
						android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(activity,
					new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,
							android.Manifest.permission.ACCESS_FINE_LOCATION},
					REQUEST_LOCATION);
		} else {
			SupportMapFragment mapFragment = SupportMapFragment.newInstance();

			mapFragment.getMapAsync(this);

			getChildFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();

			createLocationUpdates();
		}
	}

	private void checkGoogleApi() {
		final FragmentActivity activity = getActivity();
		if (activity == null)
			return;

		GoogleApiAvailability api = GoogleApiAvailability.getInstance();
		int code = api.isGooglePlayServicesAvailable(activity);
		if (code == ConnectionResult.SUCCESS) {
			onActivityResult(REQUEST_GOOGLE_PLAY_SERVICES, Activity.RESULT_OK, null);
		} else if (
				api.isUserResolvableError(code)
						&&
						api.showErrorDialogFragment(activity, code, REQUEST_GOOGLE_PLAY_SERVICES)) {
			// wait for onActivityResult call (see below)
		} else {
			Toast.makeText(getContext(), api.getErrorString(code), Toast.LENGTH_LONG).show();
		}
	}

	private void checkRequirements() {
		checkPermissions();
		checkGoogleApi();
	}

	//endregion

	//region Location updates

	private FusedLocationProviderClient fusedLocationProviderClient;
	private LocationCallback locationCallback;
	private LatLng lastLocation;

	private GoogleMap googleMap;

	//private Marker googleMapMarkerForMe;
	private HashMap<String, Marker> googleMapMarkers = new HashMap<>();

	@SuppressLint("MissingPermission")
	@Override
	public void onMapReady(final GoogleMap googleMap) {
		this.googleMap = googleMap;

		final Context context = getContext();
		if (context == null)
			return;

		try {
			googleMap.setMyLocationEnabled(true);
		} catch (SecurityException e) {
			Log.e(TAG, "setMyLocationEnabled", e);
		}
		googleMap.setBuildingsEnabled(true);
		googleMap.setIndoorEnabled(true);
		googleMap.setTrafficEnabled(true);
		googleMap.getUiSettings().setAllGesturesEnabled(true);
		googleMap.getUiSettings().setCompassEnabled(true);
		googleMap.getUiSettings().setIndoorLevelPickerEnabled(true);
		googleMap.getUiSettings().setMapToolbarEnabled(true);
		googleMap.getUiSettings().setMyLocationButtonEnabled(true);
		googleMap.getUiSettings().setRotateGesturesEnabled(true);
		googleMap.getUiSettings().setScrollGesturesEnabled(true);
		googleMap.getUiSettings().setTiltGesturesEnabled(true);
		googleMap.getUiSettings().setZoomControlsEnabled(true);
		googleMap.getUiSettings().setZoomGesturesEnabled(true);
		googleMap.setMaxZoomPreference(7);
		googleMap.setMinZoomPreference(7);

		final Location location = DB.getCurrentUserLocation(context);
		LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
		lastLocation = loc;
		//googleMapMarkerForMe = googleMap.addMarker(new MarkerOptions()
		//		.position(loc)
		//		.title(DB.getCurrentUserId(context))
		//		.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
		//googleMapMarkerForMe.showInfoWindow();

		googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
			@Override
			public boolean onMarkerClick(Marker marker) {
				if (marker.getTag() != null)
					try {
						User user = (User) marker.getTag();

						Intent intent = new Intent(getContext(), ConversationActivity.class);
						intent.putExtra(ConversationActivity.KEY_PEER_USER_ID, user.Id);
						getContext().startActivity(intent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				return false;
			}
		});

		resetMap();
	}

	private GeoQueryEventListener geoQueryEventListener = new GeoQueryEventListener() {
		@Override
		public void onKeyEntered(String key, final GeoLocation location) {
			if (key.equals(DB.getCurrentUserId(getContext())))
				return;

			DB.getUser(
					key,
					new JavaEx.ActionT<User>() {
						@Override
						public void execute(final User user) {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									addUser(user, location);

									Toast.makeText(getContext(), user.Id + " is near you!", Toast.LENGTH_LONG).show();
								}
							});
						}
					},
					new JavaEx.ActionT<Throwable>() {
						@Override
						public void execute(Throwable throwable) {

						}
					});
		}

		@Override
		public void onKeyExited(String key) {
			if (key.equals(DB.getCurrentUserId(getContext())))
				return;

			DB.getUser(
					key,
					new JavaEx.ActionT<User>() {
						@Override
						public void execute(final User user) {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									removeUser(user);

									Toast.makeText(getContext(), user.Id + " has gone away!", Toast.LENGTH_LONG).show();
								}
							});
						}
					},
					new JavaEx.ActionT<Throwable>() {
						@Override
						public void execute(Throwable throwable) {

						}
					});
		}

		@Override
		public void onKeyMoved(String key, final GeoLocation location) {
			if (key.equals(DB.getCurrentUserId(getContext())))
				return;

			DB.getUser(
					key,
					new JavaEx.ActionT<User>() {
						@Override
						public void execute(final User user) {
							getActivity().runOnUiThread(new Runnable() {
								@Override
								public void run() {
									removeUser(user);
									addUser(user, location);

									Toast.makeText(getContext(), user.Id + " moved!", Toast.LENGTH_LONG).show();
								}
							});
						}
					},
					new JavaEx.ActionT<Throwable>() {
						@Override
						public void execute(Throwable throwable) {

						}
					});
		}

		@Override
		public void onGeoQueryReady() {

		}

		@Override
		public void onGeoQueryError(DatabaseError error) {

		}
	};

	/***
	 * Create location api client.
	 */
	@SuppressLint("MissingPermission")
	private void createLocationUpdates() {
		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(getContext());

		locationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				super.onLocationResult(locationResult);

				Location location = locationResult.getLastLocation();

				onNewLocation(location);
			}
		};

		fusedLocationProviderClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
			@Override
			public void onComplete(@NonNull Task<Location> task) {
				Location location = task.getResult();

				onNewLocation(location);
			}
		});

		requestLocationUpdates();

	}

	/***
	 * Call location api to start publishing updated of current location changes.
	 */
	public void requestLocationUpdates() {
		try {
			LocationRequest locationRequest = LocationRequest.create();
			locationRequest.setInterval((long) 3000);
			locationRequest.setFastestInterval((long) 3000 / 2);
			locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

			fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
		} catch (SecurityException unlikely) {
			Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
		}

		// Start listening changes in cloud database.
		try {
			DB.getGeoQueryForAllUsers().removeGeoQueryEventListener(geoQueryEventListener);
		} catch (Exception e) {
			// Eat?
		}
		try {
			DB.getGeoQueryForAllUsers().addGeoQueryEventListener(geoQueryEventListener);
		} catch (Exception e) {
			// Eat?
		}
	}

	/***
	 * Stop location updates.
	 */
	public void removeLocationUpdates() {
		Log.i(TAG, "Removing location updates");
		try {
			fusedLocationProviderClient.removeLocationUpdates(locationCallback);
		} catch (SecurityException unlikely) {
			Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
		}

		try {
			DB.getGeoQueryForAllUsers().removeGeoQueryEventListener(geoQueryEventListener);
		} catch (Exception e) {
			// Eat?
		}
	}

	/***
	 * Current location of device is updated.
	 * Post it to cloud database, also save it locally.
	 * @param location
	 */
	private void onNewLocation(Location location) {
		if (location != null) {
			Log.i(TAG, "position: " + location.getLatitude() + ", " + location.getLongitude() + " accuracy: " + location.getAccuracy());

			// we have our desired accuracy of 500 meters so lets quit this service,
			// onDestroy will be called and stop our location updates
			//if (location.getAccuracy() < 500.0f) {
			//	stopLocationUpdates();

			Context context = getContext().getApplicationContext();

			lastLocation = new LatLng(location.getLatitude(), location.getLongitude());

			DB.getGeoQueryForAllUsers().setCenter(new GeoLocation(location.getLatitude(), location.getLongitude()));

			DB.setUserLocation(context, DB.getCurrentUserId(context), location);

			DB.setCurrentUserLocation(context, location);

			//if (googleMapMarkerForMe != null) {
			//	googleMapMarkerForMe.setPosition(lastLocation);
			//}

			resetMap();

			//}
		}
	}

	/***
	 * Add new user in current nearby address to map.
	 * @param user
	 * @param geoLocation
	 */
	public void addUser(User user, GeoLocation geoLocation) {
		try {
			LatLng latLng = new LatLng(geoLocation.latitude, geoLocation.longitude);

			Marker marker = googleMap.addMarker(new MarkerOptions()
					.position(latLng)
					.title(user.Id)
					.snippet(user.Id)
					.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
			marker.setTag(user);
			marker.showInfoWindow();

			if (googleMapMarkers.containsKey(user.Id))
				removeUser(user);
			googleMapMarkers.put(user.Id, marker);

			resetMap();
		} catch (Exception e) {
			Log.e(TAG, "Parse location error", e);
		}
	}

	/***
	 * Remove user from nearby map.
	 * @param user
	 */
	public void removeUser(User user) {
		if (googleMap == null)
			return;

		if (googleMapMarkers.containsKey(user.Id)) {
			Marker marker = googleMapMarkers.get(user.Id);

			googleMapMarkers.remove(user.Id);

			marker.remove();
		}

		resetMap();
	}

	/***
	 * Updated changes to map.
	 */
	public void resetMap() {
		if (googleMap == null)
			return;

		Integer count = 0;
		LatLngBounds.Builder builder = new LatLngBounds.Builder();
		for (Map.Entry<String, Marker> entry : googleMapMarkers.entrySet()) {
			String k = entry.getKey();
			Marker v = entry.getValue();

			builder.include(v.getPosition());
			count++;
		}

		if (count >= 1) {
			googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
			googleMap.animateCamera(CameraUpdateFactory.zoomTo(7));
		} else {
			LatLngBounds.Builder b = new LatLngBounds.Builder().include(lastLocation);
			googleMap.setLatLngBoundsForCameraTarget(b.build());
			googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 100));
			googleMap.animateCamera(CameraUpdateFactory.zoomTo(7));
		}
	}

	//endregion

}
