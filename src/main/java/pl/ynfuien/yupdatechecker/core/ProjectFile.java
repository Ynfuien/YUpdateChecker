package pl.ynfuien.yupdatechecker.core;

import java.io.File;

public record ProjectFile(File file, Type type) {
    public enum Type {
        PLUGIN,
        DATAPACK
    }
}
