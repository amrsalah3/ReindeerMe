package com.amr.mineapps.reindeerme;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.amr.mineapps.reindeerme.chat_classes.ChatPage;
import com.amr.mineapps.reindeerme.friendrequest_classes.FriendRequest;
import com.amr.mineapps.reindeerme.friendrequest_classes.FriendRequestAdapter;
import com.amr.mineapps.reindeerme.friends_classes.Friend;
import com.amr.mineapps.reindeerme.friends_classes.FriendsAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;


public class MainActivity extends AppCompatActivity {

    public static boolean isActivityVisible = false;
    public static boolean isActivityRunning = false;
    public static FirebaseUser currentUser;
    public static DatabaseReference usersRef;
    public static String currentUserUid;
    public static ViewPagerAdapter viewPagerAdapter;
    public static FriendsAdapter friendsAdapter;
    private DataSnapshot foundUserSnapshot;
    private ViewPager viewPager;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private ChildEventListener lastMsgListener;
    private String FEID;
    private AlertDialog alertDialog;
    private EditText friendEmailInDialog;
    private TextView friendDispNameInDialog;
    private ImageView friendPPInDialog;
    private Button searchButtonInDialog;
    private Button positiveBtn;
    private FriendRequestAdapter friendRequestAdapter;
    private ListView friendsListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set custom Title bar
        setSupportActionBar((Toolbar) findViewById(R.id.mainActivity_toolbar));

