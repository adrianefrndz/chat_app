package com.example.chatapp;

// AuthHelper.java
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.HashMap;

public class AuthHelper {

    private final PreferenceManager preferenceManager;

    public AuthHelper(PreferenceManager preferenceManager) {
        this.preferenceManager = preferenceManager;
    }

    public void signOut(Context context) {
        Toast.makeText(context, "Signing out...", Toast.LENGTH_SHORT).show();

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));

        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());

        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    context.startActivity(new Intent(context, SignInActivity.class));
                    if (context instanceof Activity) {
                        ((Activity) context).finish();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Unable to sign out", Toast.LENGTH_SHORT).show()
                );
    }
}
