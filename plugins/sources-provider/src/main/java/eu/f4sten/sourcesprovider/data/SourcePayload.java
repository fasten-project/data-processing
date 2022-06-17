package eu.f4sten.sourcesprovider.data;

public class SourcePayload {
    private String forge;
    private String product;
    private String version;
    private String sourcePath;

    public SourcePayload(String forge, String product, String version, String sourcePath) {
        setForge(forge);
        setProduct(product);
        setVersion(version);
        setSourcePath(sourcePath);
    }

    public String getForge() {
        return forge;
    }

    public void setForge(String forge) {
        this.forge = forge;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }
}
