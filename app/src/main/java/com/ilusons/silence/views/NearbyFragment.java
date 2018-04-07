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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.ilusons.silence.R;
import com.ilusons.silence.data.DB;
import com.ilusons.silence.data.User;
import com.ilusons.silence.ref.JavaEx;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import fr.tkeunebr.gravatar.Gravatar;

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

	private GoogleMap googleMap;

	private Marker googleMapMarkerForMe;
	private List<Marker> googleMapMarkers = new Vector<>();

	@SuppressLint("MissingPermission")
	@Override
	public void onMapReady(final GoogleMap googleMap) {
		this.googleMap = googleMap;

		final Context context = getContext();
		if (context == null)
			return;

		googleMap.setMyLocationEnabled(true);
		googleMap.setBuildingsEnabled(true);
		googleMap.setIndoorEnabled(true);
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

		final Location location = DB.getCurrentUserLocation(context);
		LatLng loc = new LatLng(location.getLatitude(), location.getLongitude());
		googleMapMarkerForMe = googleMap.addMarker(new MarkerOptions()
				.position(loc)
				.title(DB.getCurrentUserId(context))
				.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

		CameraPosition googlePlex = CameraPosition.builder()
				.target(loc)
				.zoom(21)
				.bearing(0)
				.tilt(45)
				.build();
		googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(googlePlex));
	}

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

	//region Location updates

	private FusedLocationProviderClient fusedLocationProviderClient;
	private LocationCallback locationCallback;

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
	}

	public void removeLocationUpdates() {
		Log.i(TAG, "Removing location updates");
		try {
			fusedLocationProviderClient.removeLocationUpdates(locationCallback);
		} catch (SecurityException unlikely) {
			Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
		}
	}

	private void onNewLocation(Location location) {
		if (location != null) {
			Log.i(TAG, "position: " + location.getLatitude() + ", " + location.getLongitude() + " accuracy: " + location.getAccuracy());

			Context context = getContext().getApplicationContext();

			// we have our desired accuracy of 500 meters so lets quit this service,
			// onDestroy will be called and stop our location updates
			//if (location.getAccuracy() < 500.0f) {
			//	stopLocationUpdates();
			DB.setUserLocation(context, DB.getCurrentUserId(context), location);

			DB.setCurrentUserLocation(context, location);
			//}
		}
	}

	//endregion

}
