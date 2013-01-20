/*
 * Copyright (c) 2003-2012 jMonkeyEngine
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
package com.jme3.gde.core.completion;

//import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.mimelookup.MimeRegistration;
import org.netbeans.spi.editor.codegen.CodeGenerator;
import org.netbeans.spi.editor.codegen.CodeGeneratorContextProvider;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

public class ModelGenerator implements CodeGenerator {

    JTextComponent textComp;

    /**
     *
     * @param context containing JTextComponent and possibly other items
     * registered by {@link CodeGeneratorContextProvider}
     */
    private ModelGenerator(Lookup context) { // Good practice is not to save Lookup outside ctor
        textComp = context.lookup(JTextComponent.class);
    }

//    @MimeRegistration(mimeType = "text/x-java", service = CodeGenerator.Factory.class)
    public static class Factory implements CodeGenerator.Factory {

        public List<? extends CodeGenerator> create(Lookup context) {
            return Collections.singletonList(new ModelGenerator(context));
        }
    }

    /**
     * The name which will be inserted inside Insert Code dialog
     */
    public String getDisplayName() {
        return "Sample Generator";
    }

    /**
     * This will be invoked when user chooses this Generator from Insert Code
     * dialog
     */
    public void invoke() {
        try {
            Document doc = textComp.getDocument();
            int caretPos = textComp.getCaretPosition();
            doc.insertString(caretPos, "hack", null);
//            Document doc = textComp.getDocument();
//            JavaSource javaSource = JavaSource.forDocument(doc);
//            CancellableTask task = new CancellableTask<WorkingCopy>() {
//                public void run(WorkingCopy workingCopy) throws IOException {
//                    workingCopy.toPhase(Phase.RESOLVED);
//                    CompilationUnitTree cut = workingCopy.getCompilationUnit();
//                    TreeMaker make = workingCopy.getTreeMaker();
//                    for (Tree typeDecl : cut.getTypeDecls()) {
//                        if (Tree.Kind.CLASS == typeDecl.getKind()) {
//                            ClassTree clazz = (ClassTree) typeDecl;
//                            ModifiersTree methodModifiers =
//                                    make.Modifiers(Collections.<Modifier>singleton(Modifier.PUBLIC),
//                                    Collections.<AnnotationTree>emptyList());
//                            VariableTree parameter =
//                                    make.Variable(make.Modifiers(Collections.<Modifier>singleton(Modifier.FINAL),
//                                    Collections.<AnnotationTree>emptyList()),
//                                    "arg0",
//                                    make.Identifier("Object"),
//                                    null);
//                            TypeElement element = workingCopy.getElements().getTypeElement("java.io.IOException");
//                            ExpressionTree throwsClause = make.QualIdent(element);
//                            MethodTree newMethod =
//                                    make.Method(methodModifiers,
//                                    "writeExternal",
//                                    make.PrimitiveType(TypeKind.VOID),
//                                    Collections.<TypeParameterTree>emptyList(),
//                                    Collections.singletonList(parameter),
//                                    Collections.<ExpressionTree>singletonList(throwsClause),
//                                    "{ throw new UnsupportedOperationException(\"Not supported yet.\") }",
//                                    null);
//                            ClassTree modifiedClazz = make.addClassMember(clazz, newMethod);
//                            workingCopy.rewrite(clazz, modifiedClazz);
//                        }
//                    }
//                }
//
//                public void cancel() {
//                }
//            };
//            ModificationResult result = javaSource.runModificationTask(task);
//            result.commit();
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
