package nl.vpro.api.cors;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: ricojansen
 * Date: 27-04-2012
 * Time: 15:16
 * To change this template use File | Settings | File Templates.
 */
public class CorsPolicyImpl implements CorsPolicy {

    private String policyFile;
    private boolean enabled;

    private Map<String, Pattern> policyTable = null;

    @Override
    public boolean allowedOrigin(String origin) {
        return allowedOriginAndMethod(origin, "GET");
    }

    @Override
    public boolean allowedOriginAndMethod(String origin, String method) {
        if (StringUtils.isNotEmpty(method) && StringUtils.isNotEmpty(origin)) {
            Pattern pattern = getPolicyTable().get(method);
            if (pattern != null) {
                return pattern.matcher(origin).matches();
            }
        }
        return false;
    }

    private Map<String, Pattern> getPolicyTable() {
        if (policyTable == null) {
            synchronized (this) {
                if (policyTable == null) {
                    policyTable = parseProperties();
                }
            }
        }
        return policyTable;
    }


    private Map<String, Pattern> parseProperties() {
        Map<String, Pattern> table = new HashMap<String, Pattern>();
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
        Properties properties=new Properties();
        String filename = getPolicyFile();
        try {
            if (filename.startsWith("classpath:")) {
                in = getClass().getResourceAsStream(StringUtils.substringAfter(filename, "classpath:"));
            } else {
                File file = new File(policyFile);
                in = new FileInputStream(file);
            }
            properties.load(in);
        } catch (IOException ioe) {

        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ieo) {

            }
        }
        return properties;
    }

    public String getPolicyFile() {
        return policyFile;
    }

    public void setPolicyFile(String policyFile) {
        this.policyFile = policyFile;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
