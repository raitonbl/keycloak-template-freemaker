package com.raitonbl.keycloak.template;

import com.raitonbl.keycloak.channel.SmsChannel;
import com.raitonbl.keycloak.channel.SmsChannelException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.beans.MessageFormatterMethod;

import java.io.StringWriter;
import java.io.Writer;
import java.util.*;

public class FreeMakerSmsTemplateProvider implements SmsTemplateProvider, ServerInfoAwareProviderFactory {

    public static final String IDENTITY_PROVIDER_BROKER_CONTEXT = "identityProviderBrokerCtx";
    public static final String TEMPLATE_PROVIDER_ID_PROPERTY = "com.raitonbl.keycloak.template.sms.provider._id";

    private UserModel user;
    private final KeycloakSession session;
    protected final Map<String, Object> attributes = new HashMap<>();

    public FreeMakerSmsTemplateProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public SmsTemplateProvider setUser(UserModel user) {
        this.user = user;
        return this;
    }

    @Override
    public SmsTemplateProvider setRealm(RealmModel realm) {
        return this;
    }

    @Override
    public SmsTemplateProvider setAttribute(String name, Object value) {
        return this;
    }

    @Override
    public SmsTemplateProvider setAuthenticationSession(AuthenticationSessionModel authenticationSession) {
        return this;
    }

    @Override
    public void send(String template, Map<String, Object> attr) {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.putAll(attr);
        attributes.put("user", user);
        addAdditionalInformation(null, null, attributes);
        processAndSend(template, attributes);
    }

    @Override
    public void sendPasswordReset(String code, long expirationInMinutes) {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("fullName", user.getFirstName() + " " + user.getLastName());
        addAdditionalInformation(code, expirationInMinutes, attributes);
        prepareAndSend("reset-password", attributes);
    }

    @Override
    public void sendExecuteActions(String code, long expirationInMinutes) {
        Map<String, Object> attributes = new HashMap<>(this.attributes);
        attributes.put("user", user);
        addAdditionalInformation(code, expirationInMinutes, attributes);
        prepareAndSend("execute-actions", attributes);
    }

    @Override
    public void sendConfirmIdentityBrokerLink(String code, long expirationInMinutes) {
        Map<String, Object> attributes = new HashMap<>(this.attributes);

        attributes.put("fullName", user.getFirstName() + " " + user.getLastName());
        addAdditionalInformation(code, expirationInMinutes, attributes);

        BrokeredIdentityContext brokerContext = (BrokeredIdentityContext) this.attributes.get(IDENTITY_PROVIDER_BROKER_CONTEXT);
        String idpAlias = brokerContext.getIdpConfig().getAlias();
        idpAlias = ObjectUtil.capitalize(idpAlias);

        // attributes.put("identityProviderContext", brokerContext);
        attributes.put("identityProviderAlias", idpAlias);

        prepareAndSend("identity-link", attributes);
    }

    private void send(String text) {

        if (user == null) {
            throw new SmsTemplateException("Sms cannot be sent since user doesn't contain a phone number");
        }

        String phone = user.getFirstAttribute("phone");

        if (phone == null) {
            throw new SmsTemplateException("Sms cannot be sent since user doesn't contain a phone number");
        }

        if (session == null) {
            throw new SmsTemplateException("Sms cannot be sent since session is null");
        }

        SmsChannel channel = session.getProvider(SmsChannel.class);

        if (channel == null) {
            throw new SmsTemplateException("Sms cannot be sent since channel is null");
        }

        channel.send(phone, text);
    }

    /**
     * Converts text and attributes into a message to be sent
     *
     * @param text       - Template as a text
     * @param attributes - Attributes to be processed
     */
    private void processAndSend(String text, Map<String, Object> attributes) {
        try {
            Configuration configuration = new Configuration(new Version(2, 3, 19));

            configuration.setDefaultEncoding("UTF-8");
            configuration.setLocale(session.getContext().resolveLocale(user));
            configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

            Template tmp = new Template(UUID.randomUUID().toString(), text, configuration);

            Writer out = new StringWriter();

            tmp.process(attributes, out);

            send(out.toString());
        } catch (SmsTemplateException | SmsChannelException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SmsTemplateException(ex);
        }
    }

    /**
     * Builds the template and sends the message
     *
     * @param name       - Template name which needs to be processed
     * @param attributes - Attributes to be processed
     */
    private void prepareAndSend(String name, Map<String, Object> attributes) {
        try {

            Locale locale = session.getContext().resolveLocale(user);

            String providerID = Optional.ofNullable(session.getContext().getRealm().getAttribute(TEMPLATE_PROVIDER_ID_PROPERTY))
                    .orElse("default");

            SmsTemplateResourceProvider provider = session.getProvider(SmsTemplateResourceProvider.class, providerID);

            if (provider == null) {
                throw new IllegalStateException("Cannot find template resource provider");
            }

            Properties properties = provider.getMessages(name, locale);

            attributes.put("locale", locale);
            attributes.put("msg", new MessageFormatterMethod(locale, properties));

            Configuration configuration = new Configuration(new Version(2, 3, 19));

            configuration.setLocale(locale);
            configuration.setDefaultEncoding("UTF-8");
            configuration.setTemplateLoader(new FreeMakerUrlSmsTemplateLoader(provider));
            configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

            Template tmp = configuration.getTemplate(name.concat(".ftl"));

            Writer out = new StringWriter();

            tmp.process(attributes, out);

            send(out.toString());
        } catch (SmsTemplateException | SmsChannelException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new SmsTemplateException(ex);
        }
    }

    private void addAdditionalInformation(String code, Long expirationInMinutes, Map<String, Object> attributes) {

        if (code != null) {
            attributes.put("oneTimePin", code);
        }

        if (expirationInMinutes != null) {
            attributes.put("durationInMinutes", expirationInMinutes);
        }

    }

    @Override
    public void close() {
        // DO NOTHING
    }

    @Override
    public Map<String, String> getOperationalInfo() {
        return Collections.singletonMap("name", "Default OTP Service Factory");
    }

}
