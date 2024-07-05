package pl.ynfuien.yupdatechecker.core;

import java.util.List;

public record CheckResult(List<ProjectCheckResult> plugins, int allPluginsCount, List<ProjectCheckResult> dataPacks, int allDataPacksCount, Times times, int requestsSent) {
    // Plugins
    public int upToDatePluginsCount() {
        int count = 0;
        for (ProjectCheckResult result : plugins) if (result.upToDate()) count++;
        return count;
    }

    public int outdatedPluginsCount() {
        return plugins.size() - upToDatePluginsCount();
    }

    // Datapacks
    public int upToDateDataPacksCount() {
        int count = 0;
        for (ProjectCheckResult result : dataPacks) if (result.upToDate()) count++;
        return count;
    }

    public int outdatedDataPacksCount() {
        return dataPacks.size() - upToDateDataPacksCount();
    }

    // Times
    public record Times(long start, long end) {
        public long duration() {
            return end - start;
        }
    }
}