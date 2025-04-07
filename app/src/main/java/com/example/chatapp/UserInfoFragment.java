package com.example.chatapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

public class UserInfoFragment extends DialogFragment {

    private User mUser;

    public UserInfoFragment(User user){
        mUser = user;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_user_info, container, false);

        TextView fullName = view.findViewById(R.id.textFullName);
        TextView birthDate = view.findViewById(R.id.textBirthDate);
        TextView gender = view.findViewById(R.id.textGender);
        ImageView infoImage = view.findViewById(R.id.textUserImage);
        fullName.setText("Full Name: " + mUser.full_name);
        birthDate.setText("Birth Date: " + mUser.birthDate);
        gender.setText("Gender: " + mUser.gender);
        infoImage.setImageBitmap(getUserImage(mUser.image));

        return view;
    }
    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
