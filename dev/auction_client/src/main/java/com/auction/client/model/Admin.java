package com.auction.client.model;

import com.auction.model.User;

public class Admin extends User {

    public Admin(int id, String username, String email, String passwordHash) {
        super(id, username, email, passwordHash);
    }

}