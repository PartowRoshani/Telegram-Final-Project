package org.to.telegramfinalproject.UI;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class MediaPathResolver {
    private final Path uploadsRoot;

    public MediaPathResolver(Path uploadsRoot) {
        this.uploadsRoot = uploadsRoot;
    }

    public String toFileUri(String raw) {
        if (raw == null || raw.isBlank()) return null;
        if (raw.startsWith("file:/")) return raw;

        Path p;
        if (raw.startsWith("/")) {
            String sub = raw.substring(1).replace("/", File.separator);
            p = uploadsRoot.resolve(sub).normalize();
        } else {
            p = Paths.get(raw).normalize();
        }
        return p.toUri().toString();
    }
}
