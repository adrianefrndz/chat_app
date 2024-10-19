package com.example.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatapp.databinding.ActivitySignInBinding;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setListeners();
    }

    private void setListeners() {
        binding.textCreateAccount.setOnClickListener(view ->
            startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.btnSignIn.setOnClickListener(view -> {
            if (isValidSignUpDetails()) {
                signIn();
            }
        });
    }
    private void signIn() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // it searches for the KEY_NAME and KEY_PASSWORD that matches the textUserName and textPassword inputs
        // under the Constants.KEY_COLLECTION_USERS collection
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_NAME, binding.textUserName.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.textPassword.getText().toString())
                .get()

                // addCompleteListener checks whether the database query is successful or not
                .addOnCompleteListener(task -> {
                   if (task.isSuccessful() && task.getResult() != null
                        && task.getResult().getDocuments().size() > 0) {

                       // since the queries are successful and if statement is true, do the following:
                       // DocumentSnapshot is an object that holds the data of this specific user document in Firestore
                       // FLAG_ACTIVITY_NEW_TASK - This flag indicates that the activity should be started in a new task.
                       // A task in Android represents a collection of activities that the user interacts with.
                       // It removes all the activities in the current task that were previously running,
                       // so only the new activity remains on top of the task stack.
                       // getApplicationContext(): Refers to the application context,
                       // which is tied to the lifecycle of the entire application and is not specific to any single activity.
                       // getApplicationContext() is safer if the activity might be destroyed
                       DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                       preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                       preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                       preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                       preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                       Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                       intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                       startActivity(intent);
                   } else {
                       loading(false);
                       showToast("Unable to sign in");
                   }
                });
    }
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void loading (Boolean isLoading) {
        if(isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnSignIn.setVisibility(View.INVISIBLE);
        } else {
            binding.btnSignIn.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }
    private Boolean isValidSignUpDetails() {

        if (binding.textUserName.getText().toString().trim().isEmpty()) {
            showToast("Enter name");
            return false;
        } else if (binding.textPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        }  else {
            return true;
        }
    }
}