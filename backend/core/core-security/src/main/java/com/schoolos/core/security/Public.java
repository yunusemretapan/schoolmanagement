package com.schoolos.core.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a request that needs no signed-in user. Each use is a deliberate exception to default deny
 * (docs/security.md): the status query (spec 0001) and the login/refresh commands (spec 0003).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Public {}
