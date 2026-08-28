package org.bitstrings.idea.plugins.mavenprime.toolwindow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Function;

import javax.swing.SortOrder;

import org.apache.commons.lang3.StringUtils;
import org.bitstrings.idea.plugins.mavenprime.MavenPrimeBundle;
import org.bitstrings.idea.plugins.mavenprime.profile.DownloadSummary;
import org.bitstrings.idea.plugins.mavenprime.profile.DownloadSummary.RepositoryTotal;

import com.intellij.openapi.util.text.Formats;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;

public final class DownloadTable
    extends TableView<RepositoryTotal>
{
    private static final long serialVersionUID = 1L;

    private static final int TRANSFER_COLUMN = 3;

    private static final double PERCENT = 100.0D;

    private transient long wallClockMillis;

    public DownloadTable()
    {
        setShowGrid(false);

        getEmptyText().setText(MavenPrimeBundle.message("mavenprime.profile.downloads.empty"));

        setModelAndUpdateColumns(
            new ListTableModel<>(columns(), new ArrayList<>(), TRANSFER_COLUMN, SortOrder.DESCENDING));

        getListTableModel().setSortable(true);
    }

    public void setSummary(DownloadSummary downloads, long wallClock)
    {
        wallClockMillis = wallClock;

        getListTableModel().setItems(new ArrayList<>(downloads.repositories()));

        updateColumnSizes();
    }

    private ColumnInfo<?, ?>[] columns()
    {
        return new ColumnInfo<?, ?>[]
        {
            new TotalColumn(
                "repository",
                DownloadTable::nameOf,
                Comparator.comparing(DownloadTable::nameOf, String.CASE_INSENSITIVE_ORDER)),
            new TotalColumn(
                "files", DownloadTable::filesOf, Comparator.comparingInt(RepositoryTotal::count)),
            new TotalColumn(
                "size",
                total -> Formats.formatFileSize(total.bytes()),
                Comparator.comparingLong(RepositoryTotal::bytes)),
            new TotalColumn(
                "transfer",
                total -> Formats.formatDuration(total.transferMillis()),
                Comparator.comparingLong(RepositoryTotal::transferMillis)),
            new TotalColumn(
                "share", this::shareOf, Comparator.comparingLong(RepositoryTotal::wallClockMillis))
        };
    }

    private String shareOf(RepositoryTotal total)
    {
        return (wallClockMillis == 0L)
            ? StringUtils.EMPTY
            : MavenPrimeBundle.message(
                "mavenprime.profile.share",
                Double.valueOf((total.wallClockMillis() * PERCENT) / wallClockMillis));
    }

    private static String nameOf(RepositoryTotal total)
    {
        return StringUtils.defaultIfBlank(
            total.repository(), MavenPrimeBundle.message("mavenprime.profile.downloads.unknown"));
    }

    private static String filesOf(RepositoryTotal total)
    {
        return (total.metadataCount() == 0)
            ? String.valueOf(total.count())
            : MavenPrimeBundle.message(
                "mavenprime.profile.downloads.files",
                Integer.valueOf(total.count()),
                Integer.valueOf(total.metadataCount()));
    }

    private static final class TotalColumn
        extends ColumnInfo<RepositoryTotal, String>
    {
        private final transient Function<RepositoryTotal, String> text;

        private final transient Comparator<RepositoryTotal> order;

        private final transient String key;

        TotalColumn(
            String key, Function<RepositoryTotal, String> text, Comparator<RepositoryTotal> order)
        {
            super(MavenPrimeBundle.message("mavenprime.profile.downloads.column." + key));

            this.key = key;
            this.text = text;
            this.order = order;
        }

        @Override
        public String getTooltipText()
        {
            return MavenPrimeBundle.message("mavenprime.profile.help.downloads." + key);
        }

        @Override
        public String valueOf(RepositoryTotal total)
        {
            return text.apply(total);
        }

        @Override
        public Comparator<RepositoryTotal> getComparator()
        {
            return order;
        }
    }
}
