package eu.f4sten.sourcesprovider.data;

import eu.fasten.core.data.Constants;

public class MavenSourcePayload extends SourcePayload {
    private String groupId;
    private String artifactId;
    private String sourcesUrl;

    public MavenSourcePayload(String forge, String groupId, String artifactId, String version, String sourcesUrl) {
        super(forge, groupId + Constants.mvnCoordinateSeparator + artifactId, version, null);
        setGroupId(groupId);
        setArtifactId(artifactId);
        setSourcesUrl(sourcesUrl);
        // TODO: downloading of sources Jar and setting the sourcePath field
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getSourcesUrl() {
        return sourcesUrl;
    }

    public void setSourcesUrl(String sourcesUrl) {
        this.sourcesUrl = sourcesUrl;
    }
}
