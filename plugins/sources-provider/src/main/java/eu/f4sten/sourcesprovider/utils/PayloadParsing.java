package eu.f4sten.sourcesprovider.utils;

import eu.f4sten.sourcesprovider.data.MavenSourcePayload;
import eu.f4sten.sourcesprovider.data.SourcePayload;
import org.json.JSONException;
import org.json.JSONObject;

public class PayloadParsing {
    public static SourcePayload findSourcePayload(JSONObject json) {
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
                    if(otherPayload != null) {
                        return otherPayload;
                    }
                }
            }
        }
        return null;
    }

    static SourcePayload parse(JSONObject payload) {
        SourcePayload result = trySourcePayload(payload);
        if(result == null) {
            result = tryMavenSourcePayload(payload);
        }
        return result;
    }

    private static SourcePayload trySourcePayload(JSONObject payload) {
        try {
            return new SourcePayload(payload.getString("forge"),
                    payload.getString("product"),
                    payload.getString("version"),
                    payload.getString("sourcePath"));
        } catch (JSONException e) {
            return null;
        }
    }

    private static SourcePayload tryMavenSourcePayload(JSONObject payload) {
        try {
            return new MavenSourcePayload(payload.getString("forge"),
                    payload.getString("groupId"),
                    payload.getString("artifactId"),
                    payload.getString("version"),
                    payload.getString("sourcesUrl"));
        } catch (JSONException e) {
            return null;
        }
    }
}
