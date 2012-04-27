package nl.vpro.api.cors;

import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: ricojansen
 * Date: 27-04-2012
 * Time: 15:16
 * To change this template use File | Settings | File Templates.
 */
public class CorsPolicyImpl implements CorsPolicy {

    String policyFile;

    boolean enabled;

    @Override
    public boolean allowedOrigin(String origin) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean allowedOriginAndMethod(String origin, String method) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    private Properties getPolicy() {
        InputStream in=null;
        Properties properties;
        try {
            if (policyFile.startsWith("classpath:")) {
                in=getClass().getResourceAsStream(StringUtils.substringAfter(policyFile, "classpath:"));
            } else {
                File file=new File(policyFile);
                in=new FileInputStream(file);
            }
        } catch (IOException ioe) {

        } finally {
            try {
                if (in!=null) {
                    in.close();
                }
            } catch (IOException ieo) {

            }
        }
        return null;
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
