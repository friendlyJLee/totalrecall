package com.loopwiki.loginregisterwithsqlite;

/**
 * Created by amardeep on 10/26/2017.
 */

public class User {
    public String id;
    public String userName;
    public String email;
    public String password;

    public User(String id, String userName, String email, String password) {
        this.id = id;
        this.userName = userName;
        this.email = email;
        this.password = password;
    }

}
