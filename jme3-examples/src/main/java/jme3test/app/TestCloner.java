/*
 * Copyright (c) 2016-2021 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package jme3test.app;

import java.util.*;

import com.jme3.util.clone.*;

/**
 *
 *
 *  @author    Paul Speed
 */
public class TestCloner {
    
    public static void main( String... args ) {
        
        System.out.println("Clone test:");
        
        Cloner cloner = new Cloner();
        
        RegularObject ro = new RegularObject(42);
        System.out.println("Regular Object:" + ro);
        RegularObject roCloneLegacy = ro.clone();
        System.out.println("Regular Object Clone:" + roCloneLegacy);
        RegularObject roClone = cloner.clone(ro);
        System.out.println("cloner: Regular Object Clone:" + roClone);
 
        System.out.println("------------------------------------");
        System.out.println();
        
        cloner = new Cloner();       
        RegularSubclass rsc = new RegularSubclass(69, "test");
        System.out.println("Regular subclass:" + rsc);
        RegularSubclass rscCloneLegacy = (RegularSubclass)rsc.clone();
        System.out.println("Regular subclass Clone:" + rscCloneLegacy);
        RegularSubclass rscClone = cloner.clone(rsc);
        System.out.println("cloner: Regular subclass Clone:" + rscClone);
        
        System.out.println("------------------------------------");
        System.out.println();
 
        cloner = new Cloner();       
        Parent parent = new Parent("Foo", 34);
        System.out.println("Parent:" + parent);
        Parent parentCloneLegacy = parent.clone();
        System.out.println("Parent Clone:" + parentCloneLegacy);
        Parent parentClone = cloner.clone(parent);
        System.out.println("cloner: Parent Clone:" + parentClone);
         
        System.out.println("------------------------------------");
        System.out.println();
        
        cloner = new Cloner();
        GraphNode root = new GraphNode("root");
        GraphNode child1 = root.addLink("child1");
        GraphNode child2 = root.addLink("child2");        
        GraphNode shared = child1.addLink("shared");
        child2.addLink(shared);
        
        // Add a circular reference to get fancy
        shared.addLink(root);
        
        System.out.println("Simple graph:");
        root.dump("  ");
        
        GraphNode rootClone = cloner.clone(root);
        System.out.println("clone:");  
        rootClone.dump("  ");
        
        System.out.println("original:");
        root.dump("  ");
 
        GraphNode reclone = Cloner.deepClone(root);
        System.out.println("reclone:");  
        reclone.dump("  ");
        
        System.out.println("------------------------------------");
        System.out.println();
        cloner = new Cloner();
        
        ArrayHolder arrays = new ArrayHolder(5, 3, 7, 3, 7, 2, 1, 4);
        System.out.println("Array holder:" + arrays);       
        ArrayHolder arraysClone = cloner.clone(arrays);
        System.out.println("Array holder clone:" + arraysClone);       
 
           
        
    }
    
    public static class RegularObject implements Cloneable {
        protected int i;
        
        public RegularObject( int i ) {
            this.i = i;
        }
 
        @Override
        public RegularObject clone() {
            try {
                return (RegularObject)super.clone();
            } catch( CloneNotSupportedException e ) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + System.identityHashCode(this) 
                    + "[i=" + i + "]";
        }
    }
    
    public static class RegularSubclass extends RegularObject {
        protected String name;
        
