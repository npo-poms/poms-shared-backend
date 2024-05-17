/*
 * Copyright (C) 2015 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.domain.api;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.monitor.*;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * @author Roelof Jan Koekoek
 * @since 4.2
 */
@Getter
@Log4j2
public abstract class AbstractConfigFileScoreManager implements ScoreManager {

    protected static final String TEXT_BOOST_PREFIX = "boost.text.";

    protected abstract String getConfigDir();

    protected abstract String getConfigFileName();

    protected abstract void loadScores();

    @Setter
    private boolean score = true;

    protected Map<String, String> loadConfig() {
        Properties properties = new Properties();
        Path configPath = Paths.get(getConfigDir(), getConfigFileName());
        final File configFile = configPath.toFile();

        if(configFile.canRead()) {
            log.info("Loading properties from {}", configFile);
            try {
                properties.load(Files.newBufferedReader(configPath, StandardCharsets.UTF_8));
            } catch(IOException ioe) {
                throw new RuntimeException("Can't load configuration file " + getConfigFileName() + " from " + getConfigDir(), ioe);
            }
        } else {
            log.info("The file {} cannot be read", configFile);
        }
        return (Map) properties;
    }

    protected void watchConfig() throws Exception {
        FileAlterationListenerAdaptor listener = new FileAlterationListenerAdaptor() {
            @Override
            public void onFileChange(File file) {
                loadScores();
            }

            @Override
            public void onFileCreate(File file) {
                loadScores();
            }
        };

        FileAlterationObserver observer = new FileAlterationObserver(getConfigDir(), pathname -> getConfigFileName().equals(pathname.getName()));
        observer.addListener(listener);

        FileAlterationMonitor monitor = new FileAlterationMonitor(10);
        monitor.addObserver(observer);
        monitor.start();
    }

    @Override
    @ManagedOperation
    public boolean getIsScoring() {
        return score;
    }
    @ManagedOperation
    @Override
    public void setIsScoring(boolean score) {
        this.score = score;
    }


}