        initialization();
    }

    private void initialization() {
        auth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference().child("users");
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser == null) { // Go to login
                    Intent inLogin = new Intent(MainActivity.this, SignIn.class);
                    startActivity(inLogin);
                    finish();
                } else {
                    currentUser.reload();
                    currentUserUid = currentUser.getUid();
                    usersRef.keepSynced(true);

                    // Sending Registration token to Firebase
                    sendRegToken();

                    viewPagerAdapter = new ViewPagerAdapter(MainActivity.this);
                    viewPager = findViewById(R.id.root_viewpager);
                    viewPager.setAdapter(viewPagerAdapter);

                    // List in the first page
                    listFriends();
                    // List in second page
                    listFriendRequests();
                }
            }
        };
        auth.addAuthStateListener(authStateListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem item = menu.findItem(R.id.search_button);
        MaterialSearchView searchView = findViewById(R.id.search_view);
        searchView.setMenuItem(item);

        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                friendsAdapter.filter(newText);
                return false;
            }
        });
        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {

            }

            @Override
            public void onSearchViewClosed() {
                friendsAdapter.filter("");
            }
        });

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_button:
                return true;
            case R.id.add_friend_button:
                addDialogClicked();
                return true;
            case R.id.settings_option:
                accSettings();
                return true;
            case R.id.about_option:
                Intent inAbout = new Intent(MainActivity.this, AboutActivity.class);
                startActivity(inAbout);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void sendRegToken() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (task.isSuccessful()) {
                            usersRef.child(currentUserUid).child("registration_token").setValue(task.getResult().getToken());
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to send token :(", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void listFriends() {
        ArrayList<Friend> friendsArrList = new ArrayList<>();
        friendsAdapter = new FriendsAdapter(this, friendsArrList);
        friendsListView = findViewById(R.id.friends_list_view);
        friendsListView.setAdapter(friendsAdapter);


        lastMsgListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                int lastMsgstatus;
                if (dataSnapshot.child("last_msg/msg_status").getValue(Integer.class) != null) {
                    lastMsgstatus = dataSnapshot.child("last_msg/msg_status").getValue(Integer.class);
                }else{
                    lastMsgstatus = 3;
                }
                friendsAdapter.insert(new Friend(dataSnapshot.child("ppurl").getValue(String.class), dataSnapshot.child("name").getValue(String.class), dataSnapshot.getKey(), dataSnapshot.child("last_msg"), lastMsgstatus), 0);
                viewPagerAdapter.chatNum = friendsAdapter.getCount();
                viewPagerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                int lastMsgstatus;
                if (dataSnapshot.child("last_msg/msg_status").getValue(Integer.class) != null) {
                    lastMsgstatus = dataSnapshot.child("last_msg/msg_status").getValue(Integer.class);
                } else {
                    lastMsgstatus = 3;
                }
                friendsAdapter.removeFriendObjByUid(dataSnapshot.getKey());
                friendsAdapter.insert(new Friend(dataSnapshot.child("ppurl").getValue(String.class), dataSnapshot.child("name").getValue(String.class), dataSnapshot.getKey(), dataSnapshot.child("last_msg"), lastMsgstatus), 0);
                viewPagerAdapter.chatNum = friendsAdapter.getCount();
                viewPagerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                friendsAdapter.removeFriendObjByUid(dataSnapshot.getKey());
                viewPagerAdapter.chatNum = friendsAdapter.getCount();
                viewPagerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error retrieving friends.", Toast.LENGTH_SHORT).show();
            }
        };
        usersRef.child(currentUserUid).child("friends").orderByChild("last_msg/date_in_millis").addChildEventListener(lastMsgListener);
        // Open chat page when friend clicked
        friendsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent inChatPage = new Intent(MainActivity.this, ChatPage.class);
                inChatPage.putExtra("sender_pp", friendsAdapter.getItem(position).getPpUrl());
                inChatPage.putExtra("sender_name", friendsAdapter.getItem(position).getName());
                inChatPage.putExtra("sender_id", friendsAdapter.getItem(position).getUid());
                // Set last message to seen
                friendsAdapter.getFriendObjByUid(friendsAdapter.getItem(position).getUid()).setLastMsgStatus(3);
                friendsAdapter.notifyDataSetChanged();
                startActivity(inChatPage);
            }
        });
        // Open alert dialog with list of options when friend long clicked
        friendsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                createListAlertDialog(friendsAdapter.getItem(position).getUid());
                return true;
            }
        });
    }

    private void listFriendRequests() {
        ArrayList<FriendRequest> reqArrList = new ArrayList<>();
        friendRequestAdapter = new FriendRequestAdapter(MainActivity.this, reqArrList);
        ListView friendReqListView = findViewById(R.id.req_list_view);
        friendReqListView.setAdapter(friendRequestAdapter);
        // Query for pending requests in the current user
        usersRef.child(currentUserUid).child("inpendingfriendreq").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull final DataSnapshot dataSnapshot, @Nullable String s) {
                // Get avatar
                usersRef.child(dataSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnap) {
                        // if user does not exist, skip!
                        if (!userSnap.child("ppurl").exists()) {
                            usersRef.child(currentUserUid).child("inpendingfriendreq").child(dataSnapshot.getKey()).setValue(null);
                            return;
                        }
                        String ppUrl = userSnap.child("ppurl").getValue(String.class);
                        if (ppUrl.equals("default")) {
                            friendRequestAdapter.insert(new FriendRequest(null, dataSnapshot.getValue(String.class), dataSnapshot.getKey()), 0);
                        } else {
                            friendRequestAdapter.insert(new FriendRequest(ppUrl, dataSnapshot.getValue(String.class), dataSnapshot.getKey()), 0);
                        }
                        viewPagerAdapter.reqNum = friendRequestAdapter.getCount();
                        viewPagerAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                friendRequestAdapter.removeFriendObjByUid(dataSnapshot.getKey());
                viewPagerAdapter.reqNum = friendRequestAdapter.getCount();
                viewPagerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createListAlertDialog(final String uid) {
        String[] listChoices = {getString(R.string.unfriend), getString(R.string.clear_chat)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(listChoices, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        // Unfriend
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.unfriend))
                                .setMessage(getString(R.string.unfriend_confirmation))
                                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        usersRef.child(currentUserUid).child("friends").child(uid).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(MainActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(MainActivity.this, "There is something wrong, please try again.", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                })
                                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();
                        break;

                    case 1:
                        // Delete previous chat content
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.clear_chat))
                                .setMessage(getString(R.string.clear_chat_confirmation))
                                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        usersRef.child(currentUserUid).child("friends").child(uid).child("last_msg").setValue(null);
                                        usersRef.child(currentUserUid).child("chats").child(uid).setValue(null);
                                        Toast.makeText(MainActivity.this, "Done", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                    }
                                }).show();
                        break;
                }
            }
        });
        builder.create().show();
    }

    public void addDialogClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.add_new_friend))
                .setView(this.getLayoutInflater().inflate(R.layout.add_friend_dialog, null))
                .setPositiveButton(getString(R.string.add), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        usersRef.child(currentUserUid).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.child("friends").exists() && dataSnapshot.child("friends").child(foundUserSnapshot.getKey()).exists()) {
                                    Toast.makeText(MainActivity.this, "You are already friends.", Toast.LENGTH_SHORT).show();
                                } else if (dataSnapshot.child("inpendingfriendreq").exists() && dataSnapshot.child("inpendingfriendreq").child(foundUserSnapshot.getKey()).exists()) {
                                    Toast.makeText(MainActivity.this, "There is already a pending request.", Toast.LENGTH_SHORT).show();
                                } else if (dataSnapshot.child("outpendingfriendreq").exists() && dataSnapshot.child("outpendingfriendreq").child(foundUserSnapshot.getKey()).exists()) {
                                    Toast.makeText(MainActivity.this, "There is already a pending request.", Toast.LENGTH_SHORT).show();
                                } else {
                                    usersRef.child(currentUser.getUid()).child("outpendingfriendreq").child(foundUserSnapshot.getKey()).setValue(foundUserSnapshot.child("name").getValue(String.class))
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(MainActivity.this, "Friend request Sent.", Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });


        alertDialog = builder.create();
        alertDialog.show();

        positiveBtn = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        positiveBtn.setClickable(false);
        positiveBtn.setTextColor(Color.GRAY);

        friendEmailInDialog = alertDialog.findViewById(R.id.email_edittext_indialog);
        searchButtonInDialog = alertDialog.findViewById(R.id.search_btn_indialog);
        friendDispNameInDialog = alertDialog.findViewById(R.id.friend_displayname_indialog);
        friendPPInDialog = alertDialog.findViewById(R.id.friend_PP_indialog);

        searchButtonInDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FEID = friendEmailInDialog.getText().toString(); // FEID : friendEmailInDialog
                if (!TextUtils.isEmpty(friendEmailInDialog.getText()) && Patterns.EMAIL_ADDRESS.matcher(FEID).matches()) {
                    searchForPeopleFunc();
                }

            }
        });
    }

    private void searchForPeopleFunc() {
        if (!FEID.equals(currentUser.getEmail())) {
            searchButtonInDialog.setVisibility(View.INVISIBLE);
            final ProgressBar progressBar = alertDialog.findViewById(R.id.addfriend_progressBar);
            progressBar.setVisibility(View.VISIBLE);
            auth.fetchSignInMethodsForEmail(FEID).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                @Override
                public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                    searchButtonInDialog.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);
                    // Check if the email search is authenticated
                    if (task.isSuccessful()) {
                        if (task.getResult().getSignInMethods().size() != 0) {
                            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    for (DataSnapshot ds : dataSnapshot.getChildren()) {
                                        if (ds.child("email").getValue(String.class).equals(FEID)) { // if the search value matches the email of the snapshot

                                            foundUserSnapshot = ds;

                                            friendEmailInDialog.setVisibility(View.GONE);

                                            friendDispNameInDialog.setText(ds.child("name").getValue(String.class));
                                            friendDispNameInDialog.setVisibility(View.VISIBLE);

                                            friendPPInDialog.setVisibility(View.VISIBLE);
                                            Glide.with(MainActivity.this).load(ds.child("ppurl").getValue(String.class)).error(R.drawable.default_pp).diskCacheStrategy(DiskCacheStrategy.ALL).circleCrop().into(friendPPInDialog);

                                            positiveBtn.setClickable(true);
                                            positiveBtn.setTextColor(Color.parseColor("#d479ff")); //clickable

                                            break;
                                        }
                                    }

                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Toast.makeText(MainActivity.this, "Error with querying", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(MainActivity.this, "Not found.", Toast.LENGTH_SHORT).show();
                        }

                    }
                }
            });
        } else {
            Toast.makeText(this, "U cannot add yourself!", Toast.LENGTH_SHORT).show();
        }

    }

    public void accSettings() {
        Intent inAccSettings = new Intent(this, AccSettingsActivity.class);
        startActivity(inAccSettings);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (authStateListener != null) {
            auth.removeAuthStateListener(authStateListener);
        }
        if (lastMsgListener != null) {
            usersRef.child(currentUserUid).child("friends").removeEventListener(lastMsgListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityVisible = true;
        isActivityRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        isActivityVisible = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        isActivityVisible = false;
        isActivityRunning = false;
        // Remove references to static variables
        friendsAdapter = null;
        currentUser = null;
        currentUserUid = null;
        usersRef = null;
        viewPagerAdapter = null;
    }

}
