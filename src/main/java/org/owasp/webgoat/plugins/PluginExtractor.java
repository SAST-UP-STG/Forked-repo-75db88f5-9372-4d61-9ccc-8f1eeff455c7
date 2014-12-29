package org.owasp.webgoat.plugins;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

/**
 * Extract the zip file and place the files in a temp directory
 */
public class PluginExtractor {

    private static final String DIRECTORY = "plugins";
    private final Path pluginArchive;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PluginExtractor(Path pluginArchive) {
        this.pluginArchive = pluginArchive;
    }


    public Plugin extract() {
        final Plugin.Builder pluginBuilder = new Plugin.Builder();
        FileSystem zip = null;
        try {
            zip = createZipFileSystem();
            final Path root = zip.getPath("/");
            final Path tempDirectory = Files.createTempDirectory(DIRECTORY);
            pluginBuilder.setBaseDirectory(tempDirectory);
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().endsWith(".class")) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        Files.copy(file, bos);
                        pluginBuilder.loadClass(file.toString(), bos.toByteArray());
                    }
                    Files.copy(file, tempDirectory, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }
            });
            return pluginBuilder.build();
        } catch (IOException io) {
            logger.error(String.format("Unable to extract: %s", pluginArchive.getFileName()), io);
        } finally {
            closeZipFileSystem(zip);
        }

        return pluginBuilder.build();
    }

    private FileSystem createZipFileSystem() throws IOException {
        final URI uri = URI.create("jar:file:" + pluginArchive.toUri().getPath());
        return FileSystems.newFileSystem(uri, new HashMap<String, Object>());
    }

    private void closeZipFileSystem(FileSystem zip) {
        if (zip != null) {
            try {
                zip.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }


}
