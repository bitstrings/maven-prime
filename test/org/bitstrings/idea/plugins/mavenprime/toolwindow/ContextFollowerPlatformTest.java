package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import com.intellij.ide.impl.HeadlessDataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class ContextFollowerPlatformTest
    extends BasePlatformTestCase
{
    private final List<VirtualFile> reported = new ArrayList<>();

    private final VirtualFile pom = new LightVirtualFile("pom.xml");

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

        follower.follow(source);

        assertEquals(List.of(pom), reported);
    }

    public void testFollow_aComponentInsideTheOwner_reportsNothing()
    {
        ContextPanel source = new ContextPanel(pom);

        owner.add(source);

        follower.follow(source);

        assertEquals(List.of(), reported);
    }

    public void testFollow_null_reportsNothing()
    {
        follower.follow(null);

        assertEquals(List.of(), reported);
    }

    public void testFollow_aComponentWithoutAFile_reportsNothing()
    {
        follower.follow(new JPanel());

        assertEquals(List.of(), reported);
    }

    public void testFollow_aComponentOfAnotherProject_reportsNothing()
    {
        ContextFollower foreign =
            new ContextFollower(ProjectManager.getInstance().getDefaultProject(), owner, reported::add);

        Disposer.register(getTestRootDisposable(), foreign);

        foreign.follow(new ContextPanel(pom));

        assertEquals(List.of(), reported);
    }

    public void testFollow_aTree_reportsAgainWhenItsSelectionChanges()
    {
        ContextTree source = new ContextTree(pom);

        follower.follow(source);
        reported.clear();

        source.setSelectionRow(0);

        assertEquals(List.of(pom), reported);
    }

    public void testDispose_afterFollowingATree_stopsReportingItsSelectionChanges()
    {
        ContextTree source = new ContextTree(pom);

        follower.follow(source);
        reported.clear();

        Disposer.dispose(follower);

        source.setSelectionRow(0);

        assertEquals(List.of(), reported);
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

        private final transient VirtualFile file;

        ContextTree(VirtualFile file)
        {
            super(new DefaultMutableTreeNode("root"));

            this.file = file;
        }

        @Override
        public void uiDataSnapshot(DataSink sink)
        {
            sink.set(CommonDataKeys.VIRTUAL_FILE, file);
        }
    }
}
