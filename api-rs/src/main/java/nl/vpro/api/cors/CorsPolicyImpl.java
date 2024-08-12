package nl.vpro.api.cors;

import lombok.extern.log4j.Log4j2;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * @author rico jansen
 */
@Log4j2
public class CorsPolicyImpl implements CorsPolicy {
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
        Properties properties = new Properties();
        try (InputStream in = getStream(policyFile)) {
            properties.load(in);
        } catch (IOException ioe) {
            log.warn(ioe.getMessage());
        }
        return properties;
    }

    private InputStream getStream(String filename) throws FileNotFoundException {
        if (filename.startsWith("classpath:")) {
            return CorsPolicyImpl.class.getResourceAsStream(StringUtils.substringAfter(filename, "classpath:"));
        } else {
            File file = new File(policyFile);
            return new FileInputStream(file);
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
