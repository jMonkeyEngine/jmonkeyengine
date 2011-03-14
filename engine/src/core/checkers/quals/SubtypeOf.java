package checkers.quals;

import java.lang.annotation.*;

/**
 * A meta-annotation to specify all the qualifiers that the given qualifier
 * is a subtype of.  This provides a declarative way to specify the type
 * qualifier hierarchy.  (Alternatively, the hierarchy can be defined
 * procedurally by subclassing {@link checkers.types.QualifierHierarchy} or
 * {@link checkers.types.TypeHierarchy}.)
 *
 * <p>
 * Example:
 * <pre> @SubtypeOf( { Nullable.class } )
 * public @interface NonNull { }
 * </pre>
 *
 * <p>
 *
 * If a qualified type is a subtype of the same type without any qualifier,
 * then use <code>Unqualified.class</code> in place of a type qualifier
 * class.  For example, to express that <code>@Encrypted <em>C</em></code>
 * is a subtype of <code><em>C</em></code> (for every class
 * <code><em>C</em></code>), and likewise for <code>@Interned</code>, write:
 *
 * <pre> @SubtypeOf(Unqualified.class)
 * public @interface Encrypted { }
 *
 * &#64;SubtypeOf(Unqualified.class)
 * public @interface Interned { }
 * </pre>
 *
 * <p>
 *
 * For the root type qualifier in the qualifier hierarchy (i.e., the
 * qualifier that is a supertype of all other qualifiers in the given
 * hierarchy), use an empty set of values:
 *
 * <pre> @SubtypeOf( { } )
 * public @interface Nullable { }
 *
 * &#64;SubtypeOf( {} )
 * public @interface ReadOnly { }
 * </pre>
 *
 * <p>
 * Together, all the @SubtypeOf meta-annotations fully describe the type
 * qualifier hierarchy.
 * No @SubtypeOf meta-annotation is needed on (or can be written on) the
 * Unqualified pseudo-qualifier, whose position in the hierarchy is
 * inferred from the meta-annotations on the explicit qualifiers.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface SubtypeOf {
    /** An array of the supertype qualifiers of the annotated qualifier **/
    Class<? extends Annotation>[] value();
}
