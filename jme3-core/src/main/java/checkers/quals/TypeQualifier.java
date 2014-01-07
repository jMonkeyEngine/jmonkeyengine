package checkers.quals;

import java.lang.annotation.*;

/**
 * A meta-annotation indicating that the annotated annotation is a type
 * qualifier.
 *
 * Examples of such qualifiers: {@code @ReadOnly}, {@code @NonNull}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface TypeQualifier {

}
