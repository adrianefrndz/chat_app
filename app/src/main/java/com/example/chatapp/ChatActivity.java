package com.example.chatapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.FragmentManager;

import com.example.chatapp.adapters.ChatAdapter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;
    private String conversationId = null;
    private Boolean isReceiverAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());

        loadReceiverDetails();
        setListeners();
        init();
        listenMessages();
    }

    private void setListeners() {

        binding.userInfo.setOnClickListener(v -> showInfoFragment());
        binding.btnBack.setOnClickListener(v -> onBackPressed());

        binding.btnSend.setOnClickListener(v -> {
            if(!binding.messageInput.getText().toString().isEmpty()
            || !(binding.messageInput.getText() == null)){
                sendMessage();
                Log.d("Availability", "Available: " + isReceiverAvailable);
                getLastMessage();
            }
        });
    }

    private void showInfoFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        UserInfoFragment infoFragment = new UserInfoFragment(receiverUser);
        infoFragment.show(fragmentManager, "info_fragment");
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textUserName.setText(receiverUser.name);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    public void loading(Boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.INVISIBLE);
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private Bitmap getBitmapFromEncodedString (String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void listenMessages() {
        // Listen for messages sent by the current user to the receiver
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);

        // Listen for messages sent by the receiver to the current user
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private void listenAvailability() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
            }
            if (isReceiverAvailable) {
                binding.textAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.textAvailability.setVisibility(View.INVISIBLE);
            }
        });
    }

    // EventListener<QuerySnapshot> eventListener is a function that defines what happens when data changes.
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (object1, object2) -> object1.dateObject.compareTo(object2.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeChanged(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        loading(false);
        if (conversationId == null && chatMessages.size() > 0) {
            checkForConversation();
        }
    };

    private void sendMessage() {
        database = FirebaseFirestore.getInstance();
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, binding.messageInput.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());

        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);

        if (conversationId != null) {
            updateConversation(binding.messageInput.getText().toString(), new Date());
        } else {
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_LAST_MESSAGE, binding.messageInput.getText().toString());
            conversation.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversation.put(Constants.KEY_TIMESTAMP, new Date());
            conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversation.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversation.put(Constants.KEY_FCM_TOKEN, receiverUser.token);
            Log.d("FCM", "Token: " + receiverUser.token);
            conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            addConversation(conversation);
        }
        binding.messageInput.setText(null); // Clear the message input

    }

    private void updateConversationList(){

    }
    // The init method sets up essential components:
    // 1. preferenceManager for managing preferences,
    // 2. chatMessages to hold chat data,
    // 3. chatAdapter to display messages in the RecyclerView,
    // 4. and database to interact with Firestore.
    private void init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                getBitmapFromEncodedString(receiverUser.image),
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    // This part is for the recent messages feature.

    // This method checks remotely (in Firestore) if a conversation
    // between the specified sender and receiver already exists.
    private void checkForConversationRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversationOnCompleteListener);
    }

    // This listener processes the query result and checks if there’s an existing conversation between
    // two users. If found, it retrieves the document ID of the conversation and assigns it to conversationId.
    // For every documentSnapshot, has a unique ID.
    private final OnCompleteListener<QuerySnapshot> conversationOnCompleteListener = (task) -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();
        }
    };

    // The checkForConversation method:
    // 1. First, checks if there are any messages between the two users.
    // 2. Then, it performs two queries to check for an existing conversation in the
    // Firestore database, regardless of which user initiated it.
    private void checkForConversation() {
        if (chatMessages.size() != 0) {
            checkForConversationRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversationRemotely(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    // The updateConversation method:
    // 1. Targets a specific conversation in Firestore (using conversationId).
    // 2. Updates the conversation document with the latest message and the current timestamp, which is
    // helpful for displaying the most recent message and sorting conversations by their last activity.
    private void updateConversation(String message, Date date) {
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    // The addConversation method:
    // 1. Adds a new document to the Firestore conversations collection using data from the conversation HashMap.
    // 2. Retrieves and saves the new document’s unique ID to conversationId.
    // 3. Will use this method in the first conversation between two users.
    private void addConversation(HashMap<String, Object> conversation) {
        database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> {
                    Log.d("Firestore", "Conversation added with ID: " + documentReference.getId());
                    conversationId = documentReference.getId();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailability();
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken();
        Log.d("FCM", "Token: " + preferenceManager.getString(Constants.KEY_FCM_TOKEN));
    }

    private void getLastMessage() {
        // Only proceed if the receiver is unavailable and has a valid FCM token
        if (!isReceiverAvailable && receiverUser.token != null) {
            Log.d("Notification", "Receiver is unavailable and has a token. Fetching last message...");

            // Query Firestore to get the latest message from the conversation collection
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
                            Log.d("Token", "Token: " + receiverUser.token);
                            // Send notification with last message
                            sendNotification("New Message", senderName + ": " + lastMessage);

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
        // Double-check that receiver has a valid token before proceeding
        if (receiverUser.token != null) {
            Log.d("Notification", "Sending notification with title: " + title + " and message: " + messageBody);

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
                Log.d("Notification", "Notification channel created.");
            }

            // Use a unique ID to prevent overwriting notifications
            notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
            Log.d("Notification", "Notification sent.");
        } else {
            Log.d("Notification", "Receiver token is null. Notification not sent.");
        }
    }

}