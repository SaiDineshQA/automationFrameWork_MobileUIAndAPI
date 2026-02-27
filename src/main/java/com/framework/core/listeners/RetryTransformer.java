package com.framework.core.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * RetryTransformer - Automatically applies RetryAnalyzer to ALL test methods.
 *
 * Register as a listener in testng.xml:
 *   <listener class-name="com.framework.core.listeners.RetryTransformer"/>
 *
 * This eliminates the need to add @Test(retryAnalyzer = RetryAnalyzer.class)
 * on every test method — it's applied globally.
 *
 * If a test already has a custom retryAnalyzer set, it will NOT be overridden.
 */
public class RetryTransformer implements IAnnotationTransformer {

    @Override
    public void transform(ITestAnnotation annotation, Class testClass,
                          Constructor testConstructor, Method testMethod) {
        if (annotation.getRetryAnalyzerClass() == null) {
            annotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
