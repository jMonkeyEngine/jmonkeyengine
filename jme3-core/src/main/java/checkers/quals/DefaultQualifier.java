package checkers.quals;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Applied to a declaration of a package, type, method, variable, etc.,
 * specifies that the given annotation should be the default.  The default is
 * applied to all types within the declaration for which no other
 * annotation is explicitly written.
 * If multiple DefaultQualifier annotations are in scope, the innermost one
 * takes precedence.
 * DefaultQualifier takes precedence over {@link DefaultQualifierInHierarchy}.
 * <p>
 *
 * If you wish to write multiple @DefaultQualifier annotations (for
 * unrelated type systems, or with different {@code locations} fields) at
 * the same location, use {@link DefaultQualifiers}.
 *
 * @see DefaultLocation
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({CONSTRUCTOR, METHOD, FIELD, LOCAL_VARIABLE, PARAMETER, TYPE})
public @interface DefaultQualifier {

    /**
     * The name of the default annotation.  It may be a short name like
     * "NonNull", if an appropriate import statement exists.  Otherwise, it
     * should be fully-qualified, like "checkers.nullness.quals.NonNull".
     * <p>
     *
     * To prevent affecting other type systems, always specify an annotation
     * in your own type hierarchy.  (For example, do not set
     * "checkers.quals.Unqualified" as the default.)
     */
    String value();

    /** @return the locations to which the annotation should be applied */
    DefaultLocation[] locations() default {DefaultLocation.ALL};
}
