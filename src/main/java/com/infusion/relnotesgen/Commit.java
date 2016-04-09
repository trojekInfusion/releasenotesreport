package com.infusion.relnotesgen;

public class Commit {

    private final String message;
    private final String id;
    private String author;

    public Commit(final String message, final String id, String author) {
        this.message = message;
        this.id = id;
        this.author = author;
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

    public String getAuthor() { return author; }
}
