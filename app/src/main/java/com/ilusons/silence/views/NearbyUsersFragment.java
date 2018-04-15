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
import com.firebase.geofire.util.GeoUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseError;
import com.ilusons.silence.ConversationActivity;
import com.ilusons.silence.R;
import com.ilusons.silence.data.DB;
import com.ilusons.silence.data.User;
import com.ilusons.silence.ref.JavaEx;
import com.ilusons.silence.ref.TimeEx;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/***
 * Handles location related work.
 * Shows list of nearby users.
 * Sends current location updates of current user to cloud.
 */
public class NearbyUsersFragment extends Fragment {

	private final String TAG = NearbyUsersFragment.class.getName();

	public static final int REQUEST_LOCATION = 32;

	public static final int REQUEST_GOOGLE_PLAY_SERVICES = 42;

	private View view;

	private RecyclerView recycler_view;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState == null) {
			checkRequirements();
		}
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.nearby_users, container, false);

		recycler_view = view.findViewById(R.id.recycler_view);
		recycler_view.setHasFixedSize(true);
		recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));

		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
		layoutManager.setReverseLayout(true);
		layoutManager.setStackFromEnd(true);

		recycler_view.setLayoutManager(layoutManager);

		return view;
	}

	@Override
	public void onDestroyView() {
		removeLocationUpdates();

		super.onDestroyView();
	}

	private ItemsAdapter adapter;

	@Override
	public void onStart() {
		super.onStart();

		adapter = new ItemsAdapter(getContext());

		recycler_view.setAdapter(adapter);

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
				Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
				ActivityCompat.checkSelfPermission(context,
						Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(activity,
					new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
							Manifest.permission.ACCESS_FINE_LOCATION},
					REQUEST_LOCATION);
		} else {
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
									adapter.addItem(user);
								}
							});
						}
					},
					new JavaEx.ActionT<Throwable>() {
						@Override
						public void execute(Throwable throwable) {

						}
					},
					false);
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
									adapter.removeItem(user);
								}
							});
						}
					},
					new JavaEx.ActionT<Throwable>() {
						@Override
						public void execute(Throwable throwable) {

						}
					},
					false);
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
									if (adapter != null) {
										adapter.removeItem(user);
										adapter.addItem(user);
									}
								}
							});
						}
					},
					new JavaEx.ActionT<Throwable>() {
						@Override
						public void execute(Throwable throwable) {

						}
					},
					false);
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

			if (fusedLocationProviderClient != null)
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
			if (fusedLocationProviderClient != null)
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

			Context context = getContext().getApplicationContext();

			lastLocation = new LatLng(location.getLatitude(), location.getLongitude());

			DB.getGeoQueryForAllUsers().setCenter(new GeoLocation(location.getLatitude(), location.getLongitude()));

			DB.setUserLocation(context, DB.getCurrentUserId(context), location);

			DB.setCurrentUserLocation(context, location);
		}
	}

	//endregion

	//region Classes

	public static class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ItemViewHolder> {

		private final Context context;

		private final ArrayList<User> items;

		public ItemsAdapter(Context context) {
			this.context = context;

			items = new ArrayList<>();
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		@NonNull
		@Override
		public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context context = parent.getContext();

			View view = LayoutInflater.from(context).inflate(R.layout.nearby_users_item, parent, false);

			ItemViewHolder vh = new ItemViewHolder(view);

			return vh;
		}

		@Override
		public void onBindViewHolder(@NonNull ItemViewHolder vh, int position) {
			final User item = items.get(position);

			vh.id.setText(item.Id);

			vh.info.setText(TimeEx.getTimeAgo(item.LastAccessed));

			Picasso.get().load(User.getAvatarUrl(item.Id)).into(vh.image);

			vh.view.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					try {
						Intent intent = new Intent(view.getContext(), ConversationActivity.class);
						intent.putExtra(ConversationActivity.KEY_PEER_USER_ID, item.Id);
						view.getContext().startActivity(intent);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

		}

		public static class ItemViewHolder extends RecyclerView.ViewHolder {

			public View view;

			public ImageView image;
			public TextView id;
			public TextView info;

			public ItemViewHolder(View view) {
				super(view);

				this.view = view;

				image = view.findViewById(R.id.image);
				id = view.findViewById(R.id.id);
				info = view.findViewById(R.id.info);

			}

		}

		public void addItem(User item) {
			if (items.contains(item))
				items.remove(item);

			items.add(item);

			notifyDataSetChanged();
		}

		public void removeItem(User item) {
			if (items.contains(item))
				items.remove(item);

			notifyDataSetChanged();
		}

	}

	//endregion

}
