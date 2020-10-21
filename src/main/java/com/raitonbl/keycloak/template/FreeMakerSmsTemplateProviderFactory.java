package com.raitonbl.keycloak.template;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class FreeMakerSmsTemplateProviderFactory implements SmsTemplateProviderFactory {

    @Override
    public SmsTemplateProvider create(KeycloakSession session) {
        return new FreeMakerSmsTemplateProvider(session);
    }

    @Override
    public void init(Config.Scope config) {
        // DO NOTHING
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // DO NOTHING
    }

    @Override
    public String getId() {
        return "freemaker";
    }

    @Override
    public void close() {
        // DO NOTHING
    }

}
