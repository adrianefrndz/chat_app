package com.example.chatapp.firebase;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.chatapp.ChatActivity;
import com.example.chatapp.R;
import com.example.chatapp.utilities.Constants;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String title = remoteMessage.getData().get("title");
        String message = remoteMessage.getData().get("message");
        String receiverUserId = remoteMessage.getData().get("receiverUserId");
        String currentUserId = "YOUR_CURRENT_USER_ID"; // Get the current user's ID
        boolean isReceiverAvailable = false;

        // Check if the message is meant for the current user
        if (receiverUserId != null && receiverUserId.equals(currentUserId)) {
            // Retrieve the receiver's token from Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(receiverUserId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            String receiverToken = task.getResult().getString("token");
                            if (receiverToken != null) {
                                // If the receiver has a valid token, proceed to get the last message
                                getLastMessage(receiverUserId, isReceiverAvailable, receiverToken, title, message);
                            } else {
                                Log.d(TAG, "Receiver user token is null. Notification not sent.");
                            }
                        } else {
                            Log.d(TAG, "Failed to get receiver user token.");
                        }
                    });
        }
    }

    private void getLastMessage(String receiverUserId, boolean isReceiverAvailable, String receiverToken, String title, String message) {
        // Only proceed if the receiver is unavailable and has a valid FCM token
        if (!isReceiverAvailable && receiverToken != null) {
            Log.d("Notification", "Receiver is unavailable and has a token. Fetching last message...");

            // Query Firestore to get the latest message from the conversation collection
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            // Retrieve the last message document
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);

                            // Extract last message and sender's name
                            String lastMessage = document.getString(Constants.KEY_LAST_MESSAGE);
                            String senderName = document.getString(Constants.KEY_SENDER_NAME);
                            Log.d("Firestore", "Last Message: " + lastMessage);
                            Log.d("Firestore", "Sender Name: " + senderName);

                            // Send notification with last message
                            sendNotification(title, senderName + ": " + lastMessage);
                        } else {
                            Log.d("Firestore", "No messages found in the conversation.");
                        }
                    })
                    .addOnFailureListener(e -> Log.d("Firestore", "Error fetching last message", e));
        } else {
            Log.d("Notification", "Notification not sent because receiver is available or has no token.");
        }
    }

    private void sendNotification(String title, String messageBody) {
        // Create a notification channel and build the notification
        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        String channelId = "chat_notification_channel";
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_notif)
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create a notification channel for Android Oreo and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Chat Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        // Use a unique ID to prevent overwriting notifications
        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
        Log.d("Notification", "Notification sent.");
    }
}

