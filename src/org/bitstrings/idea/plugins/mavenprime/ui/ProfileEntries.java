package org.bitstrings.idea.plugins.mavenprime.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public final class ProfileEntries
{
    private ProfileEntries()
    {
    }

    public static List<ProfileEntry> merge(Collection<String> available, Map<String, Boolean> selected)
    {
        Set<String> merged = new LinkedHashSet<>(available);

        merged.addAll(selected.keySet());

        return merged
            .stream()
            .sorted(String::compareToIgnoreCase)
            .map(name -> new ProfileEntry(name, ProfileState.of(selected.get(name))))
            .collect(Collectors.toCollection(ArrayList::new));
    }

    public static ProfileEntry in(Object node)
    {
        Object row =
            (node instanceof DefaultMutableTreeNode) ? ((DefaultMutableTreeNode) node).getUserObject() : null;

        return (row instanceof ProfileEntry) ? (ProfileEntry) row : null;
    }

    public static ProfileEntry at(TreePath path)
    {
        return (path == null) ? null : in(path.getLastPathComponent());
    }

    public static Map<String, Boolean> selectedIn(DefaultMutableTreeNode root)
    {
        Map<String, Boolean> selected = new LinkedHashMap<>();

        for (int index = 0; index < root.getChildCount(); index++)
        {
            ProfileEntry entry = in(root.getChildAt(index));

            Boolean enabled = (entry == null) ? null : entry.getState().toEnabled();

            if (enabled != null)
            {
                selected.put(entry.getName(), enabled);
            }
        }

        return selected;
    }
}
