package com.framework.pages.api;

/**
 * GetUserInfoApi - API page for GET /users/{id}.
 *
 * Usage:
 *   getUserApi.setResponse(response);
 *   String name = getUserApi.getString(Fields.NAME);
 */
public class GetUserInfoApi extends BaseApiPage {

    public enum Fields {
        ID              ("id"),
        NAME            ("name"),
        USERNAME        ("username"),
        EMAIL           ("email"),
        PHONE           ("phone"),
        WEBSITE         ("website"),
        ADDRESS_STREET  ("address.street"),
        ADDRESS_SUITE   ("address.suite"),
        ADDRESS_CITY    ("address.city"),
        ADDRESS_ZIPCODE ("address.zipcode"),
        GEO_LAT         ("address.geo.lat"),
        GEO_LNG         ("address.geo.lng"),
        COMPANY_NAME    ("company.name"),
        COMPANY_PHRASE  ("company.catchPhrase"),
        COMPANY_BS      ("company.bs");

        private final String jsonPath;

        Fields(String jsonPath) {
            this.jsonPath = jsonPath;
        }

        @Override
        public String toString() { return jsonPath; }
    }
}
