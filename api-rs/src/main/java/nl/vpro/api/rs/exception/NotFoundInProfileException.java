package nl.vpro.api.rs.exception;

import javax.ws.rs.NotFoundException;

import nl.vpro.domain.constraint.PredicateTestResult;
import nl.vpro.i18n.Locales;

/**
 * @author Michiel Meeuwissen
 * @since 4.8
 */
public class NotFoundInProfileException extends NotFoundException  {

    private PredicateTestResult testResult;
    public NotFoundInProfileException(PredicateTestResult testResult) {
        super(testResult.getDescription(Locales.DUTCH));
        this.testResult = testResult;
    }
    public PredicateTestResult getTestResult() {
        return testResult;
    }
}
