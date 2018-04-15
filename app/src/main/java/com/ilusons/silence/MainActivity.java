package com.ilusons.silence;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ilusons.silence.data.DB;
import com.ilusons.silence.data.User;
import com.ilusons.silence.ref.AndroidEx;
import com.ilusons.silence.ref.IOEx;
import com.ilusons.silence.ref.JavaEx;
import com.ilusons.silence.views.ConversationsFragment;
import com.ilusons.silence.views.NearbyFragment;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

	private DrawerLayout drawer_layout;
	private NavigationView navigation_view;
	private ActionBarDrawerToggle actionBarDrawerToggle;

	private ViewPager view_pager;
	private StaticFragmentPagerAdapter view_pager_adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		// Setup toolbar
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setTitle(R.string.app_name);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// Setup drawer
		drawer_layout = findViewById(R.id.drawer_layout);
		drawer_layout.closeDrawer(GravityCompat.START);

		actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer_layout, R.string.open, R.string.close);
		drawer_layout.addDrawerListener(actionBarDrawerToggle);
		actionBarDrawerToggle.syncState();

		navigation_view = findViewById(R.id.navigation_view);
		navigation_view.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(@NonNull MenuItem item) {
				switch (item.getItemId()) {
					case R.id.menu_item_clear:
						resetData();
						return true;
					case R.id.menu_item_exit:
						finish();
						System.exit(0);
						return true;
				}
				return false;
			}
		});

		// Setup tabs
		view_pager = findViewById(R.id.view_pager);
		view_pager.setOffscreenPageLimit(3);

		view_pager_adapter = new StaticFragmentPagerAdapter(getSupportFragmentManager(), this);
		view_pager_adapter.add(new NearbyFragment(), "Nearby");
		view_pager_adapter.add(new ConversationsFragment(), "Conversations");

		view_pager.setAdapter(view_pager_adapter);

		TabLayout tab_layout = findViewById(R.id.tab_layout);
		tab_layout.setupWithViewPager(view_pager, true);

	}

	@Override
	protected void onStart() {
		super.onStart();

		// Load user info

		NavigationView navigationView = findViewById(R.id.navigation_view);
		View header = navigationView.getHeaderView(0);

		TextView user_name_drawer = header.findViewById(R.id.user_name_drawer);
		user_name_drawer.setText(DB.getCurrentUserId(this));

		final ImageView user_avatar_drawer = header.findViewById(R.id.user_avatar_drawer);

		DB.getUser(
				DB.getCurrentUserId(this),
				new JavaEx.ActionT<User>() {
					@Override
					public void execute(final User user) {
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Picasso.get().load(User.getAvatarUrl(user.Id)).into(user_avatar_drawer);
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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void resetData() {
		(new AlertDialog.Builder(this)
				.setTitle("Sure?")
				.setMessage("App will become like new, all your personalized content will be lost!")
				.setCancelable(true)
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						try {
							Toast.makeText(MainActivity.this, "Reset initiated! App will restart in a moment!", Toast.LENGTH_LONG).show();

							IOEx.deleteCache(getApplicationContext());

							((ActivityManager) getSystemService(Context.ACTIVITY_SERVICE)).clearApplicationUserData();
						} catch (Exception e) {
							e.printStackTrace();
						} finally {
							AndroidEx.restartApp(MainActivity.this);
						}
					}
				})
				.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						dialogInterface.dismiss();
					}
				}))
				.show();
	}

	//region Classes

	private static class StaticFragmentPagerAdapter extends FragmentPagerAdapter {
		private Context context;

		private List<Fragment> fragments;
		private List<String> pageTitles;

		public StaticFragmentPagerAdapter(FragmentManager fm, Context context) {
			super(fm);

			this.context = context;

			this.fragments = new Vector<>();
			this.pageTitles = new Vector<>();
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return pageTitles.get(position).toUpperCase();
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

		public void add(Fragment fragment, String title) {
			fragments.add(fragment);
			pageTitles.add(title);
			notifyDataSetChanged();
		}

	}

	//endregion

}
