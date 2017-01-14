package com.jme3.material;

import com.jme3.renderer.Caps;
import com.jme3.util.SafeArrayList;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;

/**
 * @author JavaSaBr
 */
public class ThreadLocalTechDefList {

    private static final ThreadLocal<ThreadLocalTechDefList> THREAD_LOCAL = new ThreadLocal<ThreadLocalTechDefList>() {

        @Override
        protected ThreadLocalTechDefList initialValue() {
            return new ThreadLocalTechDefList();
        }
    };

    /**
     * The comparator by GLSL version.
     */
    private static final Comparator<TechniqueDef> TECHNIQUE_DEF_COMPARATOR_BY_GLSL = new Comparator<TechniqueDef>() {

        @Override
        public int compare(final TechniqueDef first, final TechniqueDef second) {

            if (first == null) {
                return 1;
            } else if (second == null) {
                return -1;
            }

            int firstLevel = getLevel(first.getRequiredCaps());
            int secondLevel = getLevel(second.getRequiredCaps());
            
            return secondLevel - firstLevel;
        }
    };

    /**
     * Get a level of the caps.
     *
     * @param caps the caps.
     * @return the level.
     */
    private static int getLevel(final EnumSet<Caps> caps) {

        if (caps.contains(Caps.GLSL400)) {
            return 7;
        } else if (caps.contains(Caps.GLSL330)) {
            return 6;
        } else if (caps.contains(Caps.GLSL150)) {
            return 5;
        } else if (caps.contains(Caps.GLSL140)) {
            return 4;
        } else if (caps.contains(Caps.GLSL130)) {
            return 3;
        } else if (caps.contains(Caps.GLSL120)) {
            return 2;
        } else if (caps.contains(Caps.GLSL110)) {
            return 1;
        }

        return 0;
    }

    /**
     * Get a thread local instance.
     *
     * @return the thread local instance.
     */
    public static ThreadLocalTechDefList get() {
        final ThreadLocalTechDefList defList = THREAD_LOCAL.get();
        defList.defs.clear();
        return defList;
    }

    /**
     * The list of technique definitions.
     */
    private final SafeArrayList<TechniqueDef> defs;

    public ThreadLocalTechDefList() {
        this.defs = new SafeArrayList<>(TechniqueDef.class);
    }

    /**
     * Add a new technique definition.
     *
     * @param techniqueDef the new technique definition.
     */
    public void add(final TechniqueDef techniqueDef) {
        this.defs.add(techniqueDef);
    }

    /**
     * Get a technique definition with the greatest GLSL version.
     *
     * @return the technique definition.
     */
    public TechniqueDef getWithGreatestGLSL() {

        if (defs.isEmpty()) {
            return null;
        } else if (defs.size() == 1) {
            return defs.get(0);
        }

        Arrays.sort(defs.getArray(), TECHNIQUE_DEF_COMPARATOR_BY_GLSL);

        return defs.get(0);
    }
}
