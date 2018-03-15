package com.apposite.nearbydemo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import java.net.MalformedURLException;
import java.net.URL;
import static com.apposite.nearbydemo.R.*;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    GoogleApiClient mGoogleApiClient;
    Message mActiveMessage;
    MessageListener mMessageListener;
    EditText userMessage;
    ImageButton send;
    TextView chat;
    FrameLayout scrollChat;
    StringBuilder allChat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);

        send = (ImageButton) findViewById(id.btnSend);
        userMessage = (EditText) findViewById(id.etMessage);
        scrollChat = (FrameLayout) findViewById(id.scrollChat);
        chat = (TextView) findViewById(id.tvChat);

        allChat= new StringBuilder();

        if(allChat.length()==0)
            scrollChat.setVisibility(View.GONE);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .enableAutoManage(this, this)
                .build();

        mMessageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                String msg = new String(message.getContent());
                try{
                    new URL(msg);
                    Uri uri = Uri.parse(msg);
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, uri);
                    PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                    NotificationCompat.Builder mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(MainActivity.this)
                            .setSmallIcon(R.drawable.bell)
                            .setContentTitle("Nearby Link Available")
                            .setContentText(msg)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent);

                    NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    mNotifyMgr.notify(0, mBuilder.build());

                    Toast.makeText(MainActivity.this, "Nearby link available.", Toast.LENGTH_SHORT).show();
                }
                catch (MalformedURLException e){
                    Toast.makeText(MainActivity.this, "Message: "+ msg, Toast.LENGTH_LONG).show();
                    if(allChat.length()==0)
                        allChat.append("Anonymous: ").append(msg);
                    else
                        allChat.append("\nAnonymous: ").append(msg);
                    chat.setText(allChat);
                    scrollChat.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onLost(final Message message) {

            }
        };

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = userMessage.getText().toString();
                if(!message.isEmpty()) {
                    publish(message);
                }
                else
                    Toast.makeText(MainActivity.this, "Empty message." , Toast.LENGTH_SHORT).show();

                View view = MainActivity.this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });
    }
    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }
    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient!=null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        unpublish();
        unsubscribe();
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        subscribe();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(MainActivity.this, "Connection Suspended.", Toast.LENGTH_SHORT).show();
    }

    private void publish(String message) {
        final String msg = message;
        mActiveMessage = new Message(msg.getBytes());
        Nearby.Messages.publish(mGoogleApiClient, mActiveMessage)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Toast.makeText(MainActivity.this, "Published successfully.", Toast.LENGTH_SHORT).show();
                            userMessage.setText("");
                            if(allChat.length()==0)
                                allChat.append("You: ").append(msg);
                            else
                                allChat.append("\nYou: ").append(msg);
                            chat.setText(allChat);
                            scrollChat.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(MainActivity.this, "Could not publish, Check your connection.", Toast.LENGTH_SHORT).show();
                        }
                        unpublish();
                    }
                });
    }
    private void unpublish() {
        if (mActiveMessage != null) {
            Nearby.Messages.unpublish(mGoogleApiClient, mActiveMessage);
            mActiveMessage = null;
        }
    }

    private void subscribe() {
        Nearby.Messages.subscribe(mGoogleApiClient, mMessageListener)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (!status.isSuccess())
                            Toast.makeText(MainActivity.this, "Could not subscribe, Check your connection.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private void unsubscribe() {
        if (mActiveMessage != null) {
            Nearby.Messages.unsubscribe(mGoogleApiClient, mMessageListener);
            mActiveMessage = null;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if(connectionResult.getResolution()==null)
            Toast.makeText(this, "Connection Failed.", Toast.LENGTH_SHORT).show();
    }
}