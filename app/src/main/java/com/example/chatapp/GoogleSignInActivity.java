package com.example.chatapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class GoogleSignInActivity extends AppCompatActivity {

    private Button signInButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_google_sign_in);

        // Find the sign-in button in the layout
        signInButton = findViewById(R.id.btn_sgnin);

        // Configure Google Sign-In
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail() // Request user's email
                .build(); // Build the GoogleSignInOptions object

        // Create a GoogleSignInClient which allows users to sign in with their Google account
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        // Check if user is already signed in
        GoogleSignInAccount googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);

        // If user is already signed in, navigate to MainActivity
        if (googleSignInAccount != null) {
            signInButton.setVisibility(View.GONE);
            startActivity(new Intent(GoogleSignInActivity.this, SignInActivity.class)); // Navigate to MainActivity
            finish();
        }
        // Register an ActivityResultLauncher for Google Sign-In to handle the result
        ActivityResultLauncher<Intent> activityResultLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleSignInTask(task); // Handle the sign-in result
                    } else {
                        // Handle cases where sign-in failed or was canceled
                        Log.e("GoogleSignIn", "Sign-in failed or canceled");
                    }
                });

        // Set click listener for the sign-in button and launch Google Sign-In intent
        signInButton.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent(); // Get the sign-in intent
            activityResultLauncher.launch(signInIntent); // Launch the sign-in intent
        });
    }
    // Handle sign-in result, update UI accordingly, and navigate to MainActivity
    private void handleSignInTask(Task<GoogleSignInAccount> task) {
        // Try to get GoogleSignInAccount from the task
        try {
            GoogleSignInAccount googleSignInAccount = task.getResult(ApiException.class); // Get the signed-in account

            final String getFullName = googleSignInAccount.getDisplayName(); // Get full name
            final String getEmail = googleSignInAccount.getEmail(); // Get email
            final String getPhotoUrl = String.valueOf(googleSignInAccount.getPhotoUrl()); // Get photo URL

            signInButton.setVisibility(View.GONE);
            startActivity(new Intent(GoogleSignInActivity.this, SignInActivity.class));
            finish();
            Toast.makeText(this, "Sign in successful", Toast.LENGTH_SHORT).show();
        }

        // Handle sign-in failure
        catch (ApiException e) {
            e.printStackTrace(); // Print the exception stack trace
            Toast.makeText(this, "Sign in failed", Toast.LENGTH_SHORT).show();
            Log.d("TAG", "handleSignInTask failed");
        }
    }
}