# 🚀 Complete Automation Capabilities Guide

This framework supports **5 automation types** in one unified platform:

---

## 1️⃣ Mobile Native App Automation

**Use:** Android/iOS native apps (SDK widgets)

**Example:**
```java
@Test
public void testNativeApp() {
    LoginPage page = GuiceInjector.getInstance(LoginPage.class);
    page.loginWith("user@test.com", "pass123");
}
```

**Capabilities:**
- Android UiAutomator2
- iOS XCUITest
- Self-healing locators
- YAML locator repository with OR conditions
- FluentWait on every action

---

## 2️⃣ Mobile Browser Automation

**Use:** Chrome/Safari browser on mobile devices

**Example:**
```java
@Test
public void testMobileBrowser() {
    // Creates Android Chrome or iOS Safari driver
    AppiumDriver driver = MobileBrowserDriver.createAndroidBrowser();
    driver.get("https://example.com");
    
    // Use standard Selenium locators
    driver.findElement(By.cssSelector("h1")).click();
}
```

**Capabilities:**
- Android Chrome automation
- iOS Safari automation
- Responsive web testing on real devices
- Mobile-specific gestures

---

## 3️⃣ Hybrid App Automation (Native + WebView)

**Use:** Apps with both native UI and web content

**Example:**
```java
@Test
public void testHybridApp() {
    AppiumDriver driver = (AppiumDriver) DriverManager.getDriver().getUnderlyingDriver();
    
    // Login with native UI
    LoginPage page = GuiceInjector.getInstance(LoginPage.class);
    page.loginWith("user@test.com", "pass123");
    
    // Switch to WebView for web dashboard
    ContextSwitcher.switchToWebView(driver);
    driver.findElement(By.cssSelector(".dashboard-widget")).click();
    
    // Switch back to native
    ContextSwitcher.switchToNative(driver);
    driver.findElement(By.id("logout_btn")).click();
}
```

**Capabilities:**
- Automatic context detection
- Switch to NATIVE_APP or WEBVIEW
- Multiple WebView support (by index or name)
- Auto-restore context after actions
- Wait for WebView to appear dynamically

---

## 4️⃣ Web UI Automation

**Use:** Desktop/responsive web applications

**Example:**
```java
@Test
public void testWebApp() {
    WebDriver driver = WebDriverFactory.createDriver("chrome");
    driver.get("https://example.com");
    
    WebElement heading = driver.findElement(By.tagName("h1"));
    Assert.assertEquals(heading.getText(), "Example Domain");
}
```

**Capabilities:**
- Chrome, Firefox, Edge, Safari
- Headless mode for CI/CD
- Selenium Grid support
- Mobile device emulation
- BrowserStack/Sauce Labs integration

---

## 5️⃣ REST API Automation

**Use:** Backend API testing

**Example:**
```java
@Test
public void testRestApi() {
    // Fluent API
    Response response = new RestApiClient()
        .baseUri("https://api.example.com")
        .endpoint("/users/1")
        .bearerAuth("jwt-token")
        .queryParam("include", "profile")
        .get();
    
    response.then()
        .statusCode(200)
        .body("name", equalTo("John Doe"));
    
    // Or static helpers
    Response quickGet = RestApiClient.get("/users/1");
}
```

**Capabilities:**
- Fluent builder pattern
- GET, POST, PUT, PATCH, DELETE
- Bearer/Basic auth
- Query params, path params, headers
- JSON/XML/form-data
- Response validation with JsonPath

---

## 6️⃣ GraphQL API Automation

**Use:** GraphQL endpoint testing

**Example:**
```java
@Test
public void testGraphQL() {
    String query = """
        query GetUser($id: ID!) {
          user(id: $id) {
            name
            email
            profile {
              bio
            }
          }
        }
        """;
    
    GraphQLResponse response = new GraphQLClient()
        .endpoint("https://api.example.com/graphql")
        .query(query)
        .variable("id", "12345")
        .bearerAuth("jwt-token")
        .execute();
    
    response
        .assertNoErrors()
        .assertStatusCode(200);
    
    String userName = response.getData("user.name").asText();
    Assert.assertEquals(userName, "John Doe");
}
```

**Capabilities:**
- Query and mutation support
- Variables support
- Fragment support
- Error detection
- Nested data extraction
- Response assertions

---

## 🔄 Context Switching API

### Available Methods

