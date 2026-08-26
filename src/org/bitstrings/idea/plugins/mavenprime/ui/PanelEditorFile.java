package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.function.Function;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.intellij.ide.actions.SplitAction;
import com.intellij.ide.plugins.UIComponentFileEditor;
import com.intellij.ide.plugins.UIComponentVirtualFile;
import com.intellij.openapi.Disposable;

public final class PanelEditorFile
    extends UIComponentVirtualFile
{
    private static final long serialVersionUID = 1L;

    private final transient Function<Disposable, JComponent> panelFactory;

    public PanelEditorFile(String name, Icon icon, Function<Disposable, JComponent> panelFactory)
    {
        super(name, icon);

        this.panelFactory = panelFactory;

        putUserData(SplitAction.FORBID_TAB_SPLIT, Boolean.TRUE);
    }

    @Override
    public Content createContent(UIComponentFileEditor editor)
    {
        return () -> panelFactory.apply(editor);
    }
}
