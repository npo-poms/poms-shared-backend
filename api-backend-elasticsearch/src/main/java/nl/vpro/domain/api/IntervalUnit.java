package nl.vpro.domain.api;

/**
 * @author Michiel Meeuwissen
 * @since ...
 */
public enum IntervalUnit {

    YEAR,
    MONTH,
    WEEK {
        @Override
        public String getShortEs() {
            return "w";
        }
    },
    DAY {
        @Override
        public String getShortEs() {
            return "d";
        }
    },
    HOUR {
        @Override
        public String getShortEs() {
            return "h";
        }
    },
    MINUTE {
        @Override
        public String getShortEs() {
            return "m";
        }
    };

    public String getShortEs() {
        throw new UnsupportedOperationException("No multiples available for  " + this);
    }

}
