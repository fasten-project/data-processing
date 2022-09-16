package eu.f4sten.vulchainfinderdev.exceptions;

public class VulnChainRepoSizeLimitException extends RuntimeException {
    public VulnChainRepoSizeLimitException(String errorMsg) {
        super(errorMsg);
    }
}