```java
// Switch to native app context
ContextSwitcher.switchToNative(driver);

// Switch to first WebView
ContextSwitcher.switchToWebView(driver);

// Switch to specific WebView by index
ContextSwitcher.switchToWebView(driver, 0);  // First WebView

// Switch to WebView by name pattern
ContextSwitcher.switchToWebView(driver, "chrome");

// Wait for WebView to appear
ContextSwitcher.waitAndSwitchToWebView(driver, Duration.ofSeconds(10));

// Get current context
String context = ContextSwitcher.getCurrentContext(driver);

// Get all available contexts
Set<String> all = ContextSwitcher.getAllContexts(driver);

// Check context type
boolean isNative = ContextSwitcher.isNativeContext(driver);
boolean isWeb = ContextSwitcher.isWebViewContext(driver);

// Execute in WebView, auto-restore context
String result = ContextSwitcher.executeInWebView(driver, d -> {
    return d.findElement(By.cssSelector("h1")).getText();
});
```

---

## 📋 Configuration

### config.properties

```properties
# ─── Mobile Native ─────────────────────────
platform=android
android.app.package=com.yourapp
android.app.activity=.MainActivity

# ─── Web Automation ────────────────────────
web.browser=chrome
web.headless=false
web.maximize=true
web.grid.enabled=false
web.grid.hub.url=http://grid-hub:4444

# ─── Mobile Browser ────────────────────────
# android.chromedriver.path=/path/to/chromedriver
# Leave empty for auto-download

# ─── API Automation ────────────────────────
api.base.url=https://api.example.com
graphql.endpoint=https://api.example.com/graphql
```

---

## 🎯 Use Case Matrix

| Scenario | Automation Type | Context |
|----------|----------------|---------|
| Native app login | Mobile Native | NATIVE_APP |
| Web dashboard in app | Hybrid (WebView) | WEBVIEW |
| OAuth/payment in app | Hybrid (WebView) | WEBVIEW |
| Mobile browser testing | Mobile Browser | Browser |
| Desktop web app | Web UI | Browser |
| Backend API validation | REST API | N/A |
| GraphQL service testing | GraphQL API | N/A |
| End-to-end: Login (native) → Dashboard (web) → Logout (native) | Hybrid | NATIVE_APP ↔ WEBVIEW |

---

## 💡 Best Practices

### Mobile Native
- Use YAML locators with OR conditions
- FluentWait handles timing automatically
- Self-healing catches locator changes

### Hybrid Apps
- Always verify context before interactions
- Use `executeInWebView()` for auto-restore
- Log context switches for debugging

### Mobile Browser
- Use standard Selenium locators (CSS, XPath)
- Test responsive design on real devices
- Verify mobile-specific gestures

### Web UI
- Use Page Object Model
- Leverage explicit waits
- Run headless in CI/CD

### REST API
- Validate status codes and response times
- Use JsonPath for nested assertions
- Store tokens in config or env vars

### GraphQL
- Always check `hasErrors()` before data
- Use variables for dynamic queries
- Validate schema compliance

---

## 🧪 Example: Complete E2E Flow

```java
@Test
public void testCompleteUserJourney() {
    // 1. REST API: Create user account
    Map<String, Object> newUser = Map.of(
        "email", "test@example.com",
        "password", "SecurePass123!"
    );
    Response apiResponse = RestApiClient.post("/users", newUser);
    String userId = apiResponse.jsonPath().getString("id");
    
    // 2. Mobile Native: Login
    LoginPage loginPage = GuiceInjector.getInstance(LoginPage.class);
    loginPage.loginWith("test@example.com", "SecurePass123!");
    
    // 3. Hybrid: Switch to WebView dashboard
    AppiumDriver driver = (AppiumDriver) DriverManager.getDriver().getUnderlyingDriver();
    ContextSwitcher.switchToWebView(driver);
    driver.findElement(By.cssSelector(".profile-settings")).click();
    
    // 4. GraphQL: Verify user profile
    String query = "query { user(id: \"" + userId + "\") { email verified } }";
    GraphQLResponse gqlResponse = GraphQLClient.query(GRAPHQL_ENDPOINT, query);
    Assert.assertTrue(gqlResponse.getData("user.verified").asBoolean());
    
    // 5. Hybrid: Switch back to native, logout
    ContextSwitcher.switchToNative(driver);
    HomePage homePage = GuiceInjector.getInstance(HomePage.class);
    homePage.logout();
}
```

---

**Your framework now handles EVERYTHING — Native Mobile · Web · Hybrid · Mobile Browser · REST API · GraphQL** 🚀
