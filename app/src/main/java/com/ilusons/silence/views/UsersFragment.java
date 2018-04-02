package com.ilusons.silence.views;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ilusons.silence.R;
import com.ilusons.silence.data.DB;
import com.ilusons.silence.data.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class UsersFragment extends Fragment {

	private View view;

	private RecyclerView recycler_view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.users, container, false);

		recycler_view = view.findViewById(R.id.recycler_view);
		recycler_view.setHasFixedSize(true);
		recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));

		return view;
	}


	@Override
	public void onStart() {
		super.onStart();

		FirebaseRecyclerAdapter<User, UserViewHolder> friendsRecyclerViewAdapter = new FirebaseRecyclerAdapter<User, UserViewHolder>(
				new SnapshotParser<User>() {
					@Override
					public User parseSnapshot(DataSnapshot snapshot) {
						return null;
					}
				},
				R.layout.users_user,
				UserViewHolder.class,
				DB.getGeoFireDatabase().getDatabaseReference()) {

			/**
			 * Each time the data at the given Firebase location changes, this method will be called for
			 * each item that needs to be displayed. The first two arguments correspond to the mLayout and
			 * mModelClass given to the constructor of this class. The third argument is the item's position
			 * in the list.
			 * <p>
			 * Your implementation should populate the view using the data contained in the model.
			 *
			 * @param viewHolder The view to populate
			 * @param model      The object containing the data used to populate the view
			 * @param position   The position in the list of the view being populated
			 */
			@Override
			protected void populateViewHolder(UserViewHolder viewHolder, User model, int position) {

			}
		};

		recycler_view.setAdapter(friendsRecyclerViewAdapter);

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

}
