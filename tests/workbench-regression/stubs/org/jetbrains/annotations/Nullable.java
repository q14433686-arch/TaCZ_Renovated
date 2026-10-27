package org.jetbrains.annotations;
import java.lang.annotation.*;
@Target({ElementType.TYPE_USE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {}
