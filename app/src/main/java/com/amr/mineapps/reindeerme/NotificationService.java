package com.amr.mineapps.reindeerme;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import android.os.Build;
import android.util.Log;
import com.amr.mineapps.reindeerme.chat_classes.ChatPage;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class NotificationService extends FirebaseMessagingService {
    private Map<String, String> incomingData;
    public static final int CHAT = 1;
    public static final int ACCEPTED_FRIEND = 2;
    public static final int DELETED_FRIEND = 3;
    DatabaseReference usersRef;
    FirebaseUser currentUser;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            if (remoteMessage.getData() != null && currentUser.getUid().equals(remoteMessage.getData().get("receiver_id"))) {
                incomingData = remoteMessage.getData();
                switch (incomingData.get("fn_call_code")) {
                    case "chat":
                        chatReceived();
                        break;
                    case "accept_friend":
                        acceptReceived();
                        break;
                }
            } else {
                Log.v("No data received: ", "no user detected");
            }
        }
    }

    // Update token in the database when changed
    @Override
    public void onNewToken(String s) {
        try {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Intent i = new Intent(this, SignIn.class);
                startActivity(i);
            }
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
            ref.child(currentUser.getUid()).child("registration_token").setValue(s);
        } catch (Exception e) {
            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
        }
    }

    private void chatReceived() {
        final String senderId = incomingData.get("sender_id");
        final String receiverId = currentUser.getUid();
        final String msgKey = incomingData.get("msg_key");
        if (!ChatPage.isActivityVisibleForFriendId(senderId)) {
            // Send "delivered" status to firebase
            FirebaseDatabase.getInstance().goOnline();
            final DatabaseReference msgRef = usersRef.child(receiverId).child("chats").child(senderId).child(msgKey);
            msgRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        msgRef.removeEventListener(this);
                        usersRef.child(receiverId).child("chats").child(senderId).child(msgKey).child("msg_status").setValue(2).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                usersRef.child(receiverId).child("friends").child(senderId).child("last_msg").child("msg_status").setValue(2).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        FirebaseDatabase.getInstance().goOffline();
                                    }
                                });

                            }
                        });
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


            // Create notification
            final Intent inNotifChat = new Intent(this, ChatPage.class);
            inNotifChat.putExtra("sender_name", incomingData.get("sender_name"));
            inNotifChat.putExtra("sender_id", senderId);
            inNotifChat.putExtra("sender_pp", incomingData.get("sender_pp"));

            PendingIntent pendingNotifyIntent = PendingIntent.getActivity(this, CHAT, inNotifChat, PendingIntent.FLAG_UPDATE_CURRENT);

            String body = Integer.parseInt(incomingData.get("msg_type")) == 1 ? incomingData.get("msg_content") : getString(R.string.photo);
            Uri notifSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + this.getPackageName() + "/" + R.raw.notification_sound);
            NotificationManager notificationManager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
            assert notificationManager != null;
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "my_channel_01")
                    .setContentTitle(incomingData.get("sender_name"))
                    .setContentText(body)
                    .setLights(Color.GREEN, 300, 1000)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setColor(Color.GREEN)
                    .setSound(notifSound)
                    .setContentIntent(pendingNotifyIntent)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setAutoCancel(true);
            // Support newer APIs: Oreo and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel
                        ("my_channel_01_oreo",
                                "Notification About Messages Android +O",
                                NotificationManager.IMPORTANCE_HIGH);
                channel.enableLights(true);
                channel.setLightColor(Color.GREEN);

                // Creating an Audio Attribute
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(notifSound, audioAttributes);

                notificationManager.createNotificationChannel(channel);
            }


            notificationManager.notify(CHAT, notificationBuilder.build());

        }
    }

    private void acceptReceived() {
        String acceptorName = incomingData.get("friend_name");
        // Create notification
        Intent inMainActivity = new Intent(this, MainActivity.class);
        PendingIntent pendingNotifyIntent = PendingIntent.getActivity(this, ACCEPTED_FRIEND, inMainActivity, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri notifSound = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + this.getPackageName() + "/" + R.raw.notification_sound);
        NotificationManager notificationManager = (NotificationManager) getSystemService(getApplicationContext().NOTIFICATION_SERVICE);
        assert notificationManager != null;
        NotificationCompat.Builder notificationBuilder =  new NotificationCompat.Builder(this, "Friend_request_notifications")
                .setContentTitle("Friend Accepted!")
                .setContentText(acceptorName + " is now your friend!")
                .setLights(Color.GREEN, 300, 1000)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setSmallIcon(R.drawable.ic_launcher)
                .setColor(Color.BLUE)
                .setSound(notifSound)
                .setContentIntent(pendingNotifyIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true);
        // Support newer APIs: Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel
                    ("Friend_request_notifications_oreo",
                            "Friend request notifications",
                            NotificationManager.IMPORTANCE_HIGH);
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);

            // Creating an Audio Attribute
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(notifSound, audioAttributes);

            notificationManager.createNotificationChannel(channel);
        }


        notificationManager.notify(ACCEPTED_FRIEND, notificationBuilder.build());


    }

}
