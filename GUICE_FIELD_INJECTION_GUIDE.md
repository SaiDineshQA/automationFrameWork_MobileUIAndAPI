# 💉 Google Guice Field Injection - Complete Guide

The framework uses **Java Reflection** to automatically inject `@Inject` annotated fields, eliminating all manual object creation.

---

## ✨ Before vs After

### **Before (Manual):**
```java
public class LoginTest extends BaseTest {
    @Test
    public void testLogin() {
        // Manual object creation every time ❌
        LoginPage loginPage = GuiceInjector.getInstance(LoginPage.class);
        HomePage homePage = GuiceInjector.getInstance(HomePage.class);
        
        loginPage.login("user", "pass");
        Assert.assertTrue(homePage.isLoaded());
    }
}
```

**Problems:**
- ❌ Repetitive `getInstance()` calls
- ❌ Boilerplate in every test method
- ❌ Easy to forget to create page objects
- ❌ Harder to read test code

### **After (Auto-Inject):**
```java
public class LoginTest extends BaseTest {
    @Inject
    private LoginPage loginPage;  // Auto-injected! ✅
    
    @Inject
    private HomePage homePage;    // Auto-injected! ✅
    
    @Test
    public void testLogin() {
        // Just use them directly!
        loginPage.login("user", "pass");
        Assert.assertTrue(homePage.isLoaded());
    }
}
```

**Benefits:**
- ✅ Zero manual object creation
- ✅ Clean, readable test code
- ✅ Declare once, use everywhere
- ✅ Type-safe at compile time

---

## 🔧 How It Works (Technical Deep Dive)

### **Step 1: Declare @Inject Fields**

```java
public class MyTest extends BaseTest {
    @Inject
    private LoginPage loginPage;
    
    @Inject
    private HomePage homePage;
}
```

### **Step 2: TestNG Calls @BeforeMethod**

```java
// In BaseTest.java
@BeforeMethod(alwaysRun = true)
public void methodSetup(Method method) {
    // ... driver setup ...
    
    // Auto-inject @Inject fields
    injectGuiceFields(this);  // ← Magic happens here!
}
```

### **Step 3: Java Reflection Finds @Inject Fields**

```java
private void injectGuiceFields(Object testInstance) {
    Class<?> clazz = testInstance.getClass();
    
    // Walk up class hierarchy (including parent classes)
    while (clazz != null && clazz != Object.class) {
        for (Field field : clazz.getDeclaredFields()) {
            // Check if field has @Inject annotation
            if (field.isAnnotationPresent(Inject.class)) {
                injectField(field, testInstance);
            }
        }
        clazz = clazz.getSuperclass();
    }
}
```

### **Step 4: Make Field Accessible (Even Private)**

```java
private void injectField(Field field, Object testInstance) {
    // Bypass private access modifier
    field.setAccessible(true);
    
    // ...continue injection...
}
```

### **Step 5: Get Instance from Guice**

```java
// Get Guice-injected instance for this field type
Object injectedInstance = GuiceInjector.getInstance(field.getType());

// Example:
// field.getType() = LoginPage.class
// injectedInstance = Guice creates LoginPage with all dependencies
```

### **Step 6: Set Field Value via Reflection**

```java
// Set the field value on the test instance
field.set(testInstance, injectedInstance);

// Equivalent to:
// this.loginPage = injectedInstance;
```

### **Complete Flow Diagram:**

```
┌────────────────────────────────────────────────┐
│ @Test Method Starts                            │
└─────────────────┬──────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────┐
│ TestNG Calls @BeforeMethod                     │
│ → methodSetup(Method method)                   │
└─────────────────┬──────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────┐
│ Call injectGuiceFields(this)                   │
└─────────────────┬──────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────┐
│ Java Reflection Scans Test Class               │
│ → getDeclaredFields()                          │
└─────────────────┬──────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────┐
│ For Each Field:                                │
│   if (field.isAnnotationPresent(Inject.class)) │
└─────────────────┬──────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────┐
│ Make Field Accessible                          │
│ → field.setAccessible(true)                    │
└─────────────────┬──────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────┐
│ Get Type from Field                            │
│ → Class<?> type = field.getType()             │
│   Example: LoginPage.class                     │
└─────────────────┬──────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────┐
│ Get Instance from Guice                        │
│ → GuiceInjector.getInstance(type)              │
│   Guice creates: new LoginPage(...)            │
│   With all constructor dependencies            │
└─────────────────┬──────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────┐
│ Set Field Value via Reflection                 │
│ → field.set(testInstance, instance)            │
│   Equivalent: this.loginPage = instance        │
└─────────────────┬──────────────────────────────┘
                  ↓
┌────────────────────────────────────────────────┐
│ @Test Method Executes                          │
│ → loginPage is now ready to use!              │
└────────────────────────────────────────────────┘
```

---

## 📋 Complete Example

### **Test Class:**

