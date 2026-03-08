# 🤖 Unified Test Automation Framework

A production-grade, thread-safe test automation framework supporting **Mobile UI (Android/iOS)**, **REST API**, and **GraphQL** testing — with built-in **Self-Healing Locators**, **Visual AI Regression**, **Fluent Wait**, **YAML-driven locators with OR conditions**, and **Extent + Analytics reporting**.

---

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Running Tests](#running-tests)
- [Configuration](#configuration)
  - [config.properties](#configproperties)
  - [devices.json](#devicesjson)
- [Mobile UI Testing](#mobile-ui-testing)
  - [Page Object Pattern](#page-object-pattern)
  - [YAML Locators with OR Conditions](#yaml-locators-with-or-conditions)
  - [Writing a UI Test](#writing-a-ui-test)
  - [Adding a New Page](#adding-a-new-page)
- [API Testing](#api-testing)
  - [REST API Client](#rest-api-client)
  - [API Page Objects (Enum-Based)](#api-page-objects-enum-based)
  - [Writing an API Test](#writing-an-api-test)
  - [Adding a New API Page](#adding-a-new-api-page)
  - [GraphQL Client](#graphql-client)
- [Self-Healing Engine](#self-healing-engine)
- [Visual AI Regression](#visual-ai-regression)
- [Retry Analyzer](#retry-analyzer)
- [Reporting](#reporting)
  - [Extent Reports](#extent-reports)
  - [AI Analytics Dashboard](#ai-analytics-dashboard)
- [Parallel Execution](#parallel-execution)
- [Appium Server Manager](#appium-server-manager)
- [Cloud Execution (Sauce Labs)](#cloud-execution-sauce-labs)
- [Thread Safety](#thread-safety)
- [Design Principles](#design-principles)
- [Key Interfaces](#key-interfaces)
- [Troubleshooting](#troubleshooting)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                          TEST LAYER                                 │
│  LoginTest, GetAccountDetailsAPITest, etc.                          │
│  (extend BaseTest for UI or BaseApiTest for API)                    │
├─────────────────────────────────────────────────────────────────────┤
│                        PAGE OBJECT LAYER                            │
│  UI Pages: LoginPage, HomePage (extend BasePage → AbstractBase)     │
│  API Pages: GetAccountDetailsApi, GetUserInfoApi (extend BaseApiPage)│
├─────────────────────────────────────────────────────────────────────┤
│                          CORE LAYER                                 │
│  DriverFactory │ DriverManager │ YamlLocatorRepository │ ConfigMgr  │
│  SelfHealingDriver │ VisualAIEngine │ RestApiClient │ GraphQLClient │
│  ExtentReporter │ SmartAnalyticsReporter │ ContextSwitcher          │
├─────────────────────────────────────────────────────────────────────┤
│                       INTERFACE LAYER                               │
│  IDriver │ IReporter │ ISelfHealer │ ILocatorRepository             │
├─────────────────────────────────────────────────────────────────────┤
│                     EXTERNAL LIBRARIES                              │
│  Appium │ Selenium │ TestNG │ REST Assured │ Guice │ ExtentReports  │
│  Jackson (YAML/JSON) │ Log4j2                                       │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Technology         | Version | Purpose                                  |
|--------------------|---------|------------------------------------------|
| Java               | 17+     | Programming language                     |
| Maven              | 3.8+    | Build & dependency management            |
| TestNG             | 7.8.0   | Test runner, parallel execution, suites  |
| Appium Java Client | 9.0.0   | Mobile automation (Android/iOS)          |
| Selenium           | 4.16.1  | WebDriver API                            |
| REST Assured       | 5.4.0   | REST API testing                         |
| Google Guice       | 7.0.0   | Dependency injection for page objects    |
| ExtentReports      | 5.1.1   | HTML test reports                        |
| Jackson            | 2.16.0  | YAML/JSON parsing (locators, devices)    |
| Log4j2             | 2.21.1  | Logging                                  |
| WebDriverManager   | 5.6.3   | ChromeDriver auto-download               |

---

## Project Structure

```
automation-framework/
├── pom.xml                                    # Maven dependencies & build config
├── README.md
├── src/
│   ├── main/
│   │   ├── java/com/framework/
│   │   │   ├── core/
│   │   │   │   ├── ai/
│   │   │   │   │   ├── healing/               # Self-healing engine
│   │   │   │   │   │   ├── SelfHealingDriver.java        # Orchestrates healing flow
│   │   │   │   │   │   ├── AlternativeLocatorStrategy.java # Generates fallback locators
│   │   │   │   │   │   └── HealingCache.java              # Thread-safe cache for healed locators
│   │   │   │   │   └── visual/
│   │   │   │   │       └── VisualAIEngine.java            # Pixel-based visual regression
│   │   │   │   ├── api/
│   │   │   │   │   ├── rest/
│   │   │   │   │   │   └── RestApiClient.java             # Fluent REST client (GET/POST/PUT/PATCH/DELETE)
│   │   │   │   │   └── graphql/
│   │   │   │   │       └── GraphQLClient.java             # Fluent GraphQL client
│   │   │   │   ├── config/
│   │   │   │   │   ├── AbstractBase.java                  # Initializes driver, locatorRepo, healer, fluentWait, ElementUtil
│   │   │   │   │   ├── ConfigManager.java                 # Loads config.properties (thread-safe)
│   │   │   │   │   ├── DeviceConfig.java                  # POJO for device JSON entries
│   │   │   │   │   ├── DeviceConfigManager.java           # Loads devices.json with round-robin support
│   │   │   │   │   ├── FrameworkModule.java               # Guice module — auto-discovers page objects
│   │   │   │   │   └── YamlLocatorRepository.java         # Parses YAML locators with OR conditions
│   │   │   │   ├── driver/
│   │   │   │   │   ├── AppiumDriverWrapper.java           # IDriver implementation wrapping AppiumDriver
│   │   │   │   │   ├── AppiumServerManager.java           # Auto-starts/stops Appium servers per thread (local only)
│   │   │   │   │   ├── DriverFactory.java                 # Creates Android/iOS drivers from DeviceConfig
│   │   │   │   │   └── DriverManager.java                 # ThreadLocal driver registry
│   │   │   │   ├── exceptions/                            # Custom exceptions (DriverInit, Healing, Locator, Visual)
│   │   │   │   ├── interfaces/
│   │   │   │   │   ├── IDriver.java                       # Driver abstraction
│   │   │   │   │   ├── ILocatorRepository.java            # Locator loading contract
│   │   │   │   │   ├── IReporter.java                     # Reporter contract
│   │   │   │   │   └── ISelfHealer.java                   # Self-healing contract
│   │   │   │   ├── listeners/
│   │   │   │   │   ├── TestListener.java                  # TestNG listener (logs test lifecycle)
│   │   │   │   │   ├── RetryAnalyzer.java                 # Auto-retries failed tests (configurable count)
│   │   │   │   │   └── RetryTransformer.java              # Applies RetryAnalyzer to all tests globally
│   │   │   │   ├── mobile/context/
│   │   │   │   │   └── ContextSwitcher.java               # Native ↔ WebView context switching
│   │   │   │   └── reports/
│   │   │   │       ├── extent/
│   │   │   │       │   └── ExtentReporter.java            # Thread-safe Extent HTML reporter
│   │   │   │       └── analytics/
│   │   │   │           └── SmartAnalyticsReporter.java    # AI analytics dashboard generator
│   │   │   ├── pages/
│   │   │   │   ├── ui/                                    # Mobile UI page objects
│   │   │   │   │   ├── BasePage.java                      # Abstract base (loc() for raw By; element interactions via inherited 'element' field)
│   │   │   │   │   ├── LoginPage.java                     # Login screen actions
│   │   │   │   │   └── HomePage.java                      # Home screen actions
│   │   │   │   └── api/                                   # API page objects (enum-based)
│   │   │   │       ├── BaseApiPage.java                   # Base (setResponse(), getString(), getInt(), assertions)
│   │   │   │       ├── GetAccountDetailsApi.java          # Fields enum for GET /posts
│   │   │   │       ├── GetUserInfoApi.java                # Fields enum for GET /users (nested JSON)
│   │   │   │       └── CreateAccountApi.java              # Fields enum for POST /posts
│   │   │   └── utils/
│   │   │       ├── ElementUtil.java                       # Context-aware element interaction utility (find, tap, type, scroll, wait)
│   │   │       ├── LoggerUtil.java                        # Log4j2 wrapper
│   │   │       └── ScreenshotUtil.java                    # Thread-safe screenshot capture (pass, fail, skip)
│   │   └── resources/
│   │       ├── config/
│   │       │   ├── config.properties                      # Framework settings (waits, platform, URLs)
│   │       │   └── devices.json                           # Device configs (local + remote/cloud)
│   │       ├── locators/
│   │       │   ├── LoginPage.yaml                         # Login page locators (with OR conditions)
│   │       │   └── HomePage.yaml                          # Home page locators (with OR conditions)
│   │       └── app/
│   │           ├── Android/                               # .apk files
│   │           └── iOS/                                   # .ipa files
│   └── test/
│       ├── java/com/framework/tests/
│       │   ├── ui/
│       │   │   ├── BaseTest.java                          # UI base (driver init, Guice injection, reporting)
│       │   │   └── LoginTest.java                         # Login UI tests
│       │   └── api/
│       │       ├── BaseApiTest.java                       # API base (pre-created page objects, reporting)
│       │       └── GetAccountDetailsAPITest.java          # API tests (GET, PUT, DELETE, nested validation)
│       └── resources/
│           ├── local-testng.xml                           # TestNG suite — local Appium execution
│           └── sauce-testng.xml                           # TestNG suite — Sauce Labs cloud execution
└── test-output/
    ├── extent-report/                                     # Extent HTML reports
    ├── analytics/                                         # AI Analytics dashboards
    └── screenshots/                                       # Failure screenshots
```

---

## Getting Started

### Prerequisites

| Requirement             | Details                                                                 |
|-------------------------|-------------------------------------------------------------------------|
| **Java JDK**            | 17 or higher                                                            |
| **Maven**               | 3.8+                                                                    |
| **Appium Server**       | 2.x installed (`npm install -g appium`) — auto-started by framework if `appium.auto.start=true` |
| **Android SDK**         | Set `ANDROID_HOME` / `ANDROID_SDK_ROOT` env variable                    |
| **Xcode** (macOS only)  | Required for iOS testing                                                |
| **Emulator / Device**   | Android emulator or real device connected via ADB                       |
| **IntelliJ IDEA**       | Recommended IDE (with TestNG plugin)                                    |

### Installation

```bash
# Clone the repository
git clone <repo-url>
cd automation-framework

# Install dependencies
mvn clean install -DskipTests

# Verify build
mvn compile
```

### Running Tests

#### From IntelliJ IDEA
1. Right-click on any `testng.xml` file → **Run**
2. Or right-click on any test class/method → **Run**

#### From Command Line

```bash
# Run local mobile UI tests (default — uses local-testng.xml)
mvn clean test

# Run local tests (explicit profile)
mvn clean test -Plocal

# Run Sauce Labs cloud tests
mvn clean test -Psauce

# Run with a custom testng.xml file
mvn clean test -DsuiteFile=src/test/resources/my-custom-testng.xml

# Override platform/environment via system properties
mvn clean test -Dplatform=ios -Denvironment=local

# Run a specific test class (skips testng.xml suite)
mvn clean test -Dtest=com.framework.tests.ui.LoginTest

# Run a specific test class with retries
mvn clean test -Dtest=com.framework.tests.ui.LoginTest -Dretry.max.count=3

# Run API tests only
mvn clean test -Dtest=com.framework.tests.api.GetAccountDetailsAPITest
```

**Maven Profiles (defined in `pom.xml`):**

| Profile   | Command              | Suite File              |
|-----------|----------------------|-------------------------|
| `local`   | `mvn test -Plocal`   | `local-testng.xml`      |
| `sauce`   | `mvn test -Psauce`   | `sauce-testng.xml`      |
| *(default)* | `mvn test`         | `local-testng.xml`      |
| *(custom)* | `mvn test -DsuiteFile=path/to/file.xml` | Any custom XML |

---

## Configuration

### config.properties

Located at: `src/main/resources/config/config.properties`

```properties
# Environment & platform
environment=local           # local | remote
platform=Android            # Android | iOS

# Appium
appium.server.url=http://127.0.0.1:4723

# Selenium Grid (optional — route through Grid hub or Appium Node)
grid.enabled=false
# grid.node.name=node1     # optional: route to a specific node

# Wait timeouts
implicit.wait=10
explicit.wait=20            # explicit wait timeout (seconds)
fluent.wait.timeout=10      # FluentWait max timeout (seconds)
fluent.wait.polling=500     # FluentWait polling interval (milliseconds)
fluent.wait.short.timeout=5 # Short FluentWait for quick checks
page.load.timeout=30        # page load timeout (seconds)

# Retry
retry.max.count=1           # 0 = disabled, 1 = retry once, 2 = retry twice, etc.

# AI model configuration
openai.api.key=YOUR_OPENAI_API_KEY   # OpenAI API key for AI features
ai.model=gpt-4                       # AI model (gpt-4, gpt-3.5-turbo, etc.)
visual.tolerance=0.02                 # Visual AI tolerance (2% = default)

# Web browser configuration
web.browser=chrome          # chrome | firefox | edge | safari
web.headless=false          # true = headless mode (no browser UI)
web.maximize=true           # true = maximize browser window on launch
web.mobile.device=          # mobile device emulation (leave empty for desktop)
web.grid.enabled=false      # true = run via Selenium Grid
web.grid.hub.url=http://selenium-grid:4444/wd/hub  # Grid hub URL

# WebDriverManager Auto-Download Settings
# web.driver.version=120.0.6099.109   # Force specific driver version (optional)
# web.driver.arch=64                  # Force architecture: 32, 64, arm64 (optional)
# web.driver.cache.ttl=30             # Driver cache time-to-live in days (default: 30)
# web.driver.proxy=http://proxy:8080  # Proxy for corporate networks (optional)

# Android ChromeDriver path (optional — leave empty for WebDriverManager auto-download)
# android.chromedriver.path=/path/to/chromedriver

# API and GraphQL configuration
api.base.url=https://api.example.com
graphql.endpoint=https://api.example.com/graphql
```

**Priority order:** System property → Environment variable → config file → default value.

```bash
# Override at runtime:
mvn test -Dplatform=ios -Dfluent.wait.timeout=15
```

### devices.json

Located at: `src/main/resources/config/devices.json`

Supports **local** and **remote** environments, each with **android** and **ios** device arrays. You can add any number of devices.

```json
{
  "local": {
    "android": [
      {
        "deviceName": "Pixel 9 API 35",
        "platformVersion": "15",
        "appPackage": "com.yourapp",
        "appActivity": "com.yourapp.MainActivity",
        "appPath": "src/main/resources/app/Android/app.apk",
        "noReset": false,
        "serverUrl": "http://127.0.0.1:4723"
      }
    ],
    "ios": [
      {
        "deviceName": "iPhone 16e",
        "platformVersion": "26.2",
        "bundleId": "com.yourapp",
        "appPath": "src/main/resources/app/iOS/app.ipa",
        "autoAcceptAlerts": true,
        "serverUrl": "http://127.0.0.1:4723"
      }
    ]
  },
  "remote": {
    "android": [
      {
        "deviceName": "Samsung Galaxy S23",
        "platformVersion": "16",
        "appPackage": "com.yourapp",
        "appActivity": "com.yourapp.MainActivity",
        "appPath": "storage:filename=app.apk",
        "serverUrl": "https://ondemand.us-west-1.saucelabs.com:443/wd/hub",
        "cloudOptions": {
          "sauce:options": {
            "username": "${SAUCE_USERNAME}",
            "accessKey": "${SAUCE_ACCESS_KEY}",
            "appiumVersion": "latest",
            "name": "Android Test",
            "build": "Build-1"
          }
        }
      }
    ]
  }
}
```

The `${SAUCE_USERNAME}` and `${SAUCE_ACCESS_KEY}` placeholders are resolved from environment variables at runtime.

---

## Mobile UI Testing

### Page Object Pattern

The framework uses a layered page object architecture:

```
AbstractBase (constructor initializes driver, locatorRepo, healer, fluentWait, ElementUtil)
    └── BasePage (loc() for raw By access)
        ├── LoginPage (typeUsername(), tapLogin(), loginWith(), etc.)
        └── HomePage (search(), tapCart(), logout(), etc.)
```

**Key:** All dependencies are initialized **once** in the `AbstractBase` constructor — no manual `init()` calls needed. When Guice creates a page object, the constructor runs and pulls the driver from `DriverManager` (ThreadLocal).

**`ElementUtil`** — a context-aware, non-static utility class created per page object. It holds the page context (driver, locatorRepo, healer, fluentWait, pageName, platform) and exposes all element interactions:

```java
// Inside page classes — use the inherited 'element' field
element.tap("loginButton");
element.enterText("usernameField", "admin");
element.getText("headerLabel");
element.isVisible("banner");
element.isPresent("errorMessage");
element.waitForClickable("submitButton");
element.waitForVisible("loader");
element.waitForInvisible(By.id("spinner"));
element.scrollDown();
element.scrollUp();
element.scrollToElement("footer");
element.findElement("searchBar");      // returns WebElement
element.loc("loginButton");            // returns raw By object
```

**Available `ElementUtil` methods:**

| Method                                    | Returns       | Description                                     |
|-------------------------------------------|---------------|-------------------------------------------------|
| `element.findElement("name")`             | `WebElement`  | Finds element using YAML OR strategies + healing |
| `element.tap("name")`                     | `void`        | Waits for clickable, then clicks                |
| `element.enterText("name", "text")`       | `void`        | Waits for visible, clears, then types           |
| `element.getText("name")`                 | `String`      | Waits for visible, returns text                 |
| `element.isVisible("name")`               | `boolean`     | Returns true if element is displayed            |
| `element.isPresent("name")`               | `boolean`     | Returns true if element exists in DOM           |
| `element.waitForVisible("name")`          | `WebElement`  | FluentWait until element is visible             |
| `element.waitForClickable("name")`        | `WebElement`  | FluentWait until element is clickable           |
| `element.waitForInvisible(By)`            | `void`        | FluentWait until element disappears             |
| `element.waitFor(condition, "desc")`      | `V`           | Custom FluentWait with any ExpectedCondition    |
| `element.scrollDown()`                    | `void`        | Mobile scroll down                              |
| `element.scrollUp()`                      | `void`        | Mobile scroll up                                |
| `element.scrollToElement("name")`         | `void`        | Scrolls until element is in view                |
| `element.loc("name")`                     | `By`          | Returns raw Selenium/Appium By locator          |

### YAML Locators with OR Conditions

Locators are stored in YAML files under `src/main/resources/locators/`. Each YAML file name must match the Page class name exactly (e.g., `LoginPage.java` → `LoginPage.yaml`).

**Format:**

```yaml
elementName:
  android: type::value | type::value | ...
  ios: type::value | type::value | ...
  common: type::value                          # fallback for any platform
```

**Supported locator types:**

| Category         | Types                                                              |
|------------------|--------------------------------------------------------------------|
| **Selenium `By`**  | `id`, `xpath`, `css`, `classname`, `name`, `tagname`              |
| **`AppiumBy`** (mobile) | `accessibilityId`, `androidUIAutomator`, `iOSClassChain`, `iOSNsPredicate` |
| **Legacy alias** | `accessibility` (maps to `AppiumBy.accessibilityId`)              |

The framework automatically uses `AppiumBy` when a mobile-specific type is detected in YAML, and `By` for standard web types — no configuration needed.

**OR logic (`|`):** The framework tries locators left-to-right. If the first fails, it tries the next, and so on.

**Example (`LoginPage.yaml`):**

```yaml
loginButton:
  android: id::com.yourapp:id/login_btn | xpath://android.widget.Button[@text='Login']
  ios: xpath://XCUIElementTypeButton[@name='Login'] | accessibilityId::login_button

findStoreButton:
  android: id::com.yourapp:id/findStoreButton | xpath:://*[@text='Find Store']
  ios: accessibilityId::find_store_button

appLogo:
  common: xpath:://*[@content-desc='app_logo'] | accessibilityId::app_logo

# iOS-specific AppiumBy examples:
searchResults:
  ios: iOSClassChain::**/XCUIElementTypeCell[`name BEGINSWITH "result"`]
  android: androidUIAutomator::new UiSelector().resourceId("search_results")
```

### Writing a UI Test

```java
public class LoginTest extends BaseTest {

    @Test(description = "Login page loads successfully")
    public void loginPageLoads() {
        step("Verify login page is loaded");
        loginPage.element.tap(LoginPage.OK_BUTTON);
        Assert.assertTrue(loginPage.element.isVisible(LoginPage.FIND_STORE_BUTTON), "Login page is not visible");
    }
}
```

**What happens behind the scenes:**

1. `BaseTest.@BeforeMethod` → creates driver from `devices.json`, stores in `DriverManager` (ThreadLocal)
2. Guice injects `loginPage` → `AbstractBase` constructor pulls driver, initializes `fluentWait`, `locatorRepo`, `healer`, `ElementUtil`
3. `loginPage.element.tap("okayButton")` → `ElementUtil.findElement()` → tries all YAML OR strategies with FluentWait
4. If all YAML strategies fail → Self-Healing kicks in, generating alternative locators
5. `BaseTest.@AfterMethod` → logs result to Extent Report, captures screenshot (pass, fail, or skip), quits driver

### Adding a New Page

1. **Create the page class** in `src/main/java/com/framework/pages/ui/`:

```java
public class ProductPage extends BasePage {
    private static final String PRODUCT_TITLE = "productTitle";
    private static final String ADD_TO_CART   = "addToCartButton";

    public String getProductTitle() { return element.getText(PRODUCT_TITLE); }
    public void addToCart()         { element.tap(ADD_TO_CART); }
}
```

2. **Create the YAML locator file** `src/main/resources/locators/ProductPage.yaml`:

```yaml
productTitle:
  android: id::com.yourapp:id/product_title | xpath:://*[@resource-id='com.yourapp:id/product_title']
  ios: accessibilityId::product_title

addToCartButton:
  android: id::com.yourapp:id/add_to_cart | xpath://android.widget.Button[@text='Add to Cart']
  ios: xpath://XCUIElementTypeButton[@name='Add to Cart'] | accessibilityId::add_to_cart
```

3. **Use it in your test** — just declare with `@Inject`:

```java
public class ProductTest extends BaseTest {
    @Inject protected ProductPage productPage;

    @Test
    public void verifyProductTitle() {
        Assert.assertEquals(productPage.getProductTitle(), "Expected Title");
    }
}
```

**That's it.** No Guice module changes needed — `FrameworkModule` uses Java reflection to auto-discover all classes extending `BasePage`.

---

## API Testing

### REST API Client

`RestApiClient` provides both **static one-liner** calls and a **fluent builder** pattern. All static helpers internally route through a single `execute()` method for consistent header/body/logging handling.

**Static helpers (quick calls):**

```java
Response response = RestApiClient.get(BASE_URL, "/posts/1");
Response response = RestApiClient.post(BASE_URL, "/posts", requestBody);
Response response = RestApiClient.put(BASE_URL, "/posts/1", requestBody);
Response response = RestApiClient.delete(BASE_URL, "/posts/1");

// With headers
Response response = RestApiClient.get(BASE_URL, "/posts", Map.of("Authorization", "Bearer token"));

// With headers + query params
Response response = RestApiClient.get(BASE_URL, "/posts", headers, Map.of("userId", 1, "_limit", 5));
```

**Fluent builder (full control):**

```java
Response response = new RestApiClient()
    .baseUri(BASE_URL)
    .endpoint("/posts")
    .header("Authorization", "Bearer token123")
    .header("X-Custom", "value")
    .body(requestBody)
    .execute("POST");
```

### API Page Objects (Enum-Based)

API responses are validated through **API Page Objects** — each one defines a `Fields` enum mapping field names to JSON paths. The enum's `toString()` returns the JSON path, so `BaseApiPage` methods accept `Enum<?>` directly — **no interface needed**.

**Example API Page:**

```java
public class GetAccountDetailsApi extends BaseApiPage {

    public enum Fields {
        ID      ("id"),
        USER_ID ("userId"),
        TITLE   ("title"),
        BODY    ("body");

        private final String jsonPath;
        Fields(String jsonPath) { this.jsonPath = jsonPath; }
        @Override
        public String toString() { return jsonPath; }
    }
}
```

**For nested JSON paths:**

```java
public class GetUserInfoApi extends BaseApiPage {

    public enum Fields {
        NAME            ("name"),
        EMAIL           ("email"),
        ADDRESS_CITY    ("address.city"),        // nested path
        GEO_LAT         ("address.geo.lat"),     // deeply nested
        COMPANY_NAME    ("company.name");

        private final String jsonPath;
        Fields(String jsonPath) { this.jsonPath = jsonPath; }
        @Override
        public String toString() { return jsonPath; }
    }
}
```

### Writing an API Test

API page objects are **pre-created** in `BaseApiTest`. Tests call `setResponse()` to map a response, then extract values using typed getters:

```java
public class GetAccountDetailsAPITest extends BaseApiTest {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    @Test(description = "GET post with field validation")
    public void testGetRequest() {
        step("Send GET request to /posts/1");
        Response response = RestApiClient.get(BASE_URL, "/posts/1");
        getAccountDetailsApi.setResponse(response);

        // Extract values using typed getters
        int id       = getAccountDetailsApi.getInt(GetAccountDetailsApi.Fields.ID);
        int userId   = getAccountDetailsApi.getInt(GetAccountDetailsApi.Fields.USER_ID);
        String title = getAccountDetailsApi.getString(GetAccountDetailsApi.Fields.TITLE);

        // Standard TestNG assertions
        Assert.assertEquals(getAccountDetailsApi.getStatusCode(), 200, "Status code mismatch");
        Assert.assertEquals(id, 1, "Post ID mismatch");
        Assert.assertEquals(userId, 1, "User ID mismatch");
        Assert.assertNotNull(title, "Title should not be null");
    }
}
```

**Available getters in `BaseApiPage`:**

| Method                          | Returns    | Example                                        |
|---------------------------------|------------|------------------------------------------------|
| `getString(Enum<?> jsonPath)`   | `String`   | `api.getString(Fields.TITLE)`                  |
| `getInt(Enum<?> jsonPath)`      | `int`      | `api.getInt(Fields.ID)`                        |
| `getLong(Enum<?> jsonPath)`     | `long`     | `api.getLong(Fields.TIMESTAMP)`                |
| `getBoolean(Enum<?> jsonPath)`  | `boolean`  | `api.getBoolean(Fields.ACTIVE)`                |
| `getList(Enum<?> jsonPath)`     | `List<T>`  | `api.getList(Fields.TAGS)`                     |
| `get(Enum<?> jsonPath)`         | `T` (auto) | `api.get(Fields.ANYTHING)`                     |
| `getStatusCode()`              | `int`      | `api.getStatusCode()`                          |
| `getResponse()`                | `Response` | `api.getResponse().getTime()`                  |

### Adding a New API Page

1. **Create the API page** in `src/main/java/com/framework/pages/api/`:

```java
public class GetOrderApi extends BaseApiPage {

    public enum Fields {
        ORDER_ID      ("orderId"),
        STATUS        ("status"),
        TOTAL_AMOUNT  ("payment.totalAmount"),
        SHIPPING_CITY ("shipping.address.city");

        private final String jsonPath;
        Fields(String jsonPath) { this.jsonPath = jsonPath; }
        @Override
        public String toString() { return jsonPath; }
    }
}
```

2. **Register it in `BaseApiTest`:**

```java
public abstract class BaseApiTest {
    // ...existing page objects...
    protected GetOrderApi getOrderApi = new GetOrderApi();
}
```

3. **Use it in your test:**

```java
@Test
public void testGetOrder() {
    Response response = RestApiClient.get(BASE_URL, "/orders/123");
    getOrderApi.setResponse(response);

    String status = getOrderApi.getString(GetOrderApi.Fields.STATUS);
    Assert.assertEquals(status, "SHIPPED", "Order status mismatch");
}
```

### GraphQL Client

```java
GraphQLClient client = new GraphQLClient("https://api.example.com/graphql");

GraphQLClient.GraphQLResponse response = client
    .query("query GetUser($id: ID!) { user(id: $id) { name email } }")
    .variable("id", "123")
    .bearerAuth("token")
    .execute();

String name  = response.getData("user.name");
String email = response.getData("user.email");
```

---

## Self-Healing Engine

When all YAML OR strategies fail to find an element, the **Self-Healing Engine** automatically generates alternative locators and tries them.

### Toggle (Enable / Disable)

Self-healing can be toggled via `config.properties` or overridden at runtime via Maven:

```properties
# config.properties
self.healing.enabled=true    # true = enabled (default), false = disabled
```

```bash
# Override via Maven command
mvn test -Dself.healing.enabled=false    # disable healing for this run
mvn test -Dself.healing.enabled=true     # enable healing for this run
```

| `self.healing.enabled` | Behavior |
|------------------------|----------|
| `true` (default)       | All YAML strategies tried → if all fail → self-healing kicks in → generates alternatives → caches working locator |
| `false`                | All YAML strategies tried → if all fail → throws `NoSuchElementException` immediately (no healing attempt) |

**Healing flow:**

```
1. ElementUtil.findElement("loginButton")
    ├── Try YAML OR strategy #1 (id::com.app:id/login_btn)        → ❌ Not Found
    ├── Try YAML OR strategy #2 (xpath://...[@text='Login'])       → ❌ Not Found
    ├── Try YAML OR strategy #3 (css::[content-desc='login_btn']) → ❌ Not Found
    └── All YAML strategies exhausted → invoke SelfHealingDriver

2. SelfHealingDriver.heal()
    ├── Check HealingCache for previously healed locator           → cache miss
    ├── AlternativeLocatorStrategy.generate() for EACH failed locator
    │   ├── Parse locator → extract meaningful values ("login_btn", "Login")
    │   ├── Generate: By.id("login_btn")
    │   ├── Generate: //*[@resource-id='login_btn']
    │   ├── Generate: //*[@content-desc='Login']
    │   ├── Generate: //*[@text='Login']
    │   ├── Generate: //*[contains(@resource-id,'login_btn')]
    │   ├── Generate: //*[contains(@text,'login')]  (case-insensitive)
    │   └── ... (20+ alternatives per locator)
    └── Try each alternative → first match cached + returned ✅

3. Future calls → HealingCache hit → instant ⚡
```

**Key classes:**

| Class                          | Responsibility                                      |
|--------------------------------|-----------------------------------------------------|
| `SelfHealingDriver`           | Orchestrates the healing flow                        |
| `AlternativeLocatorStrategy`  | Parses locators, generates intelligent alternatives  |
| `HealingCache`                | Thread-safe cache (ConcurrentHashMap) for healed locators |

---

## Visual AI Regression

Pixel-by-pixel screenshot comparison with configurable tolerance.

```java
// In any test:
VisualAIEngine.VisualResult result = visualCheck("login_screen");
Assert.assertTrue(result.passed(), "Visual regression detected: " + result.summary());
```

**How it works:**

1. First run → captures screenshot and saves as **baseline** in `src/test/resources/visual/baseline/`
2. Subsequent runs → captures **actual** screenshot, compares pixel-by-pixel with baseline
3. Generates a **diff image** highlighting differences in red
4. Returns `VisualResult` with similarity percentage, diff pixel count, and pass/fail

**Configuration:**

```properties
visual.tolerance=0.02   # 2% tolerance (default)
```

---

## Retry Analyzer

The framework includes a built-in **Retry Analyzer** that automatically re-runs failed tests. The retry count is configurable via `config.properties` — no code changes needed.

**Configuration (`config.properties`):**

```properties
retry.max.count=1    # 0 = disabled, 1 = retry once, 2 = retry twice, etc.
```

**Override at runtime:**

```bash
mvn test -Dretry.max.count=3
```

**How it works:**

1. `RetryTransformer` (registered as a TestNG listener in `testng.xml`) automatically applies `RetryAnalyzer` to **every** test method at runtime — no need to add `@Test(retryAnalyzer = ...)` on each test
2. When a test fails, `RetryAnalyzer.retry()` reads `retry.max.count` from config
3. If retries remain → logs `🔄 RETRY [1/2]: testName [thread: ...]` and re-runs the test
4. If all retries are exhausted → test is marked as **failed**

**Key classes:**

| Class              | Responsibility                                                         |
|--------------------|------------------------------------------------------------------------|
| `RetryAnalyzer`    | Implements `IRetryAnalyzer` — reads retry count from config, tracks retry state per test |
| `RetryTransformer` | Implements `IAnnotationTransformer` — auto-applies `RetryAnalyzer` to all `@Test` methods globally |

**TestNG listener registration (`testng.xml`):**

```xml
<listeners>
    <listener class-name="com.framework.core.listeners.TestListener"/>
    <listener class-name="com.framework.core.listeners.RetryTransformer"/>
</listeners>
```

> **Note:** If a specific test already has a custom `retryAnalyzer` set via `@Test(retryAnalyzer = CustomRetry.class)`, the `RetryTransformer` will **not** override it.

---

## Reporting

### Extent Reports

Generated at: `test-output/extent-report/Mobile_Report_<timestamp>.html`

- Dark-themed HTML report
- Thread-safe (ThreadLocal `ExtentTest` nodes)
- Includes: test steps, pass/fail/skip status, failure screenshots, healing events
- System info: platform, environment, AI features

### AI Analytics Dashboard

Generated at: `test-output/analytics/analytics_<timestamp>.html`

- Pass/fail/skip metrics with visual charts
- Per-test timing analysis (detects slow tests)
- Flaky test detection (tests that sometimes pass, sometimes fail)
- Self-healing statistics (how many locators were healed)
- AI-generated insights and recommendations

### Screenshots

Screenshots are captured automatically for **every test outcome** — pass, fail, and skip:

| Outcome  | Filename Pattern                         | Attached To Report |
|----------|------------------------------------------|--------------------|
| **FAIL** | `testName_FAILURE_threadName_HHmmss.png` | ✅ Embedded as failure screenshot |
| **PASS** | `testName_PASS_threadName_HHmmss.png`    | ✅ Logged as info |
| **SKIP** | `testName_SKIP_threadName_HHmmss.png`    | ✅ Logged as info |

All screenshots are saved to `test-output/screenshots/`. Thread name and timestamp are embedded in filenames to prevent collisions during parallel execution.

---

## Parallel Execution

The framework is designed for parallel execution from the ground up:

**TestNG suite (`local-testng.xml`):**

```xml
<suite name="AI Mobile Automation Suite"
       parallel="tests"
       thread-count="2">

    <!-- Device 1 -->
    <test name="Android Device 1 - Login Tests">
        <parameter name="environment" value="local"/>
        <parameter name="platform" value="android"/>
        <parameter name="deviceIndex" value="0"/>
        <classes>
            <class name="com.framework.tests.ui.LoginTest"/>
        </classes>
    </test>

    <!-- Device 2 -->
    <test name="Android Device 2 - Login Tests">
        <parameter name="environment" value="local"/>
        <parameter name="platform" value="android"/>
        <parameter name="deviceIndex" value="1"/>
        <classes>
            <class name="com.framework.tests.ui.LoginTest"/>
        </classes>
    </test>
</suite>
```

**Round-robin device assignment:** If `deviceIndex` is `-1`, `DeviceConfigManager.getNextDevice()` assigns devices in round-robin order using `AtomicInteger`.

---

## Appium Server Manager

The framework includes a built-in **Appium Server Manager** that automatically starts and stops a local Appium server — **no manual `appium` command needed**. A single Appium server handles all parallel threads; each thread creates its own isolated driver session on the shared server.

### Architecture

```
┌──────────────────────────────────────────────────────┐
│         ONE Appium Server (port 4723)                 │
│                                                        │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐         │
│  │ Session 1  │  │ Session 2  │  │ Session 3  │         │
│  │ (Thread-1) │  │ (Thread-2) │  │ (Thread-3) │         │
│  │ Device A   │  │ Device B   │  │ Device C   │         │
│  └───────────┘  └───────────┘  └───────────┘         │
└──────────────────────────────────────────────────────┘
```

- **One server process** — started once in `@BeforeSuite`, shared by all threads
- **Multiple driver sessions** — each thread creates its own `AppiumDriver` session (isolated via `ThreadLocal` in `DriverManager`)
- **Appium 2.x** natively supports concurrent sessions on a single server
- **Resource efficient** — only one Node.js process, not one per thread

### Lifecycle

```
@BeforeSuite
    └── AppiumServerManager.start()          ← starts ONE server on port 4723

@BeforeMethod (Thread-1)
    ├── deviceConfig.setServerUrl(:4723)     ← all threads point to same server
    └── DriverFactory.createDriver()         ← creates driver SESSION 1

@BeforeMethod (Thread-2)  ← parallel
    ├── deviceConfig.setServerUrl(:4723)     ← same server
    └── DriverFactory.createDriver()         ← creates driver SESSION 2

@AfterMethod (each thread)
    └── DriverManager.quitDriver()           ← quits only THIS thread's session

@AfterSuite
    └── AppiumServerManager.stop()           ← stops the ONE server
```

### Configuration

In `config.properties`:

```properties
appium.auto.start=true          # true = auto-start, false = manual start (default: false)
appium.base.port=4723           # Port for the Appium server
appium.startup.timeout=120      # Max seconds to wait for server to start
# appium.path=                  # Custom appium JS path (empty = auto-detect from PATH)
```

### Behavior by Environment

| Environment | `appium.auto.start=true` | `appium.auto.start=false` |
|-------------|--------------------------|---------------------------|
| **local**   | ✅ Auto-starts one server | ❌ User must start Appium manually |
| **remote**  | ❌ Skipped (connects to cloud URL) | ❌ Skipped (connects to cloud URL) |

### Cross-Platform Support (macOS + Windows)

The manager automatically configures the Appium process environment:

| OS | What it does |
|----|-------------|
| **macOS / Linux** | Adds `/opt/homebrew/bin`, `/usr/local/bin`, `~/.nvm`, `~/.volta`, `~/.fnm` to PATH; sets `ANDROID_HOME` |
| **Windows** | Adds `%APPDATA%\npm`, `%LOCALAPPDATA%\Programs\Volta\bin`, `%ProgramFiles%\nodejs` to PATH; sets `ANDROID_HOME` |

### Custom Appium Path

If Appium is not on your PATH (e.g., installed in a custom location):

```properties
# macOS (Homebrew)
appium.path=/opt/homebrew/lib/node_modules/appium/build/lib/main.js

# Windows
appium.path=C:\\Users\\user\\AppData\\Roaming\\npm\\node_modules\\appium\\build\\lib\\main.js
```

Leave `appium.path` empty (default) to auto-detect from PATH.

### Thread Safety

| Concern | How it's handled |
|---------|-----------------|
| Server start | `synchronized` block with double-checked locking — only one thread starts the server |
| Server reference | `volatile` field — all threads see the same running instance |
| Driver isolation | Each thread creates its own `AppiumDriver` session → stored in `ThreadLocal` via `DriverManager` |
| Session cleanup | `@AfterMethod` quits only the current thread's driver session; server stays running |
| Port probe | `ServerSocket` check before starting — automatically finds a free port |

---

## Cloud Execution (Sauce Labs)

1. **Upload your app** to Sauce Labs Storage:

```bash
curl -u $SAUCE_USERNAME:$SAUCE_ACCESS_KEY \
  -X POST "https://api.us-west-1.saucelabs.com/v1/storage/upload" \
  -F "payload=@/path/to/app.apk" \
  -F "name=app.apk"
```

2. **Set environment variables:**

```bash
export SAUCE_USERNAME=your_username
export SAUCE_ACCESS_KEY=your_access_key
```

3. **Configure `devices.json`** → `remote` section with `cloudOptions`

4. **Run:**

```bash
mvn clean test -Psauce
```

The framework automatically:
- Resolves `${SAUCE_USERNAME}` / `${SAUCE_ACCESS_KEY}` from environment variables
- Builds the authenticated Sauce Labs URL
- Applies `sauce:options` capabilities

---

## Thread Safety

Every component is designed for safe parallel execution:

| Component                | Thread Safety Mechanism                                         |
|--------------------------|-----------------------------------------------------------------|
| `DriverManager`          | `ThreadLocal<IDriver>` — each thread has its own driver         |
| `AppiumServerManager`    | `synchronized` + `volatile` — one server, double-checked locking on start |
| `ElementUtil`            | Per-instance, per-page object — holds only its own page's context |
| `ExtentReporter`         | `ThreadLocal<ExtentTest>` — each thread has its own test node   |
| `SelfHealingDriver`      | `ThreadLocal<List<String>>` for healing logs                    |
| `HealingCache`           | `ConcurrentHashMap` for locator cache                           |
| `SmartAnalyticsReporter` | `AtomicInteger` counters + `CopyOnWriteArrayList` for history   |
| `DeviceConfigManager`    | `ConcurrentHashMap` cache + `AtomicInteger` round-robin counter |
| `YamlLocatorRepository`  | `ConcurrentHashMap` for parsed YAML cache                       |
| `ConfigManager`          | `Properties` loaded once at class initialization (immutable)    |
| `ScreenshotUtil`         | Stateless — thread name + timestamp in filename prevents collision |
| Page objects             | Per-instance, per-method (Guice creates new instances each `@BeforeMethod`) |

---

## Design Principles

The framework follows **SOLID** principles:

| Principle                  | Implementation                                                     |
|----------------------------|--------------------------------------------------------------------|
| **Single Responsibility**  | Each class has one job (DriverFactory creates drivers, HealingCache caches, etc.) |
| **Open/Closed**            | New page objects, locator strategies, or reporters can be added without modifying existing code |
| **Liskov Substitution**    | `AppiumDriverWrapper` can be used anywhere `IDriver` is expected   |
| **Interface Segregation**  | `IDriver`, `IReporter`, `ISelfHealer`, `ILocatorRepository` — focused contracts |
| **Dependency Inversion**   | Tests depend on `IDriver`/`IReporter` abstractions, not concrete classes |

**Additional patterns:**
- **Page Object Model** — UI interactions encapsulated in page classes
- **Factory Pattern** — `DriverFactory` creates drivers from config
- **Strategy Pattern** — OR locator strategies, alternative locator generation
- **Dependency Injection** — Google Guice auto-discovers and injects page objects
- **Builder Pattern** — `RestApiClient` fluent API

---

## Key Interfaces

```java
// IDriver — abstracts all driver operations
public interface IDriver {
    WebElement findElement(By locator);
    List<WebElement> findElements(By locator);
    void quit();
    String getPlatform();
    boolean isAndroid();
    boolean isIOS();
    Object getUnderlyingDriver();
}

// IReporter — contract for all reporters
public interface IReporter {
    void initReport();
    void startTest(String testName, String description);
    void logStep(String message);
    void logPass(String message);
    void logFail(Throwable t, String screenshotPath);
    void logSkip(String message);
    void logInfo(String message);
    void attachScreenshot(String path);
    void flush();
}

// ISelfHealer — contract for self-healing strategies
public interface ISelfHealer {
    WebElement heal(By failedLocator, String elementName, IDriver driver);
    WebElement heal(List<By> failedLocators, String elementName, IDriver driver);
    List<String> getHealingLog();
    void clearLog();
}

// ILocatorRepository — contract for locator loading
public interface ILocatorRepository {
    By getLocator(String pageName, String elementName, String platform);
    List<LocatorStrategy> getAllStrategies(String pageName, String elementName, String platform);
    void reload(String pageName);
}
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `ANDROID_HOME not set` | Export `ANDROID_HOME` pointing to your Android SDK: `export ANDROID_HOME=~/Library/Android/sdk` |
| `SessionNotCreatedException` | Ensure Appium server is running: `appium` |
| `No devices configured` | Check `devices.json` matches the environment/platform you're using |
| `LocatorNotFoundException` | Verify YAML file name matches page class name (e.g., `LoginPage.java` → `LoginPage.yaml`) |
| `Unsupported class file major version 68` | Upgrade Guice to 7.0.0+ (compatible with JDK 24) or use JDK 17 |
| `ExceptionInInitializerError TypeTag UNKNOWN` | In IntelliJ: Settings → Build → Compiler → disable "Enable annotation processing" then re-enable |
| Sauce Labs `Appium version not available` | Use `"appiumVersion": "latest"` in `cloudOptions` |
| `NullPointerException` on `locatorRepo` | Ensure driver is created BEFORE page objects (BaseTest handles this automatically) |
| Tests interfering in parallel | Verify all shared state uses ThreadLocal/AtomicInteger/ConcurrentHashMap |
| Extent report missing steps | Use `step("description")` in test methods — it logs to both console and report |

---

## License

This project is for internal use. All rights reserved.
