package com.jme3.gde.codecheck.hints;

import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.editor.EditorRegistry;
import org.netbeans.api.java.source.CompilationInfo;
import org.netbeans.modules.java.hints.spi.AbstractHint;
import org.netbeans.spi.editor.hints.ChangeInfo;
import org.netbeans.spi.editor.hints.EnhancedFix;
import org.netbeans.spi.editor.hints.ErrorDescription;
import org.netbeans.spi.editor.hints.ErrorDescriptionFactory;
import org.netbeans.spi.editor.hints.Fix;
import org.openide.awt.StatusDisplayer;

public class TempVarsHint extends AbstractHint {

    //This hint does not enable the IDE to fix the problem:
    private static final List<Fix> NO_FIXES = Collections.<Fix>emptyList();
    //This hint applies to method invocations:
    private static final Set<Tree.Kind> TREE_KINDS =
            EnumSet.<Tree.Kind>of(Tree.Kind.METHOD);
    private List<varsPosition> vars = new ArrayList<varsPosition>();

    public TempVarsHint() {
        super(true, true, AbstractHint.HintSeverity.WARNING);
    }

    //Specify the kind of code that the hint applies to, in this case,
    //the hint applies to method invocations:
    @Override
    public Set<Kind> getTreeKinds() {
        return TREE_KINDS;
    }

    @Override
    public List<ErrorDescription> run(CompilationInfo info, TreePath treePath) {

        MethodTree mt = (MethodTree) treePath.getLeaf();
        vars.clear();
        if (mt.getBody() != null) {
            for (StatementTree t : mt.getBody().getStatements()) {


                if (t.getKind().equals(Tree.Kind.VARIABLE)) {
                    Element el = info.getTrees().getElement(info.getTrees().getPath(info.getCompilationUnit(), t));
                    String name = t.toString();

                    //This is where it all happens: if the method invocation is 'showMessageDialog',
                    //then the hint infrastructure kicks into action:
                    if (name.indexOf("TempVars.get()") >= 0) {

                        SourcePositions sp = info.getTrees().getSourcePositions();
                        int start = (int) sp.getStartPosition(info.getCompilationUnit(), t);
                        int end = (int) sp.getEndPosition(info.getCompilationUnit(), t);
                        vars.add(new varsPosition(el.getSimpleName().toString(), start, end));
                        // System.err.println("TempVars.get() at " + start + " " + end+" for variable "+el.getSimpleName().toString());
                    }

                }
                if (t.getKind().equals(Tree.Kind.EXPRESSION_STATEMENT) && !vars.isEmpty()) {
                    Element el = info.getTrees().getElement(treePath);
                    String name = t.toString();


                    if (name.indexOf(".release()") >= 0) {

                        for (Iterator<varsPosition> it = vars.iterator(); it.hasNext();) {
                            varsPosition curVar = it.next();
                            //This is where it all happens: if the method invocation is 'showMessageDialog',
                            //then the hint infrastructure kicks into action:
                            if (name.indexOf(curVar.varName + ".release()") >= 0) {
                                //prepare selection for removing                       
                                it.remove();

//                            SourcePositions sp = info.getTrees().getSourcePositions();
//                            int start = (int) sp.getStartPosition(info.getCompilationUnit(), t);
//                            int end = (int) sp.getEndPosition(info.getCompilationUnit(), t);
//                            System.err.println(curVar.varName + ".release() at " + start + " " + end);

                            }
                        }
                    }

                }


            }
        }
        if (!vars.isEmpty()) {
            List<ErrorDescription> list = new ArrayList<ErrorDescription>();

            JTextComponent editor = EditorRegistry.lastFocusedComponent();
            Document doc = editor.getDocument();
            List<Fix> fixes = new ArrayList<Fix>();
            SourcePositions sp = info.getTrees().getSourcePositions();
            int methodEnd = (int) (sp.getEndPosition(info.getCompilationUnit(), mt) - 1);

            for (varsPosition curVar : vars) {
                String bodyText = "    "+curVar.varName + ".release();\n    ";
                fixes.clear();
                fixes.add(new MessagesFix(doc, methodEnd, bodyText));

                list.add(ErrorDescriptionFactory.createErrorDescription(
                        getSeverity().toEditorSeverity(),
                        getDisplayName(),
                        fixes,
                        info.getFileObject(),
                        curVar.start, curVar.end));
            }
            return list;
        }

        return null;

    }

    //This is called if/when the hint processing is cancelled:
    @Override
    public void cancel() {
    }

    //Message that the user sees in the left sidebar:
    @Override
    public String getDisplayName() {
        return "TempVars might not be released";
    }

    //Name of the hint in the Options window:
    @Override
    public String getId() {
        return "TempVars release check";
    }

    //Description of the hint in the Options window:
    @Override
    public String getDescription() {
        return "Checks for calls TempVars.get() and search for correspondinng release() call";
    }

    class MessagesFix implements EnhancedFix {

        Document doc = null;
        int start = 0;
        String bodyText = null;

        public MessagesFix(Document doc, int start, String bodyText) {
            this.doc = doc;
            this.start = start;
            this.bodyText = bodyText;
        }

        @Override
        public CharSequence getSortText() {
            return "charsequence";
        }

        @Override
        public String getText() {
            return "Add a release() call at the end of the method";
        }

        @Override
        public ChangeInfo implement() throws Exception {
            //Adding the release call
            doc.insertString(start, bodyText, null);            
            //Display message to user in status bar:
            StatusDisplayer.getDefault().setStatusText("Added: " + bodyText);
            return null;
        }
    }

    class varsPosition {

        String varName;
        int start;
        int end;

        public varsPosition(String varName, int start, int end) {
            this.varName = varName;
            this.end = end;
            this.start = start;
        }
    }
}
