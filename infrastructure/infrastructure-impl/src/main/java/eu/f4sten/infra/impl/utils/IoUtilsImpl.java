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
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.f4sten.infra.json.JsonUtils;
import eu.f4sten.infra.json.TRef;
import eu.f4sten.infra.utils.IoUtils;

public class IoUtilsImpl implements IoUtils {

    private static final Logger LOG = LoggerFactory.getLogger(IoUtilsImpl.class);

    private File baseDir;
    private JsonUtils jsonUtils;

    public IoUtilsImpl(File baseDir, JsonUtils jsonUtils) {
        this.baseDir = baseDir;
        this.jsonUtils = jsonUtils;
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