package eu.f4sten.vulchainfinderdev.exceptions;

/**
 * This exception is raised when OPAL fails to generate a CG within a specified time.
 */
public class CGConstructionTimeOut extends RuntimeException {
    public CGConstructionTimeOut(String errorMsg) {
        super(errorMsg);
    }

}
