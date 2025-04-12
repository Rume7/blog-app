package com.codehacks.blog.util;

public final class Constants {
    private Constants() {
    }

    public static final String API_V1 = "/api/v1";
    public static final String BLOG_PATH = API_V1 + "/blog";
    public static final String AUTH_PATH = API_V1 + "/auth";
    public static final String COMMENT_PATH = API_V1 + "/comment";
    public static final String USERS_PATH = API_V1 + "/users";
    public static final int MIN_TITLE_LENGTH = 8;
    public static final int MAX_TITLE_LENGTH = 150;
    public static final int MAX_CONTENT_LENGTH = 100000;

    public static final String POST_NOT_FOUND = "Post not found with id: ";
}
