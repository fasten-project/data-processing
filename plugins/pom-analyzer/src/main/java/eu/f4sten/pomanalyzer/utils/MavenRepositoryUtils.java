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
package eu.f4sten.pomanalyzer.utils;

import static java.util.Locale.ENGLISH;
import static org.apache.http.HttpStatus.SC_MOVED_PERMANENTLY;
import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.http.HttpStatus.SC_TEMPORARY_REDIRECT;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.apache.http.HttpStatus;
import org.apache.maven.settings.Settings;
import org.jboss.shrinkwrap.resolver.impl.maven.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.c0ps.commons.Asserts;
import dev.c0ps.maven.data.Pom;

public class MavenRepositoryUtils {

    private static final Logger LOG = LoggerFactory.getLogger(MavenRepositoryUtils.class);
    private static final String[] ALLOWED_CLASSIFIERS = new String[] { null, "sources" };

    /**
     * returns url of sources jar if it exists, null otherwise
     */
    public String getSourceUrlIfExisting(Pom r) {

        var url = getUrl(r, "sources");
        return checkGetRequest(url).url;
    }

    public long getReleaseDate(Pom r) {

        var url = getUrl(r, null);
        var lm = checkGetRequest(url).lastModified;
        return lm != null ? lm.getTime() : -1;
    }

    private static String getUrl(Pom r, String classifier) {
        if (isNullEmptyOrUnset(r.artifactRepository) || isNullEmptyOrUnset(r.groupId) || isNullEmptyOrUnset(r.artifactId) || isNullEmptyOrUnset(r.packagingType) || isNullEmptyOrUnset(r.version)) {
            throw new IllegalArgumentException("cannot build sources URL with missing package information");
        }
        Asserts.assertContains(ALLOWED_CLASSIFIERS, classifier);
        var classifierStr = classifier != null ? "-" + classifier : "";
        var ar = r.artifactRepository;
        if (!ar.endsWith("/")) {
            ar += "/";
        }
        var url = ar + r.groupId.replace('.', '/') + "/" + r.artifactId + "/" + r.version + "/" + r.artifactId + "-" + r.version + classifierStr + "." + r.packagingType;
        return url;
    }

    public static UrlCheck checkGetRequest(String url) {
        try {
            var httpClient = HttpClient.newBuilder() //
                    .version(HttpClient.Version.HTTP_2) //
                    .connectTimeout(Duration.ofSeconds(10)) //
                    .followRedirects(Redirect.NEVER) //
                    .build();
            var request = HttpRequest.newBuilder().GET().uri(URI.create(url)).build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            var statusCode = response.statusCode();
            var lastModified = getDateOrNull(response, "last-modified", "Last-Modified");

            if (statusCode == HttpStatus.SC_OK) {
                return new UrlCheck(url, lastModified);
            }

            for (var scMoved : Set.of(SC_MOVED_TEMPORARILY, SC_MOVED_PERMANENTLY, SC_TEMPORARY_REDIRECT)) {
                if (statusCode == scMoved) {
                    var newLocation = getField(response, "Location", "location");
                    if (newLocation.isPresent()) {
                        return checkGetRequest(newLocation.get());
                    }
                }
            }

            return new UrlCheck(null, null);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<String> getField(HttpResponse<Void> response, String... keys) {
        for (String key : keys) {
            var val = response.headers().firstValue(key);
            if (val.isPresent()) {
                return val;
            }
        }
        return Optional.empty();
    }

    private static Date getDateOrNull(HttpResponse<Void> response, String... keys) {
        var headers = response.headers();
        for (var key : keys) {
            var val = headers.firstValue(key);
            if (val.isPresent()) {
                try {
                    var pattern = "E, d MMM yyyy HH:mm:ss Z";
                    var lastModified = new SimpleDateFormat(pattern, ENGLISH).parse(val.get());
                    return lastModified;
                } catch (ParseException e) {
                    LOG.warn("Could not parse release date: {}\n", val.get());
                }
            }
        }
        return null;
    }

    private static boolean isNullEmptyOrUnset(String s) {
        return s == null || s.isEmpty() || "?".equals(s);
    }

    public static File getPathOfLocalRepository() {
        // By default, this is set to ~/.m2/repository/, but that can be re-configured
        // or even provided as a parameter. As such, we are reusing an existing library
        // to find the right folder.
        var settings = new SettingsManager() {
            @Override
            public Settings getSettings() {
                return super.getSettings();
            }
        }.getSettings();
        var localRepository = settings.getLocalRepository();
        return new File(localRepository);
    }

    public boolean doesExist(Pom r) {
        var url = getUrl(r, null);
        return checkGetRequest(url).url != null;
    }

    public static class UrlCheck {
        public final String url;
        public final Date lastModified;

        public UrlCheck(String url, Date lastModified) {
            this.url = url;
            this.lastModified = lastModified;
        }
    }
}