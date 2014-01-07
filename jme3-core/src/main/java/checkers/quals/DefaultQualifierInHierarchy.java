package checkers.quals;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated qualifier is the default qualifier in the
 * qualifier hierarchy:  it applies if the programmer writes no explicit
 * qualifier.
 * <p>
 *
 * The {@link DefaultQualifier} annotation, which targets Java code elements,
 * takes precedence over {@code DefaultQualifierInHierarchy}.
 * <p>
 *
 * Each type qualifier hierarchy may have at most one qualifier marked as
 * {@code DefaultQualifierInHierarchy}.
 *
 * @see checkers.quals.DefaultQualifier
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ANNOTATION_TYPE)
public @interface DefaultQualifierInHierarchy {

}
