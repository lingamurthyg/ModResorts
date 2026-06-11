package com.acme.modres.security;

public class Service {
  public static final String OPERATION = "my-operation";

  public void operation() {
    // SecurityManager has been deprecated for removal since Java 17 (JEP 411).
    // System.getSecurityManager() is deprecated and will be removed in a future release.
    // The SecurityManager API is no longer the recommended way to enforce security policies.
    // Removed: SecurityManager securityManager = System.getSecurityManager();
    // If access control is required, use alternative mechanisms such as
    // module system encapsulation, or application-level authorization frameworks.
    System.out.println("Operation is executed");
  }
}
