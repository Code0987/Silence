package com.ilusons.silence.views;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.database.DatabaseError;
import com.ilusons.silence.R;
import com.ilusons.silence.data.DB;
import com.ilusons.silence.data.User;
import com.ilusons.silence.ref.JavaEx;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import fr.tkeunebr.gravatar.Gravatar;

public class ConversationsFragment extends Fragment {

	private View view;

	private RecyclerView recycler_view;
	private RecyclerViewAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.conversations, container, false);

		recycler_view = view.findViewById(R.id.recycler_view);
		recycler_view.setHasFixedSize(true);
		recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));

		return view;
	}

	@Override
	public void onStart() {
		super.onStart();

		adapter = new RecyclerViewAdapter(getContext());

		recycler_view.setAdapter(adapter);

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

	@Override
	public void onStop() {
		super.onStop();

		try {
			DB.getGeoQueryForAllUsers().removeGeoQueryEventListener(geoQueryEventListener);
		} catch (Exception e) {
			// Eat?
		}
	}

	private GeoQueryEventListener geoQueryEventListener = new GeoQueryEventListener() {
		@Override
		public void onKeyEntered(String key, final GeoLocation location) {

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
	};

	public static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.UserViewHolder> {

		private final Context context;

		private final ArrayList<User> users;

		public RecyclerViewAdapter(Context context) {
			this.context = context;

			users = new ArrayList<>();
		}

		@Override
		public int getItemCount() {
			return users.size();
		}

		@Override
		public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.conversations_conversation, parent, false);

			UserViewHolder vh = new UserViewHolder(v);

			return vh;
		}

		@Override
		public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
			final User user = users.get(position);

			holder.name.setText(user.Id);

			String gravatarUrl = Gravatar.init().with(user.Id).force404().size(Gravatar.MIN_IMAGE_SIZE_PIXEL).build();

			Picasso.get().load(gravatarUrl).into(holder.image);

		}

		public static class UserViewHolder extends RecyclerView.ViewHolder {

			public View view;

			public ImageView image;
			public TextView name;

			public UserViewHolder(View itemView) {
				super(itemView);

				view = itemView;

				image = view.findViewById(R.id.image);
				name = view.findViewById(R.id.name);

			}

		}

		public void add(User user) {
			if (!users.contains(user))
				users.add(user);

			notifyDataSetChanged();
		}

		public void remove(User user) {
			if (users.contains(user))
				users.remove(user);

			notifyDataSetChanged();
		}

	}

}
