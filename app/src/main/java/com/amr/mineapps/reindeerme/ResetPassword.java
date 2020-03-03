package com.amr.mineapps.reindeerme;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.SignInMethodQueryResult;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class ResetPassword extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText emailEditText;
    private String userEmail;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);
        emailEditText = findViewById(R.id.emailTextView);
    }

    public void resetBtnClicked(View v) {
        progressBar = findViewById(R.id.reset_password_progressbar);
        auth = FirebaseAuth.getInstance();
        userEmail = emailEditText.getText().toString();
        if (Patterns.EMAIL_ADDRESS.matcher(userEmail).matches() && userEmail.trim().length() != 0) {
            // Close keyboard
            try {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (Exception e) {
            }
            userEmail = userEmail.trim();
            if (!ConnectivityStatusListener.getInstance().isConnected()) {
                Toast.makeText(this, R.string.no_connection, Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            emailExistenceCheck();
        } else {
            Toast.makeText(this, R.string.enter_a_valid_email, Toast.LENGTH_SHORT).show();
        }
    }

    private void emailExistenceCheck() {
        auth.fetchSignInMethodsForEmail(userEmail).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                if (task.getResult().getSignInMethods().size() != 0) {
                    reset();
                } else {
                    Toast.makeText(ResetPassword.this, R.string.no_such_email, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void reset() {
        auth.sendPasswordResetEmail(userEmail).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(ResetPassword.this, R.string.reset_sent, Toast.LENGTH_SHORT).show();
                    Intent inSignIn = new Intent(ResetPassword.this, SignIn.class);
                    startActivity(inSignIn);
                } else {
                    Toast.makeText(ResetPassword.this, R.string.error, Toast.LENGTH_SHORT).show();
                }
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right_forback, R.anim.slide_in_left_forback);
    }

}
