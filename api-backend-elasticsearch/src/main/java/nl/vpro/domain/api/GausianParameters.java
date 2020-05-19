package nl.vpro.domain.api;

import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
@Getter
@Setter
public abstract class GausianParameters<T, S> {

    T scale;

    T offset;

    double decay = 0.5;

    double factorOffset = 0.5;

    double factorFactor = 0.7;

    final String field;

    public GausianParameters(String field, T scale, T offset) {
        this.field = field;
        this.scale = scale;
        this.offset = offset;
    }
    public abstract Script asScript(S origin);

    public static class Date extends GausianParameters<Duration, Instant> {

        public Date(String field, Duration scale, Duration offset) {
            super(field, scale, offset);
        }

        @Override
        public Script asScript(Instant origin) {
            long scaleLong = scale.toMillis();
            long offsetLong = offset.toMillis();
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("origin", origin.toString());
            params.put("scale", scaleLong + "ms");
            params.put("offset", offsetLong + "ms");
            params.put("decay", decay);
            params.put("factorOffset", factorOffset);
            params.put("factorFactor", factorFactor);
            String value = "doc['" + field + "']";

            Script script = new Script(ScriptType.INLINE, "painless", "params.factorFactor * (" + value +".size() == 0 ? 1 : decayDateGauss(params.origin, params.scale, params.offset, params.decay, " + value + ".value)) + params.factorOffset", params);
            return script;
        }
    }
}
