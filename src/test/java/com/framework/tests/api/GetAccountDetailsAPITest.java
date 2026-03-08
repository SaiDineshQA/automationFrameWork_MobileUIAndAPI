package com.framework.tests.api;

import com.framework.core.api.rest.RestApiClient;
import com.framework.pages.api.CreateAccountApi;
import com.framework.pages.api.GetAccountDetailsApi;
import com.framework.pages.api.GetUserInfoApi;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * GetAccountDetailsAPITest - API automation with enum-backed API pages.
 * <p>
 * Pattern:
 * 1. RestApiClient.get(baseUrl, path)           → fires the request
 * 2. getAccountDetailsApi.setResponse(response)            → maps response to page object
 * 3. int id = getAccountDetailsApi.getInt(Fields.ID)       → extract value
 * 4. Assert.assertEquals(id, 1, "ID mismatch")   → standard assertion
 */
public class GetAccountDetailsAPITest extends BaseApiTest {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    private static final String RSRC_PATH = "/posts/1";
    private Response response = null;

    @Test(description = "REST API - GET post with field validation")
    public void testGetRequest() {
        step("Send GET request to /posts/1");
        response = RestApiClient.get(BASE_URL, RSRC_PATH);
        getAccountDetailsApi.setResponse(response);

        int userId = getAccountDetailsApi.getInt("USER_ID");
        String title = getAccountDetailsApi.getString("TITLE");
        int id = getAccountDetailsApi.getInt("ID");

        Assert.assertEquals(getAccountDetailsApi.getStatusCode(), 200, "Status code mismatch");
        Assert.assertNotNull(title, "Title should not be null");
        Assert.assertEquals(id, 1, "Post ID mismatch");
        Assert.assertEquals(userId, 1, "User ID mismatch");
    }



    @Test(description = "REST API - PUT update with field validation")
    public void testPutRequest() {
        step("Send PUT request to /posts/1");
        Response response = RestApiClient.put(BASE_URL, "/posts/1", Map.of(
                "id", 1,
                "title", "Updated Title",
                "body", "Updated body content",
                "userId", 1
        ));
        getAccountDetailsApi.setResponse(response);

        step("Validate status code");
        Assert.assertEquals(getAccountDetailsApi.getStatusCode(), 200, "Status code mismatch");

        step("Validate updated title");
        String title = getAccountDetailsApi.getString(GetAccountDetailsApi.Fields.TITLE);
        Assert.assertEquals(title, "Updated Title", "Title was not updated");
    }

    @Test(description = "REST API - DELETE request")
    public void testDeleteRequest() {
        step("Send DELETE request to /posts/1");
        Response response = RestApiClient.delete(BASE_URL, "/posts/1");
        getAccountDetailsApi.setResponse(response);

        step("Validate status code");
        Assert.assertEquals(getAccountDetailsApi.getStatusCode(), 200, "Delete failed");
    }

    @Test(description = "REST API - GET user with nested JSON path validation")
    public void testGetUserWithNestedPaths() {
        step("Send GET request to /users/1");
        Response response = RestApiClient.get(BASE_URL, "/users/1");
        getUserInfoApi.setResponse(response);

        step("Validate status code");
        Assert.assertEquals(getUserInfoApi.getStatusCode(), 200, "Status code mismatch");

        step("Validate name");
        String name = getUserInfoApi.getString(GetUserInfoApi.Fields.NAME);
        Assert.assertEquals(name, "Leanne Graham", "Name mismatch");

        step("Validate email");
        String email = getUserInfoApi.getString(GetUserInfoApi.Fields.EMAIL);
        Assert.assertEquals(email, "Sincere@april.biz", "Email mismatch");

        step("Validate nested address.city");
        String city = getUserInfoApi.getString(GetUserInfoApi.Fields.ADDRESS_CITY);
        Assert.assertEquals(city, "Gwenborough", "City mismatch");

        step("Validate phone is present");
        String phone = getUserInfoApi.getString(GetUserInfoApi.Fields.PHONE);
        Assert.assertNotNull(phone, "Phone should not be null");

        step("Validate company name");
        String company = getUserInfoApi.getString(GetUserInfoApi.Fields.COMPANY_NAME);
        Assert.assertNotNull(company, "Company name should not be null");
    }

    @Test(description = "REST API - Response time validation")
    public void testResponseTime() {
        step("Send GET request to /posts/1");
        Response response = RestApiClient.get(BASE_URL, "/posts/1");
        getAccountDetailsApi.setResponse(response);

        step("Validate response time is under 2 seconds");
        Assert.assertTrue(getAccountDetailsApi.getResponse().getTime() < 2000,
                "Response time exceeded 2000ms");

        step("Validate title is present");
        String title = getAccountDetailsApi.getString(GetAccountDetailsApi.Fields.TITLE);
        Assert.assertNotNull(title, "Title should not be null");
    }

    @Test(description = "REST API - Email contains validation")
    public void testContainsValidation() {
        step("Send GET request to /users/1");
        Response response = RestApiClient.get(BASE_URL, "/users/1");
        getUserInfoApi.setResponse(response);

        step("Validate email contains domain");
        String email = getUserInfoApi.getString(GetUserInfoApi.Fields.EMAIL);
        Assert.assertTrue(email.contains("@april.biz"), "Email domain mismatch");
    }
}
