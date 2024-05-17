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
 * We use a small variant of the decayDateGauss function of elasticsearch to boost down older search results.
 * <p>
 * decayDateGauss is something like exp(-0.5*pow(valueExpl,2.0) + -1 * scale)
 * <p>
 * We add to new parameters 'gaussFactor' and 'gaussOffset':
 * <p>
 * gaussOffset + gaussFactor * exp(-0.5*pow(valueExpl,2.0) + -1 * scale)
 * <p>
 * Like this we can ensure that we never boost things entirely to zero.
 *
 * @author Michiel Meeuwissen
 * @since 5.12
 */
@Getter
@Setter
public abstract class GaussianParameters<T, S> {

    T scale;

    T offset;

    double decay = 0.5;

    double gaussOffset = 0.5;

    double gaussFactor = 0.7;

    final String field;

    protected GaussianParameters(String field, T scale, T offset) {
        this.field = field;
        this.scale = scale;
        this.offset = offset;
    }
    public abstract Script asScript(S origin);

    public static class Date extends GaussianParameters<Duration, Instant> {

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
            params.put("gaussOffset", gaussOffset);
            params.put("gaussFactor", gaussFactor);
            String value = "doc['" + field + "']";

            Script script = new Script(ScriptType.INLINE, "painless", "params.gaussFactor * (" + value +".size() == 0 ? 1 : decayDateGauss(params.origin, params.scale, params.offset, params.decay, " + value + ".value)) + params.gaussOffset", params);
            return script;
        }
    }
}
