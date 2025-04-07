package com.example.chatapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chatapp.databinding.ActivitySignInBinding;
import com.example.chatapp.databinding.ActivitySignUpBinding;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class SignUpActivity extends AppCompatActivity {


    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    private User currentUser;
    private PlacesClient placesClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());

        //placesClient = Places.createClient(this);

        setupSpinner();
        setListeners();
    }

    private void setListeners() {
        binding.textSignIn.setOnClickListener(v ->
            startActivity(new Intent(this, SignInActivity.class)));
        binding.btnSignUp.setOnClickListener(v -> {
            if (isValidSignUpDetails()) {
                signUp();
            }
        });
        binding.textUserBirthDate.setOnClickListener(v -> showDatePickerDialog());
        //binding.textPlace.setOnClickListener(v -> openPlacePicker());
        binding.layoutImage.setOnClickListener(v -> {
            // Intent.ACTION_PICK: This is the intent action used to open a screen
            // where the user can select a piece of data (in this case, an image).
            // MediaStore.Images.Media.EXTERNAL_CONTENT_URI: This URI tells the system to open the
            // external storage (such as the device's gallery) and allow the user to pick an image from there.
            // FLAG_GRANT_READ_URI_PERMISSION: This flag grants temporary read
            // access to the URI (the image file that the user selects).
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
    private void signUp() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        // HashMap<> - It's useful for storing data where you may have a variety of
        // value types but want to easily access them using string keys.
        String conversationTemp = "";
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_NAME, binding.textUserName.getText().toString());
        user.put(Constants.KEY_PASSWORD, binding.textPassword.getText().toString());
        user.put(Constants.KEY_IMAGE, encodedImage);
        user.put(Constants.KEY_COLLECTION_CONVERSATIONS, conversationTemp);
        user.put(Constants.KEY_GENDER, binding.spinnerGender.getSelectedItem().toString());
        user.put(Constants.KEY_BIRTH_DATE, binding.textUserBirthDate.getText().toString());
        user.put(Constants.KEY_FULL_NAME, binding.textFirstName.getText().toString() + " " +
                binding.textLastName.getText().toString());
        //user.put(Constants.KEY_USER_ID, database.collection(Constants.KEY_USER).document().getId());
        // it will add user details to the database under the KEY_COLLECTION_USERS "user"
        database.collection(Constants.KEY_COLLECTION_USERS)
                .add(user)

                .addOnSuccessListener(documentReference -> {
                    loading(false);
                    preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                    preferenceManager.putString(Constants.KEY_USER_ID, documentReference.getId());
                    preferenceManager.putString(Constants.KEY_NAME, binding.textUserName.getText().toString());
                    preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                    Log.d("Firestore", "User added with ID:" + preferenceManager.getString(Constants.KEY_USER_ID));
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                })
                .addOnFailureListener(exception -> {
                    loading(false);
                    showToast(exception.getMessage());
                });
    }
    private String encodeImage(Bitmap bitmap) {
        // Scale to preserve aspect ratio
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);

        // ByteArrayOutputStream is used to hold the compressed image data in memory.
        // The compress() method compresses the scaled bitmap into JPEG format.
        // Converts the contents of the ByteArrayOutputStream into a byte array,
        // which represents the compressed image.
        // Base64 is commonly used to represent binary data (like images) in a text format,
        // which is useful when transmitting or storing images in text-based formats (e.g., JSON, XML).
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    // ActivityResultLauncher<Intent> is used in Android to handle results from activities
    // Register the ActivityResultLauncher: This step ensures your app is ready to handle
    // the result when an activity (like selecting an image) finishes.
    // The user picks an image from the gallery using an Intent.
    // The ActivityResultLauncher gets the result and the URI of the image.
    // An InputStream is created to read the image data from the URI.
    // BitmapFactory.decodeStream() converts the image data into a Bitmap.
    // The Bitmap is then set to an ImageView to display the selected image.
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imgProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );
    private Boolean isValidSignUpDetails() {

        if (encodedImage == null) {
            showToast("Select profile image");
            return false;
        } else if (binding.textFirstName.getText().toString().trim().isEmpty()) {
            showToast("Enter first name");
            return false;
        } else if (binding.textLastName.getText().toString().trim().isEmpty()) {
            showToast("Enter last name");
            return false;
        } else if (binding.textUserBirthDate.getText().toString().trim().isEmpty()) {
            showToast("Enter birth date");
            return false;
        } else if (binding.textUserName.getText().toString().trim().isEmpty()){
            showToast("Enter name");
            return false;
        } else if (binding.textUserName.getTextSize() < 4) {
            binding.textUserName.setHint("Username must contain atleast 4 characters.");
            return false;
        } else if (binding.textPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else if (binding.textPassword.getTextSize() < 8) {
            binding.textPassword.setHint("Password must contain atleast 8 characters");
            return false;
        } else if (binding.textConfirmPassword.getText().toString().trim().isEmpty()) {
            showToast("Confirm your password");
            return false;
        } else if (!binding.textPassword.getText().toString().equals(binding.textConfirmPassword.getText().toString())) {
            showToast("Password & confirm password must be same");
            return false;
        } else {
            return true;
        }
    }

    private void loading (Boolean isLoading) {
        if(isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnSignUp.setVisibility(View.INVISIBLE);
        } else {
            binding.btnSignUp.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showDatePickerDialog() {
        // Get the current date to set as default
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create and show the DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                SignUpActivity.this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Format the selected date
                    String birthdate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                    binding.textUserBirthDate.setText(birthdate);
                },
                year, month, day
        );

        // Optionally restrict the selectable date range to prevent future dates
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());

        datePickerDialog.show();
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.spinner_options, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner using binding
        binding.spinnerGender.setAdapter(adapter);

        binding.spinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get selected item text from spinner
                String selectedItem = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle no selection here if needed
            }
        });
    }
/*
    private void openPlacePicker() {
        // Specify the fields to return after the user selects a place
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS);

        // Launch the autocomplete activity
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                // Set the selected place address in the EditText
                binding.textPlace.setText(place.getAddress());
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.e("PlacePicker", "Error: " + status.getStatusMessage());
            }
        }
    }

 */
}