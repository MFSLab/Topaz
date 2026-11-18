package com.selekode.topaz.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AboutService {
    @Value("${app.data.dir}")
    private String appDataDirectory;

    @Value("${app.jcef.dir}")
    private String jcefBundleDirectory;

    public String getDatabasePath() {
        return appDataDirectory;
    }

    public String getJcefBundlePath() {
        return jcefBundleDirectory;
    }
}