        public RegularSubclass( int i, String name ) {
            super(i);
            this.name = name;
        }
               
        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + System.identityHashCode(this) 
                    + "[i=" + i + ", name=" + name + "]";
        }
    }
    
    public static class Parent implements Cloneable, JmeCloneable {
        
        private RegularObject ro;
        private RegularSubclass rsc;
        
        public Parent( String name, int age ) {
            this.ro = new RegularObject(age);
            this.rsc = new RegularSubclass(age, name);
        }
        
        @Override
        public Parent clone() {
            try {
                return (Parent)super.clone();
            } catch( CloneNotSupportedException e ) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public Parent jmeClone() {
            // Ok to delegate to clone() in this case because no deep
            // cloning is done there.
            return clone();
        }
 
        @Override
        public void cloneFields( Cloner cloner, Object original ) {
            this.ro = cloner.clone(ro);
            this.rsc = cloner.clone(rsc);
        } 
        
        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + System.identityHashCode(this)
                    + "[ro=" + ro + ", rsc=" + rsc + "]";
        }
    }
    
    public static class GraphNode implements Cloneable, JmeCloneable {
 
        final private String name;       
        private List<GraphNode> links = new ArrayList<>();
        
        public GraphNode( String name ) {
            this.name = name;
        }
        
        public void dump( String indent ) {
            dump(indent, new HashSet<GraphNode>());        
        }
        
        private void dump( String indent, Set<GraphNode> visited ) {
            if( visited.contains(this) ) {
                // already been here
                System.out.println(indent + this + " ** circular.");
                return;
            } 
            System.out.println(indent + this);
            visited.add(this);
            for( GraphNode n : links ) {
                n.dump(indent + "    ", visited);
            }
            visited.remove(this);
        }
        
        public GraphNode addLink( String name ) {
            GraphNode node = new GraphNode(name);
            links.add(node);
            return node;
        }
        
        public GraphNode addLink( GraphNode node ) {
            links.add(node);
            return node;
        }
        
        public List<GraphNode> getLinks() {
            return links;
        }
        
        @Override
        public GraphNode jmeClone() {
            try {
                return (GraphNode)super.clone();
            } catch( CloneNotSupportedException e ) {
                throw new RuntimeException(e);
            }
        }
 
        @Override
        public void cloneFields( Cloner cloner, Object original ) {
            this.links = cloner.clone(links);
        } 
        
        @Override
        public String toString() {
            return getClass().getSimpleName() + "@" + System.identityHashCode(this)
                    + "[name=" + name + "]";
        }
    }
    
    public static class ArrayHolder implements JmeCloneable {
    
        private int[] intArray;
        private int[][] intArray2D;
        final private Object[] objects;
        private RegularObject[] regularObjects;
        final private String[] strings;
 
        public ArrayHolder( int... values ) {
            this.intArray = values;
            this.intArray2D = new int[values.length][2];
            for( int i = 0; i < values.length; i++ ) {
                intArray2D[i][0] = values[i] + 1;
                intArray2D[i][1] = values[i] * 2;
            }
            this.objects = new Object[values.length];
            this.regularObjects = new RegularObject[values.length];
            this.strings = new String[values.length];
            for( int i = 0; i < values.length; i++ ) {
                objects[i] = values[i];
                regularObjects[i] = new RegularObject(values[i]);
                strings[i] = String.valueOf(values[i]);   
            }
        }
        
        @Override
        public ArrayHolder jmeClone() {
            try {
                return (ArrayHolder)super.clone();
            } catch( CloneNotSupportedException e ) {
                throw new RuntimeException(e);
            }
        }
 
        @Override
        public void cloneFields( Cloner cloner, Object original ) {
            intArray = cloner.clone(intArray);
            intArray2D = cloner.clone(intArray2D);
            
            // Boxed types are not cloneable so this will fail
            //objects = cloner.clone(objects);
            
            regularObjects = cloner.clone(regularObjects);
            
            // Strings are also not cloneable
            //strings = cloner.clone(strings);
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("intArray=" + intArray);
            for( int i = 0; i < intArray.length; i++ ) {
                if( i == 0 ) {
                    sb.append("[");
                } else {
                    sb.append(", ");
                }
                sb.append(intArray[i]);
            }
            sb.append("], ");
            
            sb.append("intArray2D=" + intArray2D);
            for( int i = 0; i < intArray2D.length; i++ ) {
                if( i == 0 ) {
                    sb.append("[");
                } else {
                    sb.append(", ");
                }
                sb.append("intArray2D[" + i + "]=" + intArray2D[i]);
                for( int j = 0; j < 2; j++ ) {
                    if( j == 0 ) {
                        sb.append("[");
                    } else {
                        sb.append(", ");
                    }
                    sb.append(intArray2D[i][j]);                    
                }
                sb.append("], ");
            }
            sb.append("], ");
            
            sb.append("objectArray=" + objects);
            for( int i = 0; i < objects.length; i++ ) {
                if( i == 0 ) {
                    sb.append("[");
                } else {
                    sb.append(", ");
                }
                sb.append(objects[i]);
            }
            sb.append("], ");
            
            sb.append("objectArray=" + regularObjects);
            for( int i = 0; i < regularObjects.length; i++ ) {
                if( i == 0 ) {
                    sb.append("[");
                } else {
                    sb.append(", ");
                }
                sb.append(regularObjects[i]);
            }
            sb.append("], ");
            
            sb.append("stringArray=" + strings);
            for( int i = 0; i < strings.length; i++ ) {
                if( i == 0 ) {
                    sb.append("[");
                } else {
                    sb.append(", ");
                }
                sb.append(strings[i]);
            }
            sb.append("]");
            
            return getClass().getSimpleName() + "@" + System.identityHashCode(this)
                    + "[" + sb + "]";
        }       
    }
}
