package eu.f4sten.sourcesprovider.utils;

import com.google.inject.Inject;
import eu.f4sten.infra.utils.IoUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

public class SourcesDownloader {

    private final IoUtils io;

    @Inject
    public SourcesDownloader(IoUtils io) {
        this.io = io;
    }

    public File getFromUrl(URL url) throws IOException {
        var fileName = Path.of(url.getPath()).getFileName();
        var tempFile = new File(io.getTempFolder(), String.format("sources-provider-download-%s", fileName));
        IOUtils.copy(url, tempFile);
        return tempFile;
    }
}
