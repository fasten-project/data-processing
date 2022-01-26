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
package eu.f4sten.mavencrawler.utils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;

import org.codehaus.plexus.util.FileUtils;

import eu.f4sten.infra.AssertArgs;
import eu.f4sten.infra.utils.IoUtils;
import eu.f4sten.mavencrawler.MavenCrawlerArgs;

public class RemoteUtils {

    private final MavenCrawlerArgs args;
    private final IoUtils io;

    @Inject
    public RemoteUtils(MavenCrawlerArgs args, IoUtils io) {
        AssertArgs.assertFor(args) //
                .notNull(a -> a.indexUrl, "index url cannot be null") //
                .that(a -> !a.indexUrl.isEmpty(), "index url cannot be empty") //
                .that(a -> a.indexUrl.contains("%d"), "index url must contain marker for index number");
        this.args = args;
        this.io = io;
    }

    public boolean exists(int index) {
        try {
            var url = getUrl(index);
            var huc = (HttpURLConnection) url.openConnection();
            int responseCode = huc.getResponseCode();
            return responseCode == 200;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public File download(int index) {
        try {
            URL from = getUrl(index);
            var fileName = String.format("maven-crawler-%d.tmp", index);
            File to = new File(io.getTempFolder(), fileName);
            FileUtils.copyURLToFile(from, to);
            return to;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private URL getUrl(int index) throws MalformedURLException {
        return new URL(String.format(args.indexUrl, index));
    }
}