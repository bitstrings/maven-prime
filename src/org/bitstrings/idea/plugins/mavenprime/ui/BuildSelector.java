package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.List;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JList;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.util.BuildHistory.BuildEntry;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.util.text.DateFormatUtil;

public final class BuildSelector<T>
{
    private static final int MIN_BUILDS_TO_OFFER = 2;

    private final ComboBox<BuildEntry<T>> combo = new ComboBox<>();

    private final Consumer<BuildEntry<T>> onSelected;

    private boolean applicable = true;

    private int available;

    public BuildSelector(Consumer<BuildEntry<T>> onSelected)
    {
        this.onSelected = onSelected;

        combo.setRenderer(new EntryRenderer<T>());
        combo.addActionListener(event -> reportSelection());
    }

    public JComponent getComponent()
    {
        return combo;
    }

    public void reload(List<BuildEntry<T>> builds, BuildEntry<T> selected)
    {
        available = builds.size();

        combo.setModel(new CollectionComboBoxModel<>(builds, selected));

        applyVisibility();
    }

    public void setApplicable(boolean applicable)
    {
        this.applicable = applicable;

        applyVisibility();
    }

    private void applyVisibility()
    {
        combo.setVisible(applicable && (available >= MIN_BUILDS_TO_OFFER));
    }

    private void reportSelection()
    {
        onSelected.accept(combo.getItem());
    }

    static String labelOf(BuildEntry<?> entry)
    {
        String startedAt = DateFormatUtil.formatTime(entry.startedMillis());

        return StringUtils.isBlank(entry.name())
            ? startedAt
            : MavenPrimeBundle.message("mavenprime.build.entryLabel", entry.name(), startedAt);
    }

    private static final class EntryRenderer<T>
        extends SimpleListCellRenderer<BuildEntry<T>>
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void customize(
            JList<? extends BuildEntry<T>> list,
            BuildEntry<T> value,
            int index,
            boolean selected,
            boolean focused)
        {
            setText((value == null) ? MavenPrimeBundle.message("mavenprime.build.none") : labelOf(value));
        }
    }
}