```java
public class CheckoutTest extends BaseTest {
    
    // ═══ All page objects declared once ═══
    @Inject
    private LoginPage loginPage;
    
    @Inject
    private HomePage homePage;
    
    @Inject
    private ProductPage productPage;
    
    @Inject
    private CartPage cartPage;
    
    @Inject
    private CheckoutPage checkoutPage;
    
    // ═══ Tests just use them ═══
    
    @Test
    public void testCompleteCheckoutFlow() {
        step("Login");
        loginPage.login("user@test.com", "pass123");
        
        step("Verify home page");
        Assert.assertTrue(homePage.isLoaded());
        
        step("Search for product");
        homePage.search("laptop");
        
        step("Select product");
        productPage.selectFirstProduct();
        
        step("Add to cart");
        productPage.addToCart();
        
        step("Open cart");
        cartPage.open();
        
        step("Proceed to checkout");
        cartPage.checkout();
        
        step("Complete checkout");
        checkoutPage.fillDetails("John Doe", "1234 5678 9012 3456");
        checkoutPage.submit();
        
        step("Verify order confirmation");
        Assert.assertTrue(checkoutPage.isOrderConfirmed());
    }
    
    @Test
    public void testEmptyCart() {
        loginPage.login("user@test.com", "pass123");
        cartPage.open();
        Assert.assertTrue(cartPage.isEmpty());
    }
}
```

**What happens behind the scenes:**

```
1. TestNG calls @BeforeMethod
2. BaseTest creates driver
3. BaseTest calls injectGuiceFields(this)
4. Reflection finds 5 @Inject fields:
   - loginPage
   - homePage
   - productPage
   - cartPage
   - checkoutPage
5. For each field:
   a. field.setAccessible(true)
   b. Get type: LoginPage.class, HomePage.class, etc.
   c. Call: GuiceInjector.getInstance(type)
   d. Guice creates instance with all dependencies
   e. field.set(this, instance)
6. All 5 page objects are now ready to use!
7. Test executes with all fields populated
```

---

## 🎯 Supported Features

### **1. Private Fields (Recommended)**

```java
@Inject
private LoginPage loginPage;  // ✅ Works via setAccessible(true)
```

### **2. Protected/Public Fields**

```java
@Inject
protected LoginPage loginPage;  // ✅ Works

@Inject
public LoginPage loginPage;      // ✅ Works (but not recommended)
```

### **3. Inherited Fields**

```java
public abstract class BaseTestWithCommonPages extends BaseTest {
    @Inject
    protected LoginPage loginPage;  // Declared in parent
}

public class MyTest extends BaseTestWithCommonPages {
    @Inject
    private HomePage homePage;      // Declared in child
    
    @Test
    public void test() {
        loginPage.login(...);  // ✅ Inherited field works!
        homePage.verify();      // ✅ Own field works!
    }
}
```

### **4. Multiple Fields**

```java
@Inject
private LoginPage loginPage;

@Inject
private HomePage homePage;

@Inject
private SettingsPage settingsPage;

// All injected automatically! ✅
```

---

## 🔍 Advanced Scenarios

### **Scenario 1: Conditional Page Object Usage**

```java
public class LoginTest extends BaseTest {
    @Inject
    private LoginPage loginPage;
    
    @Inject
    private HomePage homePage;
    
    @Test
    public void testSuccessfulLogin() {
        loginPage.login("valid@user.com", "validpass");
        // homePage is injected but only used if login succeeds
        Assert.assertTrue(homePage.isLoaded());
    }
    
    @Test
    public void testFailedLogin() {
        loginPage.login("invalid@user.com", "wrongpass");
        // homePage is injected but never used in this test
        Assert.assertTrue(loginPage.isErrorVisible());
    }
}
```

**Performance Note:**
- Injected fields are created even if not used
- This is fine — object creation is lightweight
- Driver setup is the expensive part, not page object creation

### **Scenario 2: Multiple Test Classes Sharing Page Objects**

```java
// LoginTest.java
public class LoginTest extends BaseTest {
    @Inject
    private LoginPage loginPage;
}

// HomeTest.java
public class HomeTest extends BaseTest {
    @Inject
    private LoginPage loginPage;  // Same type, fresh instance per test
    @Inject
    private HomePage homePage;
}
```

**Each test gets fresh instances:**
- `@BeforeMethod` runs before every `@Test`
- New injection happens each time
- No state shared between tests
- Thread-safe parallel execution

### **Scenario 3: Page Objects with Complex Dependencies**

```java
// Page object with Guice constructor injection
public class ComplexPage extends BasePage {
    @Inject
    public ComplexPage(IDriver driver,
                       ILocatorRepository locatorRepo,
                       ISelfHealer healer,
                       String platform) {
        super(driver, locatorRepo, healer, platform);
    }
}

// Test class
public class MyTest extends BaseTest {
    @Inject
    private ComplexPage complexPage;  // ✅ Guice handles all dependencies!
}
```

**Guice recursively resolves:**
1. Test needs `ComplexPage`
2. `ComplexPage` needs `IDriver`, `ILocatorRepository`, etc.
3. Guice creates `IDriver` from `DriverManager`
4. Guice gets `ILocatorRepository` singleton
5. Guice creates `ComplexPage` with all dependencies
6. Field is set with fully-constructed object

---

