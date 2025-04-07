package com.example.chatapp;

import android.app.Application;

import com.google.android.libraries.places.api.Places;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCEfYr1nETdAJLTDdw7D-mbwo5aIG1H9Bw");
        }
    }
}
