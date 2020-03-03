package com.amr.mineapps.reindeerme;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.firebase.auth.SignInMethodQueryResult;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SignIn extends AppCompatActivity {

    private FirebaseAuth auth;
    private EditText emailEditText;
    private EditText passwordEditText;
    private String userEmail;
    private String userPassword;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        emailEditText = findViewById(R.id.emailTextView);
        passwordEditText = findViewById(R.id.passwordTextView);
    }

    public void loginBtnClicked(View v) {
        progressBar = findViewById(R.id.login_progress_Bar);
        auth = FirebaseAuth.getInstance();
        userEmail = emailEditText.getText().toString();
        userPassword = passwordEditText.getText().toString();

        if (android.util.Patterns.EMAIL_ADDRESS.matcher(userEmail).matches() && userPassword.length() != 0) {
            try {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } catch (Exception e) {
            }
            if (!ConnectivityStatusListener.getInstance().isConnected()) {
                Toast.makeText(this, R.string.no_connection, Toast.LENGTH_SHORT).show();
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE); //prevent user interactions while loading
            emailExistenceCheck(); //email existence check then sign in
        } else {
            Toast.makeText(this, R.string.type_a_valid_signin, Toast.LENGTH_SHORT).show();
        }

    }


    private void emailExistenceCheck() {
        auth.fetchSignInMethodsForEmail(userEmail).addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
            @Override
            public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                if (task.isSuccessful()) {
                    if (task.getResult().getSignInMethods().size() != 0) {
                        signIn();
                    } else {
                        Toast.makeText(SignIn.this, R.string.email_not_registered, Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//interactions back again
                    }
                }

            }
        });
    }


    public void signIn() {
        auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);//interactions back again
                        if (task.isSuccessful()) {
                            Toast.makeText(SignIn.this, R.string.welcome, Toast.LENGTH_SHORT).show();
                            Intent inMainActivity = new Intent(SignIn.this, MainActivity.class);
                            inMainActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(inMainActivity);
                            finish();
                        } else {
                            Toast.makeText(SignIn.this, R.string.sth_wrong_login, Toast.LENGTH_SHORT).show();
                        }
                    }
                });


    }


    public void GoToSignUp(View v) {
        Intent SignUp = new Intent(this, SignUp.class);
        startActivity(SignUp);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_in_left);
    }

    public void ForgotPassFun(View v) {
        Intent inResetPassword = new Intent(this, ResetPassword.class);
        startActivity(inResetPassword);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_in_left);
    }


}
