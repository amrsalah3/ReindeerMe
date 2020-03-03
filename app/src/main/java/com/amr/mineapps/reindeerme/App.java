package com.amr.mineapps.reindeerme;
import android.app.Application;
import com.cookingfox.android.app_lifecycle.impl.AppLifecycleProvider;
import com.cookingfox.android.app_lifecycle.impl.listener.PersistentAppLifecycleListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.FirebaseDatabase;
import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.google.GoogleEmojiProvider;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Install Emojis
        EmojiManager.install(new GoogleEmojiProvider());
        // Enable offline capabilities for all components of the app
        if (!FirebaseApp.getApps(this).isEmpty()) {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        }
        // Trace connectivity to internet
        ConnectivityStatusListener.getInstance().checkConnectivity(this);
        // Update connectivity of the user in Firebase when app changes cycles
        AppLifecycleProvider.initialize(this);
        AppLifecycleProvider.getManager()
                .addListener(new PersistentAppLifecycleListener() {
                    @Override
                    public void onAppResumed(Class<?> origin) {
                        super.onAppResumed(origin);
                        FirebaseDatabase.getInstance().goOnline();
                    }

                    @Override
                    public void onAppStopped(Class<?> origin) {
                        super.onAppStopped(origin);
                        FirebaseDatabase.getInstance().goOffline();
                    }
                });

    }


}
