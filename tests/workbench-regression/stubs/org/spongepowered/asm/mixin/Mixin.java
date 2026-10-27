package org.spongepowered.asm.mixin;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE)
public @interface Mixin { String[] targets() default {}; boolean remap() default true; }
