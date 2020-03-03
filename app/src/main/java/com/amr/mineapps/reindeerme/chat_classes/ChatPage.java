package com.amr.mineapps.reindeerme.chat_classes;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.amr.mineapps.reindeerme.ConnectivityStatusListener;
import com.amr.mineapps.reindeerme.R;
import com.amr.mineapps.reindeerme.SignIn;
import com.amr.mineapps.reindeerme.ConnectivityChangeListener;
import com.amr.mineapps.reindeerme.ViewImageActivity;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;


public class ChatPage extends AppCompatActivity {
    String messageOfEditText;
    public static boolean isActivityVisible = false;
    public static boolean isActivityRunning = false;
    public static final int PENDING = 0, SENT = 1, DELIVERED = 2, SEEN = 3;
    public static final int SENDER_CODE = 0, RECEIVER_CODE = 1;
    public static final int MSG_TEXT = 1, MSG_IMAGE = 2;
    private boolean replyEnabled = false;
    private String replyKey = null;
    ConstraintLayout replyRootView;
    TextView senderNameTv;
    TextView captionTextView;
    ImageView captionImageView;
    public static String chatID;
    private String chatName;
    private String chatPpUrl;
    public static MessageAdapter msgAdapter;
    private ArrayList<Message> arrList;
    private ListView msgListView;
    private FirebaseUser currentUser;
    private String currentUserUid;
    private DatabaseReference usersRef;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ChildEventListener chatListener;
    private TextView friendStateTextView;
    ConstraintLayout rootView;
    EmojiEditText typeMsgEditText;
    EmojiPopup emojiPopup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_page);

        setSupportActionBar((Toolbar) findViewById(R.id.chat_toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Add navigate back button
        rootView = findViewById(R.id.chatpage_rootview);
        typeMsgEditText = findViewById(R.id.typeMessage_edittext);
        emojiPopup = EmojiPopup.Builder.fromRootView(rootView).build(typeMsgEditText);

        findViewById(R.id.typeMessage_edittext);

        auth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser == null) {
                    Intent inSignIn = new Intent(ChatPage.this, SignIn.class);
                    startActivity(inSignIn);
                    finish();
                } else {
                    currentUser.reload();
                    currentUserUid = currentUser.getUid();
                    usersRef = FirebaseDatabase.getInstance().getReference().child("users");

                    chatName = getIntent().getExtras().getString("sender_name");
                    chatID = getIntent().getExtras().getString("sender_id");
                    chatPpUrl = getIntent().getExtras().getString("sender_pp");

                    // Set title bar properties
                    TextView titleText = findViewById(R.id.friend_in_chat_name);
                    titleText.setText(chatName);
                    ImageView titleAvatar = findViewById(R.id.friend_in_chat_avatar);
                    Glide.with(ChatPage.this).load(chatPpUrl).circleCrop().error(R.drawable.default_pp).into(titleAvatar);
                    friendStateTextView = findViewById(R.id.friend_state_textview);
                    friendConnectivityState();

                    arrList = new ArrayList<>();
                    msgAdapter = new MessageAdapter(ChatPage.this, arrList);
                    msgListView = findViewById(R.id.messagesListView);
                    msgListView.setAdapter(msgAdapter);

                    msgListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            Message message = msgAdapter.getItem(position);
                            if (message.getMsg_type() == MSG_IMAGE) {
                                Intent inViewImage = new Intent(ChatPage.this, ViewImageActivity.class);
                                inViewImage.putExtra("image_url", message.getMsg_content());
                                startActivity(inViewImage);
                            }
                        }
                    });
                    msgListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                            createListAlertDialog(position);
                            return false;
                        }
                    });

                    // Receiving all messages from database to the app and listen for the upcoming
                    listenerForUpcomingMessages();
                    changeStatusOfMessagesWhenConnectivityReturns();
                }
            }
        };
        auth.addAuthStateListener(authStateListener);
    }

    private void changeStatusOfMessagesWhenConnectivityReturns() {
        final TextView connectivityStateOfFriend = findViewById(R.id.friend_state_textview);
        ConnectivityStatusListener.getInstance().setConnectivityListener(new ConnectivityChangeListener() {
            @Override
            public void onConnected() {
                connectivityStateOfFriend.setVisibility(View.VISIBLE);
                if (msgAdapter != null) {
                    // Make sure to change last messages to "SENT"
                    for (int i = msgAdapter.getCount() - 1; i >= 0; i--) {
                        Message msg = msgAdapter.getItem(i);
                        if (msg.getmSentOrRecieved() == SENDER_CODE) {
                            if (msg.getMsg_status() == PENDING) {
                                msg.setMsg_status(SENT);
                            } else {
                                break;
                            }
                        }

                    }
                    msgAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onDisconnected() {
                connectivityStateOfFriend.setVisibility(View.GONE);
            }
        });

    }

    private void listenerForUpcomingMessages() {
        chatListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                Message message = dataSnapshot.getValue(Message.class);
                if (message.getDeleted() == 2){return;}
                message.setmKey(dataSnapshot.getKey());
                String senderId = message.getSender_id();
                Log.v("MMMMM", "Hi from chat!");
                if (isActivityRunning && senderId != null) {
                    // If the underlying message is sent by the current user
                    if (senderId.equals(currentUserUid)) {
                        int status = message.getMsg_status();
                        message.setmSentOrRecieved(SENDER_CODE);
                        msgAdapter.add(message);
                        // If message status is PENDING, check live connection to firebase
                        if (status == PENDING) {
                            if (ConnectivityStatusListener.getInstance().isConnected()) {
                                // If connected then change status to SENT
                                usersRef.child(currentUserUid).child("chats").child(chatID).child(dataSnapshot.getKey()).child("msg_status").setValue(SENT);
                                usersRef.child(currentUserUid).child("friends").child(chatID).child("last_msg").child("msg_status").setValue(SENT);
                            }
                        }

                    } else { // Otherwise is received
                        // First check that the current chat activity is for that specific friend; for not showing other people's new messages when added
                        if (senderId.equals(chatID)) {
                            message.setmSentOrRecieved(RECEIVER_CODE);
                            msgAdapter.add(message);
                            // If the chat page is visible then set the received messages to SEEN in firebase
                            if (isActivityVisibleForFriendId(senderId)) {
                                msgExistenceCheck(senderId, currentUserUid, message.getmKey(), SEEN);
                            }
                        }
                    }
                    msgListView.setSelection(msgAdapter.getCount() - 1);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                // First check that the current chat activity is for the current friend
                if (isActivityRunningForFriendId(dataSnapshot.getRef().getParent().getKey())) {
                    // if a message status has been changed update it
                    Message msg = msgAdapter.getMsgByKey(dataSnapshot.getKey());
                    if (msg != null && msg.getSender_id() != null) {
                        // A message status has changed
                        int newStatus = dataSnapshot.child("msg_status").getValue(Integer.class);
                        if (newStatus > msg.getMsg_status()) {
                            msg.setMsg_status(newStatus);
                            msgAdapter.notifyDataSetChanged();
                        }

                        // if a message has been deleted update the chat
                        if (dataSnapshot.child("deleted").getValue(Integer.class) == 1) {
                            msg.setDeleted(1);
                            msg.setReply_key(null);
                            msg.setMsg_type(MSG_TEXT);
                            msg.setMsg_content("This message has been deleted.");
                            msgAdapter.notifyDataSetChanged();
                        }
                        else if(dataSnapshot.child("deleted").getValue(Integer.class) == 2){
                            msgAdapter.remove(msg);
                            msgAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ChatPage.this, "Error receiving message", Toast.LENGTH_SHORT).show();
                throw databaseError.toException();
            }
        };
        usersRef.child(currentUserUid).child("chats").child(chatID).addChildEventListener(chatListener);

    }

    private void friendConnectivityState() {
        usersRef.child(chatID).child("state").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String s = dataSnapshot.getValue(String.class);
                // If state is not "Online" but last online date
                if (s.length() > 6) {
                    s = s.substring(4, 20);
                }
                friendStateTextView.setVisibility(View.VISIBLE);
                friendStateTextView.setText(s);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void OpenGallery(View view) {
        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickImageIntent.setType("image/*");
        startActivityForResult(pickImageIntent, 0);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 0) {
                final Uri uri = data.getData();
                final StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                storageRef.child("images").child(uri.getLastPathSegment()).putFile(uri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            send(MSG_IMAGE, task.getResult().toString());
                                        } else {
                                            Toast.makeText(ChatPage.this, "Error retrieving link", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ChatPage.this, "Error uploading:" + e, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }

    public void sendTextMsg(View view) {
        typeMsgEditText = findViewById(R.id.typeMessage_edittext);
        messageOfEditText = typeMsgEditText.getText().toString();
        if (!TextUtils.isEmpty(messageOfEditText) && messageOfEditText.trim().length() > 0) {

            int txtLength = messageOfEditText.length();
            while (messageOfEditText.charAt(txtLength - 1) == ' ') {
                // While there is a white space at the end of the text, delete it!
                messageOfEditText = messageOfEditText.substring(0, txtLength - 1);
                txtLength--;
            }
            if (messageOfEditText.trim().length() > 0) {
                typeMsgEditText.setText("");
                // Send that message to firebase
                send(MSG_TEXT, messageOfEditText);
            }
        }
    }

    private void send(int msgType, String msgTextOrImageUrl) {
        Date date = new Date();
        final Message message = new Message(msgTextOrImageUrl, msgType, PENDING, replyKey, 0, currentUserUid, currentUser.getDisplayName(), null, SENDER_CODE, date.toString(), date.getTime());
        // if this msg is a reply to a previous one
        if (replyEnabled) {
            dismissReply();
        }
        MediaPlayer mp = MediaPlayer.create(this, R.raw.sent_sound_2);
        mp.start();

        usersRef.child(currentUserUid).child("chats").child(chatID).push().setValue(message).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    // Add the last message to the "friends/(friend)" node
                    usersRef.child(currentUserUid).child("friends").child(chatID).child("last_msg").setValue(message);
                } else {
                    Toast.makeText(ChatPage.this, "Fail", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void createListAlertDialog(final int position) {
        String[] listChoices = {"Reply", "Copy", "Delete"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(listChoices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        reply(position);
                        break;
                    case 1:
                        copy(position);
                        break;
                    case 2:
                        delete(position);
                        break;
                }
            }
        });
        builder.create().show();
    }


    private void reply(int position) {
        Message message = msgAdapter.getItem(position);
        replyRootView = findViewById(R.id.reply_rootview);
        senderNameTv = findViewById(R.id.sender_name_textview);
        captionTextView = findViewById(R.id.reply_textview);
        captionImageView = findViewById(R.id.reply_imageview);
        replyRootView.setVisibility(View.VISIBLE);

        String senderName = message.getmSentOrRecieved() == ChatPage.SENDER_CODE ? getString(R.string.replying_to_urself) : getString(R.string.replying_to) + chatName;
        senderNameTv.setText(senderName);
        senderNameTv.setVisibility(View.VISIBLE);

        if (message.getMsg_type() == ChatPage.MSG_TEXT) {
            captionTextView.setText(message.getMsg_content());
            captionImageView.setVisibility(View.GONE);
            captionTextView.setVisibility(View.VISIBLE);
        } else {
            captionImageView.setVisibility(View.VISIBLE);
            Glide.with(this).load(message.getMsg_content()).into(captionImageView);
            captionTextView.setText(R.string.photo);
            captionTextView.setVisibility(View.VISIBLE);
        }
        replyEnabled = true;
        replyKey = message.getmKey();
        // Dismiss reply view when clicked on
        replyRootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissReply();
            }
        });
    }

    private void dismissReply() {
        replyRootView.setVisibility(View.GONE);
        senderNameTv.setVisibility(View.GONE);
        captionImageView.setVisibility(View.GONE);
        captionTextView.setVisibility(View.GONE);
        replyEnabled = false;
        replyKey = null;
    }

    private void copy(int position) {
        Message message = msgAdapter.getItem(position);
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Message text", message.getMsg_content()));
        Toast.makeText(ChatPage.this, R.string.message_copied, Toast.LENGTH_SHORT).show();
    }

    private void delete(int position) {
        Message message = msgAdapter.getItem(position);
        Map<String, Object> deleteMsg = new HashMap<>();
        if(message.getmSentOrRecieved() == SENDER_CODE){
            // Synchronous delete
            deleteMsg.put("deleted", 1);
            deleteMsg.put("msg_content", getString(R.string.deleted_msg));
            deleteMsg.put("msg_type", MSG_TEXT);
            deleteMsg.put("reply_key", null);
            usersRef.child(currentUserUid).child("chats").child(chatID).child(message.getmKey()).updateChildren(deleteMsg);
            usersRef.child(chatID).child("chats").child(currentUserUid).child(message.getmKey()).updateChildren(deleteMsg);
        }else{
            // Local delete
            deleteMsg.put("deleted", 2);
            usersRef.child(currentUserUid).child("chats").child(chatID).child(message.getmKey()).updateChildren(deleteMsg);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (authStateListener != null) {
            auth.removeAuthStateListener(authStateListener);
        }


        try {
            EditText editText = findViewById(R.id.typeMessage_edittext);
            SharedPreferences.Editor sharedPreferencesEditor = getSharedPreferences("save_chat_text", MODE_PRIVATE).edit();
            sharedPreferencesEditor.putString("text", editText.getText().toString());
            sharedPreferencesEditor.apply();
        } catch (Exception e) {
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
        isActivityRunning = true;

        SharedPreferences sharedPreferencesEditor = getSharedPreferences("save_chat_text", MODE_PRIVATE);
        EditText editText = findViewById(R.id.typeMessage_edittext);
        editText.setText(sharedPreferencesEditor.getString("text", ""));
        if (editText.getText().toString().trim().length() > 0)
            editText.setSelection(editText.length());

        if (msgAdapter != null) {
            // Make sure to change last messages to "SEEN"
            for (int i = msgAdapter.getCount() - 1; i >= 0; i--) {
                Message msg = msgAdapter.getItem(i);
                if (msg.getmSentOrRecieved() == RECEIVER_CODE) {
                    if (msg.getMsg_status() != SEEN) {
                        msgExistenceCheck(chatID, currentUserUid, msg.getmKey(), SEEN);
                    } else {
                        break;
                    }
                }

            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatListener != null) {
            usersRef.child(currentUserUid).child("chats").child(chatID).removeEventListener(chatListener);
        }
        isActivityVisible = false;
        isActivityRunning = false;
        // Remove reference to static adapter
        msgAdapter = null;
    }

    public static boolean isActivityRunningForFriendId(String id) {
        if (isActivityRunning && chatID.equals(id)) {
            return true;
        }
        return false;
    }

    public static boolean isActivityVisibleForFriendId(String id) {
        if (isActivityVisible && chatID.equals(id)) {
            return true;
        }
        return false;
    }

    private void msgExistenceCheck(final String senderId, final String receiverId, final String msgKey, final int newStatus) {
        usersRef.child(receiverId).child("chats").child(senderId).child(msgKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    usersRef.child(receiverId).child("chats").child(senderId).child(msgKey).child("msg_status").setValue(newStatus);
                    usersRef.child(receiverId).child("friends").child(senderId).child("last_msg").child("msg_status").setValue(newStatus);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void openEmojiPad(View view) {
        emojiPopup.toggle();
    }
}
