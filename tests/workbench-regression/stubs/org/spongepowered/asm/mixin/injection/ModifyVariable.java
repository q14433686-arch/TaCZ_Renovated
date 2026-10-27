package org.spongepowered.asm.mixin.injection;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.METHOD)
public @interface ModifyVariable { String[] method(); At at(); boolean argsOnly() default false; int ordinal() default -1; int require() default -1; }
