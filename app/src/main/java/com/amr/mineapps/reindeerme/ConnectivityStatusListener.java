package com.amr.mineapps.reindeerme;

import android.content.Context;
import com.androidstudy.networkmanager.Monitor;
import com.androidstudy.networkmanager.Tovuti;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Date;
import androidx.annotation.NonNull;

public class ConnectivityStatusListener {

    private static ConnectivityStatusListener cS = new ConnectivityStatusListener();
    private static boolean connected;
    private static DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
    private static DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference().child("users");
    private static FirebaseAuth auth = FirebaseAuth.getInstance();
    private ValueEventListener firebaseListener;
    private ConnectivityChangeListener OnChangeListener;

    private ConnectivityStatusListener() {
    }

    public static ConnectivityStatusListener getInstance() {
        return cS;
    }

    public void checkConnectivity(Context context) {
        firebaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue(Boolean.class)) {
                    reconnected();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        connectedRef.addValueEventListener(firebaseListener);

        Tovuti.from(context).monitor(new Monitor.ConnectivityListener() {
            @Override
            public void onConnectivityChanged(int connectionType, boolean isConnected, boolean isFast) {
                if (isConnected) {
                    FirebaseDatabase.getInstance().goOnline();
                    connected = true;
                } else {
                    FirebaseDatabase.getInstance().goOffline();
                    disconnected();
                }
            }
        });
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnectivityListener(ConnectivityChangeListener listener) {
        this.OnChangeListener = listener;
    }

    private void reconnected() {
        connected = true;
        if (auth.getCurrentUser() == null) {
            return;
        }
        // Set state to ONLINE
        usersRef.child(auth.getCurrentUser().getUid()).child("state").setValue("Online");
        // Set state to offline when disconnected
        Date lastOnlineDate = new Date();
        usersRef.child(auth.getCurrentUser().getUid()).child("state").onDisconnect().setValue(lastOnlineDate.toString());
        // Listener for when disconnected
        if (OnChangeListener != null)
            OnChangeListener.onConnected();

    }

    private void disconnected() {
        connected = false;
        if (auth.getCurrentUser() == null) {
            return;
        }
        // Listener for when reconnected
        if (OnChangeListener != null)
            OnChangeListener.onDisconnected();
    }


}
