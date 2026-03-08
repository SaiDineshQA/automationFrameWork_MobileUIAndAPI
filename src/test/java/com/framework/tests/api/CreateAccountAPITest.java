package com.framework.tests.api;

import com.framework.core.api.rest.RestApiClient;
import com.framework.pages.api.CreateAccountApi;
import com.framework.tests.ui.BaseTest;
import com.framework.utils.JsonSchemaUtil;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Map;

public class CreateAccountAPITest extends BaseApiTest {
    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    private Response response = null;

    @Test(description = "REST API - POST create with field validation")
    public void testPostRequest() {
        step("Send POST request to /posts");
        response = RestApiClient.post(BASE_URL, "/posts", JsonSchemaUtil.read("CreatePost.json"));
        createAccountApi.setResponse(response);

        step("Validate status code");
        Assert.assertEquals(createAccountApi.getStatusCode(), 201, "Status code mismatch");

        step("Validate title");
        String title = createAccountApi.getString(CreateAccountApi.Fields.TITLE);
        Assert.assertEquals(title, "Test Post", "Title mismatch");

        step("Validate id was generated");
        int id = createAccountApi.getInt(CreateAccountApi.Fields.ID);
        Assert.assertTrue(id > 0, "ID should be generated");
    }
}
