package com.infusion.relnotesgen;

public class Commit {
    private final String message;
    private final String id;

    public Commit(final String message, final String id) {
        this.message = message;
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
