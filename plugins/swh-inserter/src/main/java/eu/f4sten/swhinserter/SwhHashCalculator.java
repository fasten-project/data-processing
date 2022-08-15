/*
 * Copyright 2022 Delft University of Technology
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
package eu.f4sten.swhinserter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

public class SwhHashCalculator {

    public String calc(File basePath, String path) {

        if (path.startsWith("/")) {
            throw new IllegalArgumentException("path must be relative, was: " + path);
        }

        var content = read(basePath, path);
        var bytes = content.getBytes(StandardCharsets.UTF_8);
        var hash = computeSwhHash(bytes);
        return hash;
    }

    private String read(File base, String path) {
        try {
            var f = new File(base, path);
            if (!f.exists()) {
                throw new IllegalStateException("File does not exist: " + f.getAbsolutePath());
            }
            return FileUtils.readFileToString(f, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String computeSwhHash(byte[] fileContent) {
        var md = getSha1Digest();
        // The SWH hash is based on Git, which saltes the content with "blob"
        md.update(String.format("blob %d\u0000", fileContent.length).getBytes());
        md.update(fileContent);
        return Hex.encodeHexString(md.digest());
    }

    private static MessageDigest getSha1Digest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}