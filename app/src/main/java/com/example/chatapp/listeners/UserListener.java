package com.example.chatapp.listeners;

import com.example.chatapp.models.User;

// if the user click a user in the userList, this interface will be implemented
public interface UserListener {
    void onUserClicked(User user);
}
