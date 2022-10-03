package eu.f4sten.vulchainfinderdev.data;

import eu.f4sten.pomanalyzer.data.MavenId;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import static org.apache.commons.lang3.builder.ToStringStyle.MULTI_LINE_STYLE;

public class VcfPayload {

    private String groupId;
    private String artifactId;
    private String version;
    private String packagingType;
    private String vulCallChainRepoPath;
    private int numFoundVulCallChains;

    public VcfPayload() {
        // For object mappers
    }

    public VcfPayload(String groupId, String artifactId, String version, String packagingType,
                      String vulCallChainRepoPath, int numFoundVulCallChains) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packagingType = packagingType;
        this.vulCallChainRepoPath = vulCallChainRepoPath;
        this.numFoundVulCallChains = numFoundVulCallChains;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setPackagingType(String packagingType) {
        this.packagingType = packagingType;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getPackagingType() {
        return packagingType;
    }

    public String getVulCallChainRepoPath() {
        return vulCallChainRepoPath;
    }

    public void setVulCallChainRepoPath(String vulCallChainRepoPath) {
        this.vulCallChainRepoPath = vulCallChainRepoPath;
    }

    public int getNumFoundVulCallChains() {
        return numFoundVulCallChains;
    }

    public void setNumFoundVulCallChains(int numFoundVulCallChains) {
        this.numFoundVulCallChains = numFoundVulCallChains;
    }

    public MavenId toMavenId() {
        return new MavenId(this.groupId, this.artifactId, this.version, null, this.packagingType);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, MULTI_LINE_STYLE);
    }

}