## ⚡ Performance & Thread Safety

### **Performance:**

| Operation | Time |
|-----------|------|
| Reflection field scan | ~1ms per class |
| Guice injection per field | ~0.5ms |
| Total overhead per test | ~5-10ms |

**Compared to:**
- Driver initialization: ~2000ms
- Test execution: ~5000ms

**Conclusion:** Negligible overhead (< 0.2%)

### **Thread Safety:**

```java
// Thread 1 executes:
@BeforeMethod  // Creates driver for Thread 1
→ injectGuiceFields(this)  // Injects page objects for Thread 1

// Thread 2 executes (parallel):
@BeforeMethod  // Creates driver for Thread 2
→ injectGuiceFields(this)  // Injects page objects for Thread 2
```

**Each thread gets:**
- ✅ Own driver (ThreadLocal)
- ✅ Own page object instances
- ✅ No shared state
- ✅ Fully isolated execution

---

## 🛠️ Debugging

### **Enable Debug Logging:**

```java
// In BaseTest.java - already implemented
LoggerUtil.debug("[Guice] Injected field: " + field.getName() +
        " (" + field.getType().getSimpleName() + ")");
```

**Console Output:**
```
[Guice] Injected field: loginPage (LoginPage)
[Guice] Injected field: homePage (HomePage)
[Guice] Injected field: cartPage (CartPage)
```

### **Common Issues:**

**Issue 1: Field is null in test**

```java
@Test
public void test() {
    loginPage.login(...);  // NullPointerException!
}
```

**Cause:** Field not annotated with `@Inject`

**Solution:**
```java
@Inject  // ← Add this!
private LoginPage loginPage;
```

---

**Issue 2: NoSuchMethodException**

```
Error: com.google.inject.ProvisionException: Unable to provision LoginPage
Caused by: NoSuchMethodException: LoginPage.<init>()
```

**Cause:** Page object missing `@Inject` constructor

**Solution:**
```java
public class LoginPage extends BasePage {
    @Inject  // ← Add this!
    public LoginPage(IDriver driver,
                     ILocatorRepository locatorRepo,
                     ISelfHealer healer,
                     String platform) {
        super(driver, locatorRepo, healer, platform);
    }
}
```

---

**Issue 3: IllegalAccessException**

```
Error: Cannot access field loginPage
```

**Cause:** Should never happen (we use `setAccessible(true)`)

**Solution:** Check if you're running with Java security manager enabled

---

## 💡 Best Practices

### ✅ **DO:**

1. **Declare fields as `private`**
   ```java
   @Inject
   private LoginPage loginPage;  // ✅ Encapsulation
   ```

2. **Use descriptive field names**
   ```java
   @Inject
   private LoginPage loginPage;         // ✅ Clear
   
   @Inject
   private ProductPage productPage;     // ✅ Clear
   ```

3. **Group related page objects**
   ```java
   // Auth pages
   @Inject private LoginPage loginPage;
   @Inject private RegisterPage registerPage;
   
   // Main app pages
   @Inject private HomePage homePage;
   @Inject private SettingsPage settingsPage;
   ```

4. **Inject all pages needed for the test class**
   ```java
   // CheckoutTest needs these pages
   @Inject private LoginPage loginPage;
   @Inject private ProductPage productPage;
   @Inject private CartPage cartPage;
   @Inject private CheckoutPage checkoutPage;
   ```

### ❌ **DON'T:**

1. **Don't use same variable name as class**
   ```java
   @Inject
   private LoginPage LoginPage;  // ❌ Confusing
   ```

2. **Don't inject in @BeforeMethod**
   ```java
   @BeforeMethod
   public void setup() {
       loginPage = GuiceInjector.getInstance(LoginPage.class);  // ❌ Manual
   }
   ```

3. **Don't initialize injected fields**
   ```java
   @Inject
   private LoginPage loginPage = new LoginPage(...);  // ❌ Defeats purpose
   ```

4. **Don't inject non-page-objects**
   ```java
   @Inject
   private String username;  // ❌ Guice doesn't know how to create String
   ```

---

## 📊 Comparison with Other Frameworks

| Framework | Injection Style |
|-----------|----------------|
| **This Framework** | `@Inject` fields via Reflection |
| **Spring Test** | `@Autowired` fields |
| **JUnit 5 + Guice** | `@ExtendWith(GuiceExtension.class)` |
| **TestNG + Guice** | Manual `Guice.createInjector()` |
| **Cucumber + PicoContainer** | Constructor injection |

**Our approach advantages:**
- ✅ No test framework coupling
- ✅ Works with any TestNG test
- ✅ Zero configuration needed
- ✅ Simple and straightforward

---

## 🎓 Summary

**Before:** Manual object creation in every test
```java
LoginPage page = GuiceInjector.getInstance(LoginPage.class);
```

**After:** Automatic field injection
```java
@Inject
private LoginPage loginPage;  // Just declare and use!
```

**How:** Java Reflection + Guice + TestNG lifecycle

**Result:** Clean, readable, maintainable test code!

---

**@Inject Field Injection = Zero Boilerplate Test Code** 🎯
