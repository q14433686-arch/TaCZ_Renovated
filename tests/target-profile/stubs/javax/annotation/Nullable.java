package javax.annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
@Target({ElementType.TYPE_USE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD})
public @interface Nullable {}
