package com.framework.tests.ui;

import com.framework.pages.ui.LoginPage;
import org.testng.Assert;
import org.testng.annotations.Test;


public class LoginTest extends BaseTest {


    // ═══ Tests ═══

    @Test(priority = 1, description = "Login page loads and passes visual check")
    public void loginPageLoads() {
        step("Verify login page is loaded");
        loginPage.tap(LoginPage.OK_BUTTON);
        Assert.assertTrue(loginPage.isVisible(LoginPage.FIND_STORE_BUTTON), "Login page is not visible");
    }



    // ═══ Tests ═══

    @Test(priority = 1, description = "Login new loads and passes visual check")
    public void loginPageLoads2() {
        step("Verify login page is loaded");
        loginPage.tap(LoginPage.OK_BUTTON);
        Assert.assertTrue(loginPage.isVisible(LoginPage.FIND_STORE_BUTTON), "Login page is not visible");
    }
}
