package com.webscraper.auth;

public class User {
    public int    id;
    public String name;
    public String email;
    public transient String passwordHash;  // never serialized to JSON

    public User() {}

    public User(int id, String name, String email) {
        this.id    = id;
        this.name  = name;
        this.email = email;
    }
}
