package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import com.intellij.ide.impl.HeadlessDataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;

public class ContextFollowerPlatformTest
    extends BasePlatformTestCase
{
    private final List<VirtualFile> reported = new ArrayList<>();

    private final VirtualFile pom = new LightVirtualFile("pom.xml");

    private final VirtualFile selectedPom = new LightVirtualFile("child-pom.xml");

    private JPanel owner;

    private ContextFollower follower;

    @Override
    protected void setUp()
        throws Exception
    {
        super.setUp();

        HeadlessDataManager.fallbackToProductionDataManager(getTestRootDisposable());

        owner = new JPanel();
        follower = new ContextFollower(getProject(), owner, reported::add);

        Disposer.register(getTestRootDisposable(), follower);
    }

    public void testFollow_aComponentOutsideTheOwner_reportsItsFile()
    {
        ContextPanel source = new ContextPanel(pom);

        follow(source);

        assertEquals(List.of(pom), reported);
    }

    public void testFollow_aComponentOutsideTheOwner_readsNothingOnTheCallingThread()
    {
        follower.follow(new ContextPanel(pom));

        assertEquals(
            "reading the file on the event thread waits for the read lock there, and the events pumped "
                + "while it waits re-enter the action system",
            List.of(),
            reported);
    }

    public void testFollow_aComponentInsideTheOwner_reportsNothing()
    {
        ContextPanel source = new ContextPanel(pom);

        owner.add(source);

        follow(source);

        assertEquals(List.of(), reported);
    }

    public void testFollow_null_reportsNothing()
    {
        follow(null);

        assertEquals(List.of(), reported);
    }

    public void testFollow_aComponentWithoutAFile_reportsNothing()
    {
        follow(new JPanel());

        assertEquals(List.of(), reported);
    }

    public void testFollow_aComponentOfAnotherProject_reportsNothing()
    {
        ContextFollower foreign =
            new ContextFollower(ProjectManager.getInstance().getDefaultProject(), owner, reported::add);

        Disposer.register(getTestRootDisposable(), foreign);

        foreign.follow(new ContextPanel(pom));

        settle();

        assertEquals(List.of(), reported);
    }

    public void testFollow_aTree_reportsAgainWhenItsSelectionChanges()
    {
        ContextTree source = new ContextTree(pom, selectedPom);

        follow(source);
        reported.clear();

        source.setSelectionRow(0);

        settle();

        assertEquals(List.of(selectedPom), reported);
    }

    public void testFollow_aTreeSelectedBeforeTheReadFinishes_reportsWhatTheSelectionBecame()
    {
        ContextTree source = new ContextTree(pom, selectedPom);

        follower.follow(source);

        source.setSelectionRow(0);

        settle();

        assertEquals(
            "a tree has to be listened to as it is focused, or the click that focused it is read as the "
                + "selection it replaced",
            List.of(selectedPom),
            reported);
    }

    public void testDispose_afterFollowingATree_stopsReportingItsSelectionChanges()
    {
        ContextTree source = new ContextTree(pom, selectedPom);

        follow(source);
        reported.clear();

        Disposer.dispose(follower);

        source.setSelectionRow(0);

        settle();

        assertEquals(List.of(), reported);
    }

    private void follow(Component source)
    {
        follower.follow(source);

        settle();
    }

    private static void settle()
    {
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();

        UIUtil.dispatchAllInvocationEvents();
    }

    private static final class ContextPanel
        extends JPanel
        implements UiDataProvider
    {
        private static final long serialVersionUID = 1L;

        private final transient VirtualFile file;

        ContextPanel(VirtualFile file)
        {
            this.file = file;
        }

        @Override
        public void uiDataSnapshot(DataSink sink)
        {
            sink.set(CommonDataKeys.VIRTUAL_FILE, file);
        }
    }

    private static final class ContextTree
        extends JTree
        implements UiDataProvider
    {
        private static final long serialVersionUID = 1L;

        private final transient VirtualFile unselected;

        private final transient VirtualFile selected;

        ContextTree(VirtualFile unselected, VirtualFile selected)
        {
            super(new DefaultMutableTreeNode("root"));

            this.unselected = unselected;
            this.selected = selected;
        }

        @Override
        public void uiDataSnapshot(DataSink sink)
        {
            sink.set(CommonDataKeys.VIRTUAL_FILE, isSelectionEmpty() ? unselected : selected);
        }
    }
}
