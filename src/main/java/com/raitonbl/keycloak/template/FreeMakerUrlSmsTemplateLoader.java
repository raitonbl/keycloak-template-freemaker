package com.raitonbl.keycloak.template;

import freemarker.cache.URLTemplateLoader;

import java.net.URL;

public class FreeMakerUrlSmsTemplateLoader extends URLTemplateLoader {

    private final SmsTemplateResourceProvider provider;

    public FreeMakerUrlSmsTemplateLoader(SmsTemplateResourceProvider provider) {
        this.provider = provider;
    }

    @Override
    protected URL getURL(String name) {
        return provider.getTemplate(name);
    }

}
