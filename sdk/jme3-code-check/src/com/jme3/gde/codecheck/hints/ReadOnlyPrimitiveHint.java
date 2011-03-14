package com.jme3.gde.codecheck.hints;

import com.jme3.system.Annotations.ReadOnly;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
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

public class ReadOnlyPrimitiveHint extends AbstractHint {

    //This hint does not enable the IDE to fix the problem:
    private static final List<Fix> NO_FIXES = Collections.<Fix>emptyList();
    //This hint applies to method invocations:
    private static final Set<Tree.Kind> TREE_KINDS =
            EnumSet.<Tree.Kind>of(Tree.Kind.METHOD_INVOCATION);

    public ReadOnlyPrimitiveHint() {
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

//        MethodInvocationTree mit = (MethodInvocationTree) treePath.getLeaf();
//        ExpressionTree select = mit.getMethodSelect();
//        TreePath method = new TreePath(treePath, select);
//        Element el = info.getTrees().getElement(method);
//        TypeElement invokedClass = (TypeElement) el.getEnclosingElement();

        if (info.getTrees().getElement(treePath).getAnnotation(ReadOnly.class) != null) {
            Tree t = treePath.getLeaf();
            //prepare selection for removing
            JTextComponent editor = EditorRegistry.lastFocusedComponent();
//            Document doc = editor.getDocument();
            SourcePositions sp = info.getTrees().getSourcePositions();
//            int start = (int) sp.getStartPosition(info.getCompilationUnit(), t);
            int end = (int) sp.getEndPosition(info.getCompilationUnit(), t);

            //TODO: add more checks
            boolean fail = false;
            if (info.getText().length() >= end + 4) {
                String dotText = info.getText().substring(end, end + 4);
                if (".set".equals(dotText)) {
                    fail = true;
                }
            }

            if (fail) {
                return Collections.<ErrorDescription>singletonList(
                        ErrorDescriptionFactory.createErrorDescription(
                        getSeverity().toEditorSeverity(),
                        getDisplayName(),
                        NO_FIXES,
                        info.getFileObject(),
                        (int) info.getTrees().getSourcePositions().getStartPosition(info.getCompilationUnit(), t),
                        (int) info.getTrees().getSourcePositions().getEndPosition(info.getCompilationUnit(), t)));
            }
            return null;

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
        return "This primitive is read only and should not be modified!";
    }

    //Name of the hint in the Options window:
    @Override
    public String getId() {
        return "ReadOnly Primitives";
    }

    //Description of the hint in the Options window:
    @Override
    public String getDescription() {
        return "Checks for modifications to readonly primitives. (getLocalTranslation().set())";
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
            return "Remove this call";
        }

        @Override
        public ChangeInfo implement() throws Exception {
            //Add 1 character, for the semi-colon:
            doc.remove(start, bodyText.length() + 1);
            //Display message to user in status bar:
            StatusDisplayer.getDefault().setStatusText("Removed: " + bodyText);
            return null;
        }
    }
}
