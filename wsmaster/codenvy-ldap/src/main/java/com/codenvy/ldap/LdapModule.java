/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.ldap;

import com.codenvy.api.dao.authentication.AuthenticationHandler;
import com.codenvy.ldap.auth.AuthenticatorProvider;
import com.codenvy.ldap.auth.EntryResolverProvider;
import com.codenvy.ldap.auth.LdapAuthenticationHandler;
import com.codenvy.ldap.sync.LdapEntrySelector;
import com.codenvy.ldap.sync.LdapEntrySelectorProvider;
import com.codenvy.ldap.sync.LdapSynchronizer;
import com.codenvy.ldap.sync.LdapSynchronizerService;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;

import org.ldaptive.ConnectionFactory;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.EntryResolver;
import org.ldaptive.pool.PooledConnectionFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Binder for Ldap modules.
 *
 * @author Sergii Kabashniuk
 */
public class LdapModule extends AbstractModule {

    @Override
    protected void configure() {

        Multibinder<AuthenticationHandler> handlerBinder =
                Multibinder.newSetBinder(binder(), com.codenvy.api.dao.authentication.AuthenticationHandler.class);
        handlerBinder.addBinding().to(LdapAuthenticationHandler.class);

        bind(Authenticator.class).toProvider(AuthenticatorProvider.class);
        bind(LdapConfiguration.class);
        bind(ConnectionFactory.class).toProvider(LdapConnectionFactoryProvider.class);
        bind(PooledConnectionFactory.class).toProvider(LdapConnectionFactoryProvider.class);

        bind(EntryResolver.class).toProvider(EntryResolverProvider.class);

        bind(LdapEntrySelector.class).toProvider(LdapEntrySelectorProvider.class);
        bind(LdapSynchronizer.class).asEagerSingleton();
        bind(LdapSynchronizerService.class);

        final Map<String, String> syncProps = new HashMap<>();
        syncProps.put("ldap.sync.initial_delay_ms", "10000");
        syncProps.put("ldap.sync.period_ms", "-1");
        syncProps.put("ldap.sync.page.size", "1000");
        syncProps.put("ldap.sync.page.read_timeout_ms", "30000");
        syncProps.put("ldap.sync.user.additional_dn", null);
        syncProps.put("ldap.sync.user.filter", "(objectClass=inetOrgPerson)");
        syncProps.put("ldap.sync.user.attr.email", "mail");
        syncProps.put("ldap.sync.user.attr.id", "cn");
        syncProps.put("ldap.sync.user.attr.name", "cn");
        syncProps.put("ldap.sync.profile.attrs", "firstName=givenName,phone=telephoneNumber,lastName=sn,employer=o,country=st,jobtitle=title");
        syncProps.put("ldap.sync.group.additional_dn", null);
        syncProps.put("ldap.sync.group.filter", null);
        syncProps.put("ldap.sync.group.attr.members", null);
        for (Map.Entry<String, String> entry : syncProps.entrySet()) {
            if (entry.getValue() == null) {
                bind(String.class).annotatedWith(Names.named(entry.getKey())).toProvider(Providers.of(null));
            } else {
                bindConstant().annotatedWith(Names.named(entry.getKey())).to(entry.getValue());
            }
        }
    }
}
