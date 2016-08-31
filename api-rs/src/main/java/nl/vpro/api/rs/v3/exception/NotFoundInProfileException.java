package nl.vpro.api.rs.v3.exception;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;

import nl.vpro.domain.constraint.PredicateTestResult;
import nl.vpro.i18n.Locales;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
public class NotFoundInProfileException extends NotFoundException  {

    private PredicateTestResult<?> testResult;
    public NotFoundInProfileException(PredicateTestResult<?> testResult) {
        super(Arrays.stream(testResult.getReasonDescription(Locales.DUTCH)).collect(Collectors.joining()));
        this.testResult = testResult;
    }
    public PredicateTestResult<?> getTestResult() {
        return testResult;
    }
}
