/*
 * Copyright 2022 Software Improvement Group
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
package eu.f4sten.sourcesprovider.utils;

import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import eu.f4sten.pomanalyzer.data.MavenId;
import eu.f4sten.sourcesprovider.data.SourcePayload;
import jakarta.inject.Inject;

public class PayloadParsing {
    private final SourcesJarProvider sourcesJarProvider;

    @Inject
    public PayloadParsing(SourcesJarProvider sourcesJarProvider) {
        this.sourcesJarProvider = sourcesJarProvider;
    }

    public SourcePayload findSourcePayload(JSONObject json) {
        for (var key : json.keySet()) {
            if (key.equals("payload")) {
                var candidatePayload = parse(json.getJSONObject(key));
                if (candidatePayload != null) {
                    return candidatePayload;
                }
            } else {
                var other = json.get(key);
                if (other instanceof JSONObject) {
                    var otherPayload = findSourcePayload((JSONObject) other);
                    if (otherPayload != null) {
                        return otherPayload;
                    }
                }
            }
        }
        return null;
    }

    public SourcePayload parse(JSONObject payload) {
        SourcePayload result = trySourcePayload(payload);
        if (result == null) {
            result = tryMavenSourcePayload(payload);
        }
        return result;
    }

    private SourcePayload trySourcePayload(JSONObject payload) {
        try {
            return new SourcePayload(payload.getString("forge"), payload.getString("product"), payload.getString("version"), payload.getString("sourcePath"));
        } catch (JSONException e) {
            return null;
        }
    }

    private SourcePayload tryMavenSourcePayload(JSONObject payload) {
        try {
            if (payload.getString("forge").equals("mvn")) {
                var sourcesUrl = new URL(payload.getString("sourcesUrl"));
                var mavenId = new MavenId();
                mavenId.groupId = payload.getString("groupId");
                mavenId.artifactId = payload.getString("artifactId");
                mavenId.version = payload.getString("version");
                var sourcesPath = sourcesJarProvider.downloadSourcesJar(mavenId, sourcesUrl);
                return new SourcePayload("mvn", payload.getString("groupId") + ":" + payload.getString("artifactId"), payload.getString("version"), sourcesPath);
            } else {
                return null;
            }
        } catch (JSONException | MalformedURLException e) {
            return null;
        }
    }
}
