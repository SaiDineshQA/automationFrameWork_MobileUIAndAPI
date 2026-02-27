package com.framework.pages.api;

/**
 * CreateAccountApi - API page for POST /posts.
 *
 * Usage:
 *   createPostApi.setResponse(response);
 *   String title = createPostApi.getString(Fields.TITLE);
 */
public class CreateAccountApi extends BaseApiPage {

    public enum Fields {
        ID      ("id"),
        TITLE   ("title"),
        BODY    ("body"),
        USER_ID ("userId");

        private final String jsonPath;

        Fields(String jsonPath) {
            this.jsonPath = jsonPath;
        }

        @Override
        public String toString() { return jsonPath; }
    }
}
