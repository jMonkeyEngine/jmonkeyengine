package checkers.quals;

/**
 * Specifies the locations to which a {@link DefaultQualifier} annotation applies.
 *
 * @see DefaultQualifier
 */
public enum DefaultLocation {

    /** Apply default annotations to all unannotated types. */
    ALL,

    /** Apply default annotations to all unannotated types except the raw types
     * of locals. */
    ALL_EXCEPT_LOCALS,

    /** Apply default annotations to unannotated upper bounds:  both
     * explicit ones in <code>extends</code> clauses, and implicit upper bounds
     * when no explicit <code>extends</code> or <code>super</code> clause is
     * present. */
    // Especially useful for parameterized classes that provide a lot of
    // static methods with the same generic parameters as the class.
    UPPER_BOUNDS;

}
