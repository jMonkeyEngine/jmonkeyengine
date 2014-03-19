/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.jme3.gde.core.assets.nodes;

import java.io.IOException;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.NotifyDescriptor.Confirmation;
import org.openide.cookies.SaveCookie;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;

/**
 *
 * @author normenhansen
 */
public class SaveNode extends AbstractNode {

    SaveCookie impl;

    public SaveNode() {
        super(Children.LEAF);
        impl = new SaveCookieImpl();
    }

    public SaveNode(SaveCookie impl) {
        super(Children.LEAF);
        this.impl = impl;
    }

    public void fire(boolean modified) {
        if (modified) {
            //If the text is modified,
            //we implement SaveCookie,
            //and add the implementation to the cookieset:
            getCookieSet().assign(SaveCookie.class, impl);
        } else {
            //Otherwise, we make no assignment
            //and the SaveCookie is not made available:
            getCookieSet().assign(SaveCookie.class);
        }
    }

    private class SaveCookieImpl implements SaveCookie {

        public void save() throws IOException {

            Confirmation msg = new NotifyDescriptor.Confirmation("This plugin can not save!",
                    NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.QUESTION_MESSAGE);

            Object result = DialogDisplayer.getDefault().notify(msg);

            //When user clicks "Yes", indicating they really want to save,
            //we need to disable the Save button and Save menu item,
            //so that it will only be usable when the next change is made
            //to the text field:
            if (NotifyDescriptor.YES_OPTION.equals(result)) {
                fire(false);
                //Implement your save functionality here.
            }

        }
    }
}
