package com.framework.pages.api;

/**
 * GetAccountDetailsApi - API page for GET /posts/{id}.
 *
 * Usage:
 *   getPostApi.setResponse(response);
 *   int id = getPostApi.getInt(Fields.ID);
 */
public class GetAccountDetailsApi extends BaseApiPage {

    public enum Fields {
        ID      ("id"),
        USER_ID ("userId"),
        TITLE   ("title"),
        BODY    ("body");

        private final String jsonPath;

        Fields(String jsonPath) {
            this.jsonPath = jsonPath;
        }

        @Override
        public String toString() { return jsonPath; }
    }
}
