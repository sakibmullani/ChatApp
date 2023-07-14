package com.example.chatapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.databinding.ActivityAuthenticationBinding;
import com.example.chatapp.databinding.ActivityMainBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AuthenticationActivity extends AppCompatActivity {

    ActivityAuthenticationBinding binding;
    String name, email, pass;
    DatabaseReference databaseReference;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthenticationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseReference = FirebaseDatabase.getInstance().getReference("user");
        progressDialog = new ProgressDialog(AuthenticationActivity.this);
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);

        binding.Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                email = binding.enterEmail.getText().toString().trim();
                pass = binding.enterPass.getText().toString().trim();

                if (email.isEmpty()) {
                    // Display an error message for empty email
                    Toast.makeText(AuthenticationActivity.this, "Please enter an email", Toast.LENGTH_SHORT).show();
                } else if (pass.isEmpty()) {
                    // Display an error message for empty password
                    Toast.makeText(AuthenticationActivity.this, "Please enter a password", Toast.LENGTH_SHORT).show();
                } else {
                    loginNow();
                }
            }
        });

        binding.signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                name = binding.enterName.getText().toString();
                email = binding.enterEmail.getText().toString();
                pass = binding.enterPass.getText().toString();

                if (name.isEmpty()) {
                    // Display an error message for empty name
                    Toast.makeText(AuthenticationActivity.this, "Please enter a name", Toast.LENGTH_SHORT).show();
                } else if (email.isEmpty()) {
                    // Display an error message for empty email
                    Toast.makeText(AuthenticationActivity.this, "Please enter an email", Toast.LENGTH_SHORT).show();
                } else if (pass.isEmpty()) {
                    // Display an error message for empty password
                    Toast.makeText(AuthenticationActivity.this, "Please enter a password", Toast.LENGTH_SHORT).show();
                } else {
                    signUpNow();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(AuthenticationActivity.this, MainActivity.class));
            finish();
        }
    }

    private void loginNow() {
        progressDialog.show(); // Show progress dialog

        FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email.trim(), pass)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // Check if the user has completed the sign-up process
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            // User is signed up, proceed to MainActivity
                            progressDialog.dismiss(); // Dismiss progress dialog
                            startActivity(new Intent(AuthenticationActivity.this, MainActivity.class));
                            finish();
                        } else {
                            // User is not signed up, show an error message
                            progressDialog.dismiss(); // Dismiss progress dialog
                            Toast.makeText(AuthenticationActivity.this, "User not found. Please sign up first.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        progressDialog.dismiss(); // Dismiss progress dialog
                        Toast.makeText(AuthenticationActivity.this, "Login failed. Please try again or sign up first.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signUpNow() {
        progressDialog.show(); // Show progress dialog

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email.trim(), pass)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        progressDialog.dismiss(); // Dismiss progress dialog

                        UserProfileChangeRequest userProfileChangeRequest = new UserProfileChangeRequest
                                .Builder()
                                .setDisplayName(name).build();

                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        firebaseUser.updateProfile(userProfileChangeRequest);

                        UserModel userModel = new UserModel(FirebaseAuth.getInstance().getUid(), name, email, pass);
                        databaseReference.child(FirebaseAuth.getInstance().getUid()).setValue(userModel);

                        startActivity(new Intent(AuthenticationActivity.this, MainActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        progressDialog.dismiss(); // Dismiss progress dialog
                        Toast.makeText(AuthenticationActivity.this, "Sign up failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
