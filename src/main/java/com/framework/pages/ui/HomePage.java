package com.framework.pages.ui;

/**
 * HomePage - All locators from HomePage.yaml.
 * Injected via @Inject in test classes. AbstractBase constructor
 * initializes driver/locatorRepo/fluentWait automatically.
 */
public class HomePage extends BasePage {

    private static final String SEARCH_BAR      = "searchBar";
    private static final String CART_ICON       = "cartIcon";
    private static final String CART_BADGE      = "cartBadge";
    private static final String PROFILE_MENU    = "profileMenu";
    private static final String FEATURED_BANNER = "featuredBanner";
    private static final String PRODUCT_LIST    = "productList";
    private static final String LOGOUT_BUTTON   = "logoutButton";


    // ── Actions ───────────────────────────────────────────────

    public HomePage search(String query) {
        enterText(SEARCH_BAR, query);
        return this;
    }

    public void tapCart() {
        tap(CART_ICON);
    }

    public String getCartBadgeCount() {
        return getText(CART_BADGE);
    }

    public void openProfileMenu() {
        tap(PROFILE_MENU);
    }

    public void logout() {
        openProfileMenu();
        tap(LOGOUT_BUTTON);
    }

    public boolean isFeaturedBannerVisible() {
        return isVisible(FEATURED_BANNER);
    }


   }
