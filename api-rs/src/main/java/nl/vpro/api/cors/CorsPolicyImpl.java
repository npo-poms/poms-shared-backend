package nl.vpro.api.cors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rico jansen
 */
public class CorsPolicyImpl implements CorsPolicy {
    private static final Logger LOG = LoggerFactory.getLogger(CorsPolicyImpl.class);
    private final String policyFile;
    private final boolean enabled;

    private final Map<String, Pattern> policyTable;

    public CorsPolicyImpl(boolean enabled, String policyFile) {
        this.policyFile = policyFile;
        this.enabled = enabled;
        policyTable = parseProperties();
    }


    @Override
    public boolean allowedOriginAndMethod(String origin, String method) {
        if (StringUtils.isNotEmpty(method) && StringUtils.isNotEmpty(origin)) {
            Pattern pattern = policyTable.get(method);
            if (pattern != null) {
                return pattern.matcher(origin).matches();
            }
        }
        return false;
    }


    private Map<String, Pattern> parseProperties() {
        Map<String, Pattern> table = new HashMap<>();
        Properties properties = getPolicyProperties();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            Pattern pattern = Pattern.compile(value);
            table.put(key, pattern);
        }
        return table;
    }

    private Properties getPolicyProperties() {
        InputStream in = null;
        Properties properties = new Properties();
        String filename = policyFile;
        try {
            if (filename.startsWith("classpath:")) {
                in = CorsPolicyImpl.class.getResourceAsStream(StringUtils.substringAfter(filename, "classpath:"));
            } else {
                File file = new File(policyFile);
                in = new FileInputStream(file);
            }
            properties.load(in);
        } catch (IOException ioe) {
            LOG.warn(ioe.getMessage());
        } finally {
            IOUtils.closeQuietly(in);
        }
        return properties;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
