package org.spectralpowered.revtools.deobfuscator.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ObfInfo {
    String owner() default "";
    String name() default "";
    String desc() default "";
    int opaque() default -1;
    int intMultiplier() default -1;
    long longMultiplier() default -1L;
}
