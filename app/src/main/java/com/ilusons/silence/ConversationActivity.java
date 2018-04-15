package com.ilusons.silence;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ilusons.silence.data.DB;
import com.ilusons.silence.data.Message;
import com.ilusons.silence.data.User;
import com.ilusons.silence.ref.TimeEx;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/***
 * Activity for single peer-to-peer conversation i.e. chatting.
 * Shows all the previous messages and allows to send new message.
 */
public class ConversationActivity extends AppCompatActivity {

	public final static String KEY_PEER_USER_ID = "peer_user_id";

	private String peerUserId;

	@Override
	public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
		super.onSaveInstanceState(outState, outPersistentState);

		outState.putString(KEY_PEER_USER_ID, peerUserId);
		outPersistentState.putString(KEY_PEER_USER_ID, peerUserId);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
		super.onRestoreInstanceState(savedInstanceState, persistentState);

		if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PEER_USER_ID))
			peerUserId = savedInstanceState.getString(KEY_PEER_USER_ID);
		if (persistentState != null && persistentState.containsKey(KEY_PEER_USER_ID))
			peerUserId = persistentState.getString(KEY_PEER_USER_ID);

	}

	private EditText content;
	private Button send;

	private RecyclerView recycler_view;
	private MessagesAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Get intent data
		if (getIntent() != null) {
			peerUserId = getIntent().getStringExtra(KEY_PEER_USER_ID);
		}

		// Get from old data
		if (savedInstanceState != null) {
			peerUserId = savedInstanceState.getString(KEY_PEER_USER_ID);
		}

		setContentView(R.layout.activity_conversation);

		// Setup toolbar
		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		getSupportActionBar().setTitle(peerUserId);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);

		// Setup send
		content = findViewById(R.id.content);
		send = findViewById(R.id.send);

		send.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (content.getText().length() <= 0)
					return;

				final Context context = view.getContext();

				Message m = new Message();
				m.SenderId = DB.getCurrentUserId(context);
				m.ReceiverId = peerUserId;
				m.Content = content.getText().toString();
				m.Timestamp = System.currentTimeMillis();

				content.getText().clear();

				HashMap<Object, Object> values = new HashMap<>();
				values.put(DB.KEY_MESSAGES_SENDER_ID, m.SenderId);
				values.put(DB.KEY_MESSAGES_RECEIVER_ID, m.ReceiverId);
				values.put(DB.KEY_MESSAGES_CONTENT, m.Content);
				values.put(DB.KEY_MESSAGES_TIMESTAMP, m.Timestamp);

				DB.getFirebaseDatabase().getReference()
						.child(DB.KEY_MESSAGES)
						.push()
						.setValue(values, new DatabaseReference.CompletionListener() {
							@Override
							public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
								if (databaseError != null) {
									Toast.makeText(getApplicationContext(), "Send FAILED!", Toast.LENGTH_LONG).show();
								}
							}
						});
			}
		});

		// Setup messages
		recycler_view = findViewById(R.id.recycler_view);
		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		layoutManager.setStackFromEnd(true);
		recycler_view.setLayoutManager(layoutManager);
		recycler_view.setHasFixedSize(true);
		recycler_view.setItemViewCacheSize(7);

		adapter = new MessagesAdapter(this);

		recycler_view.setAdapter(adapter);

	}

	@Override
	protected void onStart() {
		super.onStart();

		ValueEventListener valueEventListener = new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				if (dataSnapshot.exists())
					for (DataSnapshot child : dataSnapshot.getChildren()) {
						adapter.addItem(Message.createFromData(child));
					}

				if (recycler_view != null)
					recycler_view.smoothScrollToPosition(recycler_view.getAdapter().getItemCount() - 1);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		};

		ChildEventListener childEventListener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot dataSnapshot, String s) {
				adapter.addItem(Message.createFromData(dataSnapshot));

				if (recycler_view != null)
					recycler_view.smoothScrollToPosition(recycler_view.getAdapter().getItemCount() - 1);
			}

			@Override
			public void onChildChanged(DataSnapshot dataSnapshot, String s) {

			}

			@Override
			public void onChildRemoved(DataSnapshot dataSnapshot) {
				adapter.removeItem(Message.createFromData(dataSnapshot));

				if (recycler_view != null)
					recycler_view.smoothScrollToPosition(recycler_view.getAdapter().getItemCount() - 1);
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
				.equalTo(DB.getCurrentUserId(this));
		query1.addListenerForSingleValueEvent(valueEventListener);
		query1.addChildEventListener(childEventListener);

		Query query2 = DB.getFirebaseDatabase().getReference()
				.child(DB.KEY_MESSAGES)
				.orderByChild(DB.KEY_MESSAGES_SENDER_ID)
				.equalTo(DB.getCurrentUserId(this));
		query2.addListenerForSingleValueEvent(valueEventListener);
		query2.addChildEventListener(childEventListener);

	}

	/* NOT NOW


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();

		inflater.inflate(R.menu.chat_menu, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();

		switch (itemId) {
			case R.id.action_delete_chat:
				Toast.makeText(this, "Delete chat bro", Toast.LENGTH_LONG).show();
				break;
			case R.id.action_block:
				Toast.makeText(this, "Block this nibba", Toast.LENGTH_LONG).show();
				break;

			case android.R.id.home:
				finish();
		}

		return super.onOptionsItemSelected(item);
	}

	*/

	public static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
		private final Context context;

		private final ArrayList<Message> items;

		private final String myUserId;

		public MessagesAdapter(Context context) {
			this.context = context;

			items = new ArrayList<>();

			myUserId = DB.getCurrentUserId(context);
		}

		@Override
		public int getItemCount() {
			return items.size();
		}

		@NonNull
		@Override
		public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context context = parent.getContext();

			View view = LayoutInflater.from(context).inflate(R.layout.conversation_item, parent, false);

			ViewHolder vh = new ViewHolder(view);

			return vh;
		}

		@Override
		public void onBindViewHolder(@NonNull ViewHolder vh, int position) {
			final Message item = items.get(position);

			vh.info.setText(TimeEx.getTimeAgo(item.Timestamp));

			vh.content.setText(item.Content);

			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) vh.wrapper.getLayoutParams();
			if (item.SenderId.equals(myUserId)) {
				lp.removeRule(RelativeLayout.ALIGN_PARENT_LEFT);
				lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
			} else {
				lp.removeRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				vh.wrapper.setBackground(new ColorDrawable(ContextCompat.getColor(context, R.color.colorWhite)));
				vh.content.setTextColor(ContextCompat.getColor(context, R.color.colorAccent));
			}
			vh.wrapper.setLayoutParams(lp);
		}

		public static class ViewHolder extends RecyclerView.ViewHolder {

			public View view;

			public View wrapper;
			public TextView info;
			public TextView content;

			public ViewHolder(View view) {
				super(view);

				this.view = view;

				wrapper = view.findViewById(R.id.wrapper);
				info = view.findViewById(R.id.info);
				content = view.findViewById(R.id.content);

			}

		}

		public void addItem(Message newItem) {
			if (!items.contains(newItem))
				items.add(newItem);

			Collections.sort(items, new Comparator<Message>() {
				@Override
				public int compare(Message l, Message r) {
					return Long.compare(l.Timestamp, r.Timestamp);
				}
			});

			notifyDataSetChanged();
		}

		public void removeItem(Message newItem) {
			if (items.contains(newItem))
				items.remove(newItem);

			notifyDataSetChanged();
		}

	}

}
