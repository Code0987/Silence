package com.ilusons.silence;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.ilusons.silence.views.ConversationsFragment;
import com.ilusons.silence.views.NearbyFragment;

import java.util.List;
import java.util.Vector;

public class MainActivity extends AppCompatActivity {

	private DrawerLayout drawer_layout;
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

		getSupportActionBar().setTitle(null);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// Setup drawer
		drawer_layout = findViewById(R.id.drawer_layout);
		drawer_layout.closeDrawer(GravityCompat.START);

		actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawer_layout, R.string.open, R.string.close);
		drawer_layout.addDrawerListener(actionBarDrawerToggle);
		actionBarDrawerToggle.syncState();

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
	public boolean onOptionsItemSelected(MenuItem item) {
		if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	//Just for testing UI
	public void openChatActivity(View view) {
		Intent openChat = new Intent(this, ConversationActivity.class);

		startActivity(openChat);
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
