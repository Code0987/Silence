package com.ilusons.silence.views;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ilusons.silence.R;
import com.ilusons.silence.data.DB;
import com.ilusons.silence.data.Message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/***
 * Lists all the conversations related to current user.
 */
public class ConversationsFragment extends Fragment {

	private View view;

	private RecyclerView recycler_view;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		view = inflater.inflate(R.layout.conversations, container, false);

		recycler_view = view.findViewById(R.id.recycler_view);
		recycler_view.setHasFixedSize(true);
		recycler_view.setLayoutManager(new LinearLayoutManager(getContext()));

		LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
		layoutManager.setReverseLayout(true);
		layoutManager.setStackFromEnd(true);

		recycler_view.setLayoutManager(layoutManager);

		return view;
	}

	private ItemsAdapter adapter;

	@Override
	public void onStart() {
		super.onStart();

		adapter = new ItemsAdapter(getContext());

		ValueEventListener valueEventListener = new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				adapter.addData(DB.getCurrentUserId(getContext()), Message.createFromData(dataSnapshot));
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		};

		ChildEventListener childEventListener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot dataSnapshot, String s) {
				adapter.addData(DB.getCurrentUserId(getContext()), Message.createFromData(dataSnapshot));
			}

			@Override
			public void onChildChanged(DataSnapshot dataSnapshot, String s) {

			}

			@Override
			public void onChildRemoved(DataSnapshot dataSnapshot) {
				adapter.removeData(Message.createFromData(dataSnapshot));
			}

			@Override
			public void onChildMoved(DataSnapshot dataSnapshot, String s) {

			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		};

		Query query1 = DB.getFirebaseDatabase().getReference()
				.child(DB.KEY_MESSAGES)
				.orderByChild(DB.KEY_MESSAGES_RECEIVER_ID)
				.equalTo(DB.KEY_MESSAGES_RECEIVER_ID, DB.getCurrentUserId(getContext()));
		query1.addListenerForSingleValueEvent(valueEventListener);
		query1.addChildEventListener(childEventListener);

		Query query2 = DB.getFirebaseDatabase().getReference()
				.child(DB.KEY_MESSAGES)
				.orderByChild(DB.KEY_MESSAGES_SENDER_ID)
				.equalTo(DB.KEY_MESSAGES_SENDER_ID, DB.getCurrentUserId(getContext()));
		query2.addListenerForSingleValueEvent(valueEventListener);
		query2.addChildEventListener(childEventListener);

		recycler_view.setAdapter(adapter);

	}

	public static class ItemsAdapter extends RecyclerView.Adapter<ItemsAdapter.ItemViewHolder> {

		private final Context context;

		private final ArrayList<Pair<String, ArrayList<Message>>> items;

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

			View view = LayoutInflater.from(context).inflate(R.layout.conversations_item, parent, false);

			ItemViewHolder vh = new ItemViewHolder(view);

			return vh;
		}

		@Override
		public void onBindViewHolder(@NonNull ItemViewHolder vh, int position) {
			final Pair<String, ArrayList<Message>> item = items.get(position);

			vh.id.setText(item.first);

			vh.info.setText(item.second.size());

		}

		public static class ItemViewHolder extends RecyclerView.ViewHolder {

			public View view;

			public ImageView image;
			public TextView id;
			public TextView info;

			public ItemViewHolder(View itemView) {
				super(itemView);

				view = itemView;

				image = view.findViewById(R.id.image);
				id = view.findViewById(R.id.id);
				info = view.findViewById(R.id.info);

			}

		}

		public void addData(String myId, Message data) {
			Pair<String, ArrayList<Message>> itemMatched = null;

			for (Pair<String, ArrayList<Message>> item : items)
				if (item.first.equals(data.SenderId) || item.first.equals(data.ReceiverId)) {
					itemMatched = item;
					break;
				}

			if (itemMatched == null) {
				itemMatched = Pair.create(
						data.SenderId,
						new ArrayList<Message>());

				items.add(itemMatched);
			}

			if (myId.equals(data.SenderId) || myId.equals(data.ReceiverId))
				itemMatched.second.add(data);

			notifyDataSetChanged();
		}

		public void removeData(Message data) {
			Pair<String, ArrayList<Message>> itemMatched = null;

			for (Pair<String, ArrayList<Message>> item : items)
				if (item.first.equals(data.SenderId) || item.first.equals(data.ReceiverId)) {
					itemMatched = item;
					break;
				}

			if (itemMatched != null) {
				items.remove(itemMatched);
			}

			notifyDataSetChanged();
		}

	}

}
