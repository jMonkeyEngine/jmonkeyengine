package checkers.quals;

import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.*;

/**
 * Declares that the field may not be accessed if the receiver is of the
 * specified qualifier type (or any supertype).
 *
 * This property is verified by the checker that type-checks the {@code
 * when} element value qualifier.
 *
 * <p><b>Example</b>
 * Consider a class, {@code Table}, with a locking field, {@code lock}.  The
 * lock is used when a {@code Table} instance is shared across threads.  When
 * running in a local thread, the {@code lock} field ought not to be used.
 *
 * You can declare this behavior in the following way:
 *
 * <pre><code>
 * class Table {
 *   private @Unused(when=LocalToThread.class) final Lock lock;
 *   ...
 * }
 * </code></pre>
 *
 * The checker for {@code @LocalToThread} would issue an error for the following code:
 *
 * <pre>  @LocalToThread Table table = ...;
 *   ... table.lock ...;
 * </pre>
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD})
public @interface Unused {
    /**
     * The field that is annotated with @Unused may not be accessed via a
     * receiver that is annotated with the "when" annotation.
     */
    Class<? extends Annotation> when();
}
