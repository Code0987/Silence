package com.ilusons.silence;

import android.content.Context;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
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
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.ilusons.silence.data.DB;
import com.ilusons.silence.data.Message;
import com.ilusons.silence.data.User;
import com.ilusons.silence.views.ConversationsFragment;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ConversationActivity extends AppCompatActivity {

	public final static String KEY_PEER_USER_ID = "peer_user_id";

	private String peerUserId;

	Toolbar toolbar;

	private EditText content;
	private Button send;

	RecyclerView rc_view;
	ChatAdaptor chatAdaptor;

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

		getSupportActionBar().setTitle(null);
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

				Toast.makeText(getApplicationContext(), "Sending ...", Toast.LENGTH_LONG).show();

				DB.getFirebaseDatabase().getReference()
						.child(DB.KEY_MESSAGES)
						.push()
						.setValue(values, new DatabaseReference.CompletionListener() {
							@Override
							public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
								if (databaseError == null) {
									Toast.makeText(getApplicationContext(), "Sent!", Toast.LENGTH_LONG).show();
								}
							}
						});
			}
		});

		// Setup messages

		rc_view = (RecyclerView) findViewById(R.id.rv_message_list);
		LinearLayoutManager layoutManager = new LinearLayoutManager(this);
		layoutManager.setStackFromEnd(true);
		rc_view.setLayoutManager(layoutManager);

		rc_view.setHasFixedSize(true);

		chatAdaptor = new ChatAdaptor(this);

		rc_view.setAdapter(chatAdaptor);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();

		inflater.inflate(R.menu.chat_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int itemId = item.getItemId();

		//Just for testing

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

		//Delete after testing

		return super.onOptionsItemSelected(item);
	}

	public static class ChatAdaptor extends RecyclerView.Adapter<ChatAdaptor.ChatViewHolder> {

		private final Context context;

		private final ArrayList<String> messages;

		public ChatAdaptor(Context context) {
			this.context = context;
			messages = new ArrayList<>(Arrays.asList("m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8", "m9", "m10"));
		}

		@Override
		public int getItemCount() {
			return messages.size();
		}

		@NonNull
		@Override
		public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			Context context = parent.getContext();
			int layoutItem = R.layout.message_bubble;
			LayoutInflater inflater = LayoutInflater.from(context);
			boolean attachImmedietly = false;

			View view = inflater.inflate(layoutItem, parent, attachImmedietly);
			ChatViewHolder cv = new ChatViewHolder(view);

			return cv;
		}

		@Override
		public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
			String m = messages.get(position);

			holder.tv_message.setText(m);
		}

		public static class ChatViewHolder extends RecyclerView.ViewHolder {

			public TextView tv_message;

			public ChatViewHolder(View itemView) {
				super(itemView);

				tv_message = itemView.findViewById(R.id.tv_message_bubble);

			}

		}

	}

}
