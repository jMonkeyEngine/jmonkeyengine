package checkers.quals;

import java.lang.annotation.Target;

/**
 * A special annotation intended solely for representing an unqualified type in
 * the qualifier hierarchy, as an argument to {@link SubtypeOf#value()},
 * in the type qualifiers declarations.
 *
 * <p>
 * Programmers cannot write this in source code.
 */
@TypeQualifier
@SubtypeOf({})
@Target({}) // empty target prevents programmers from writing this in a program
public @interface Unqualified { }
