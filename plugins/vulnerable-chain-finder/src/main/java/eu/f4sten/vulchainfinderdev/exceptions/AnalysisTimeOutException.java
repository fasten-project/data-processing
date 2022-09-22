package eu.f4sten.vulchainfinderdev.exceptions;

/**
 * This exception is raised when Vuln-Chain-Finder fails to process a pkg. version within a specified time.
 */
public class AnalysisTimeOutException extends RuntimeException {
    public AnalysisTimeOutException(String errorMsg) {
        super(errorMsg);
    }

}
