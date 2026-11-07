package com.hms.model;

/** Host profile of a user. Owns one property (standalone host) or many (chain) - same model for both. */
public class Owner {

    private final String id;
    private final String userId;
    private final String name;

    public Owner(String id, String userId, String name) {
        this.id = id;
        this.userId = userId;
        this.name = name;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getName() { return name; }
}
