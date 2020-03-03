package com.amr.mineapps.reindeerme;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SignUp extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText firstNameEditText;
    private EditText lastNameEditText;
    private String userEmail;
    private String userPassword;
    private String firstName;
    private String lastName;
    private ProgressBar progressBar;
    private String dispName = "default";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        emailEditText = findViewById(R.id.emailTextView);
        passwordEditText = findViewById(R.id.passwordTextView);
        firstNameEditText = findViewById(R.id.first_name);
        lastNameEditText = findViewById(R.id.last_name);

    }

    public void signUpBtnClicked(View v) {
        progressBar = findViewById(R.id.signup_progressBar);

        auth = FirebaseAuth.getInstance();

        userEmail = emailEditText.getText().toString();
        userPassword = passwordEditText.getText().toString();
        firstName = firstNameEditText.getText().toString().trim();
        lastName = lastNameEditText.getText().toString().trim();

        if (firstName.trim().length() != 0 && lastName.trim().length() != 0 && Patterns.EMAIL_ADDRESS.matcher(userEmail).matches() && userPassword.length() >= 6) {
            // close keyboard
            try {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (Exception e) {
            }
            if(!ConnectivityStatusListener.getInstance().isConnected()){
                Toast.makeText(this, R.string.no_connection, Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE); //prevent user interactions while loading
            emailExistenceCheck(); // Email existence check then sign up
        } else {
            Toast.makeText(this, R.string.enter_valid_singup, Toast.LENGTH_SHORT).show();
        }
    }


    private void emailExistenceCheck() {
        auth.fetchSignInMethodsForEmail(userEmail).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                if (task.isSuccessful()){
                    if (task.getResult().getSignInMethods().size() == 0) {
                        signUp();
                    } else {
                        Toast.makeText(SignUp.this, R.string.already_registered, Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//interactions back again
                    }
                }


            }
        });
    }


    public void signUp() {
        auth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//interactions back again
                        if (task.isSuccessful()) {

                            FirebaseUser currentuser = auth.getCurrentUser();
                            dispName = firstName + " " + lastName;
                            UserProfileChangeRequest displayName = new UserProfileChangeRequest.Builder().setDisplayName(dispName).build();
                            currentuser.updateProfile(displayName); //sets display name for the user

                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users").child(currentuser.getUid());
                            ref.setValue(new UserFB(currentuser.getUid(), currentuser.getEmail(), dispName)); //creates an object with this new account in the firebase

                            Intent inMainActivity = new Intent(SignUp.this, MainActivity.class);
                            inMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(inMainActivity);
                            finish();
                        } else {
                            Toast.makeText(SignUp.this, R.string.signup_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.slide_in_right_forback, R.anim.slide_in_left_forback);
    }
}
