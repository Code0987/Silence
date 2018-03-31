package com.ilusons.silence.ref;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static android.content.Context.VIBRATOR_SERVICE;


public class AndroidEx {

	public static int dpToPx(int dp) {
		return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
	}

	public static int pxToDp(int px) {
		return (int) (px / Resources.getSystem().getDisplayMetrics().density);
	}

	public static void vibrate(final Context context, int duration) {
		try {
			if (Build.VERSION.SDK_INT >= 26) {
				((Vibrator) context.getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
			} else {
				((Vibrator) context.getSystemService(VIBRATOR_SERVICE)).vibrate(duration);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void vibrate(final Context context) {
		vibrate(context, 150);
	}

	public static boolean isNetworkAvailable(final Context context) {
		try {
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo activeNetworkInfo = null;
			if (connectivityManager != null) {
				activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
			}
			return activeNetworkInfo != null && activeNetworkInfo.isConnected();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

}
