/*
 * Copyright 2021 Delft University of Technology
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.f4sten.infra.impl.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.utils.IoUtils;

public class IoUtilsImpl implements IoUtils {

    private static final Logger LOG = LoggerFactory.getLogger(IoUtilsImpl.class);

    private final File baseDir;
    private final JsonUtils jsonUtils;
    private final ObjectMapper om;

    public IoUtilsImpl(File baseDir, JsonUtils jsonUtils, ObjectMapper om) {
        this.baseDir = baseDir;
        this.jsonUtils = jsonUtils;
        this.om = om;
    }

    @Override
    public File getTempFolder() {
        String defaultBaseDir = System.getProperty("java.io.tmpdir");
        return new File(defaultBaseDir);
    }

    @Override
    public File getBaseFolder() {
        return baseDir;
    }

    @Override
    public <T> void writeToFile(T t, File file) {
        try {
            if (file.exists()) {
                LOG.info("Overriding existing file: {}", file);
            }

            String json = jsonUtils.toJson(t);
            FileUtils.write(file, json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> void writeToZip(T t, File file) {
        try (//
                var fos = new FileOutputStream(file); //
                var zos = new ZipOutputStream(fos)) {

            var name = file.getName();
            if (name.endsWith(".zip")) {
                name = name.substring(0, name.length() - 4);
            }
            name = name + ".json";

            zos.putNextEntry(new ZipEntry(name));
            om.writeValue(zos, t);
            zos.closeEntry();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T readFromFile(File file, Class<T> typeOfContent) {
        try {
            String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return jsonUtils.fromJson(json, typeOfContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T readFromFile(File file, TRef<T> typeOfContent) {
        try {
            String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            return jsonUtils.fromJson(json, typeOfContent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T readFromZip(File file, Class<T> type) {
        return readFromZip(file, in -> {
            try {
                return om.readValue(in, type);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public <T> T readFromZip(File file, TRef<T> typeRef) {
        return readFromZip(file, in -> {
            try {
                return om.readValue(in, typeRef);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private <T> T readFromZip(File file, Function<InputStream, T> c) {
        try (var zf = new ZipFile(file)) {
            var entries = zf.entries();
            var next = entries.nextElement();
            var in = zf.getInputStream(next);
            var v = c.apply(in);
            in.close();
            if (entries.hasMoreElements()) {
                LOG.warn("Only the first entry of .zip file is read, all other entries will be ignored: {}", file);
            }
            return v;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void move(File from, File to) {
        try {
            if (to.exists()) {
                LOG.info("Replacing existing file: {}", to);
                to.delete();
            }
            FileUtils.moveFile(from, to);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}