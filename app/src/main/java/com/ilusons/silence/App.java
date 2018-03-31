package com.ilusons.silence;

import android.app.Application;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

public class App extends Application {

	private static final String TAG = App.class.getSimpleName();

	// Called when the application is starting, before any other application objects have been created.
	// Overriding this method is totally optional!
	@Override
	public void onCreate() {
		super.onCreate();

		// WTFs
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable e) {
				if (getMainLooper() != null)
					Toast.makeText(App.this, "Aw, snap!", Toast.LENGTH_LONG).show();

				Log.wtf(TAG, e);
			}
		});

		// DB
		try {
			FirebaseDatabase.getInstance().setPersistenceEnabled(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Images
		try {
			Picasso.Builder builder = new Picasso.Builder(this);
			Picasso picasso = builder.build();
			picasso.setIndicatorsEnabled(true);
			picasso.setLoggingEnabled(true);
			Picasso.setSingletonInstance(picasso);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Called by the system when the device configuration changes while your component is running.
	// Overriding this method is totally optional!
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	// This is called when the overall system is running low on memory,
	// and would like actively running processes to tighten their belts.
	// Overriding this method is totally optional!
	@Override
	public void onLowMemory() {
		super.onLowMemory();
	}
}
