package com.amr.mineapps.reindeerme;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import java.util.Date;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class AccSettingsActivity extends AppCompatActivity {

    Intent inSignInActivity;
    FirebaseAuth auth;
    FirebaseUser currentUser;
    FirebaseAuth.AuthStateListener authStateListener;
    ImageView avatarImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acc_settings);

        setSupportActionBar((Toolbar) findViewById(R.id.acc_settings_tool));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Add navigate back button

        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        //ConnectivityStatusListener.getInstance().checkConnectivity(this, null, null, null);

        TextView displayNameTextView = findViewById(R.id.display_name_in_settings_tv);
        displayNameTextView.setText(currentUser.getDisplayName());

        avatarImageView = findViewById(R.id.avatar_imageview);

        // Load user avatar
        MainActivity.usersRef.child(currentUser.getUid()).child("ppurl").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot ppurlLink) {
                if (!AccSettingsActivity.this.isDestroyed()) {
                    Glide.with(AccSettingsActivity.this)
                            .load(ppurlLink.getValue(String.class))
                            .error(R.drawable.default_pp)
                            .priority(Priority.HIGH)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(avatarImageView);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public void signOut(View v) {
        if (!ConnectivityStatusListener.getInstance().isConnected()) {
            Toast.makeText(this, R.string.no_connection, Toast.LENGTH_SHORT).show();
            return;
        }
        implementations();
        // Set the user to offline in Firebase before signing out
        Date lastOnlineDate = new Date();
        FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid()).child("state").setValue(lastOnlineDate.toString()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                auth.addAuthStateListener(authStateListener);
                auth.signOut();
            }
        });

    }

    public void deleteAcc(View v) {
        if (!ConnectivityStatusListener.getInstance().isConnected()) {
            Toast.makeText(this, R.string.no_connection, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(AccSettingsActivity.this)
                .setTitle(getString(R.string.delete_account))
                .setMessage(getString(R.string.delete_acc_confirmation))
                .setPositiveButton(getString(R.string.confirm), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        implementations();
                        // Remove friend requests sent to other users
                        MainActivity.usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                // Delete account info from the database
                                MainActivity.usersRef.child(currentUser.getUid()).setValue(null).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            auth.addAuthStateListener(authStateListener);
                                            try {
                                                auth.getCurrentUser().delete(); // Delete authentication
                                                if (auth.getCurrentUser() != null) {
                                                    auth.signOut();
                                                }
                                            } catch (Exception e) {
                                            }
                                        }
                                    }
                                });


                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Toast.makeText(AccSettingsActivity.this, "Error with deleting account.", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();

    }

    public void implementations() {
        auth = FirebaseAuth.getInstance();
        inSignInActivity = new Intent(this, SignIn.class);
        inSignInActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() == null) {
                    auth.removeAuthStateListener(authStateListener);
                    startActivity(inSignInActivity);
                    finish();
                } else {
                    firebaseAuth.getCurrentUser().reload();
                }
            }
        };
    }

    public void changeAvatar(View view) {
        Intent pickImageIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickImageIntent.setType("image/*");
        startActivityForResult(pickImageIntent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 0) {
                final ProgressBar progressBar = findViewById(R.id.change_avatar_progressbar);
                avatarImageView.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.VISIBLE);
                final Uri uri = data.getData();
                final StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                storageRef.child("/avatars").child(currentUser.getUid()).putFile(uri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                taskSnapshot.getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull final Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            // Upload new avatar
                                            MainActivity.usersRef.child(currentUser.getUid()).child("ppurl").setValue(task.getResult().toString())
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> updateProfileTast) {
                                                            if (!AccSettingsActivity.this.isDestroyed()) {
                                                                Glide.with(AccSettingsActivity.this).load(task.getResult().toString()).error(R.drawable.default_pp).diskCacheStrategy(DiskCacheStrategy.ALL).into(avatarImageView);
                                                                progressBar.setVisibility(View.INVISIBLE);
                                                                avatarImageView.setVisibility(View.VISIBLE);
                                                                Toast.makeText(AccSettingsActivity.this, R.string.done, Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(AccSettingsActivity.this, R.string.error_retrieving_link, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AccSettingsActivity.this, getString(R.string.error_uploading) + e, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

    }

}
