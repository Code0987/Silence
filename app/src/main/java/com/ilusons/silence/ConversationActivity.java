package com.ilusons.silence;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class ConversationActivity extends AppCompatActivity {

    Toolbar toolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        toolbar = (Toolbar)findViewById(R.id.toolBarChat);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.chat_menu , menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        //Just for testing

        switch (itemId){
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
}
