package com.aws.rpdk;

/**
 * NOTE: This is an example class which needs to be removed as we integrate the RPDK
 */
public class ResourceModel {

    private String typeName;
    private String title;
    private String author;

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getAuthor() {
        return this.author;
    }

    public void setAuthor(final String author) {
        this.author = author;
    }

    public String getTypeName() {
        return this.typeName;
    }

    public void setTypeName(final String typeName) {
        this.typeName = typeName;
    }

    public ResourceModel() { }
}
