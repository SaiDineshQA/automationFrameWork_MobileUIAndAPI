package com.framework.core.config;

import com.framework.pages.ui.BasePage;
import com.framework.utils.LoggerUtil;
import com.google.inject.AbstractModule;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * FrameworkModule - Guice module with automatic page object discovery.
 *
 * Uses Java reflection to scan the "com.framework.pages" package at runtime,
 * finds ALL concrete classes extending BasePage, and binds them automatically.
 *
 * To add a new page:
 *   1. Create MyNewPage.java in com.framework.pages extending BasePage
 *   2. Create MyNewPage.yaml in resources/locators
 *   3. Use @Inject protected MyNewPage myNewPage; in your test class
 *   — No changes needed here. Reflection handles it.
 *
 * How it works:
 *   1. ClassLoader locates the "com/framework/pages" package directory
 *   2. Scans all .class files in that directory
 *   3. Loads each class and checks: is it concrete? does it extend BasePage?
 *   4. If yes → bind(PageClass.class) so Guice can create instances via @Inject
 */
public class FrameworkModule extends AbstractModule {

    private static final String PAGES_PACKAGE = "com.framework.pages";

    @Override
    @SuppressWarnings("unchecked")
    protected void configure() {
        List<Class<? extends BasePage>> pageClasses = discoverPageClasses();

        for (Class<? extends BasePage> pageClass : pageClasses) {
            bind(pageClass);
            LoggerUtil.info("[Guice] Auto-bound page: " + pageClass.getSimpleName());
        }

        LoggerUtil.info("[Guice] Total pages discovered: " + pageClasses.size());
    }

    /**
     * Scans the pages package and returns all concrete classes extending BasePage.
     * Uses ClassLoader to find the package directory, then loads each .class file.
     */
    @SuppressWarnings("unchecked")
    private List<Class<? extends BasePage>> discoverPageClasses() {
        List<Class<? extends BasePage>> pages = new ArrayList<>();

        try {
            String path = PAGES_PACKAGE.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL packageUrl = classLoader.getResource(path);

            if (packageUrl == null) {
                LoggerUtil.warn("[Guice] Package not found: " + PAGES_PACKAGE);
                return pages;
            }

            File packageDir = new File(packageUrl.toURI());
            File[] classFiles = packageDir.listFiles((dir, name) -> name.endsWith(".class"));

            if (classFiles == null) return pages;

            for (File file : classFiles) {
                String className = PAGES_PACKAGE + "." + file.getName().replace(".class", "");
                Class<?> clazz = Class.forName(className);

                // Only bind concrete (non-abstract) classes that extend BasePage
                if (BasePage.class.isAssignableFrom(clazz)
                        && !Modifier.isAbstract(clazz.getModifiers())) {
                    pages.add((Class<? extends BasePage>) clazz);
                }
            }
        } catch (Exception e) {
            LoggerUtil.error("[Guice] Page discovery failed: " + e.getMessage());
        }

        return pages;
    }
}

