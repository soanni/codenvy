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
package com.codenvy.ldap.auth;

import com.codenvy.ldap.LdapConfiguration;
import com.google.common.base.Strings;

import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.EntryResolver;
import org.ldaptive.auth.FormatDnResolver;
import org.ldaptive.auth.PooledBindAuthenticationHandler;
import org.ldaptive.auth.PooledCompareAuthenticationHandler;
import org.ldaptive.auth.PooledSearchDnResolver;
import org.ldaptive.control.PasswordPolicyControl;
import org.ldaptive.pool.PooledConnectionFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Create Authenticator based on container configuration.
 */
@Singleton
public class AuthenticatorProvider implements Provider<Authenticator> {

    Authenticator authenticator;

    @Inject
    public AuthenticatorProvider(PooledConnectionFactory connFactory,
                                 EntryResolver entryResolver,
                                 LdapConfiguration configuration) {
        this.authenticator = getAuthenticator(connFactory, entryResolver, configuration);
    }

    @Override
    public Authenticator get() {
        return authenticator;
    }

    private Authenticator getAuthenticator(PooledConnectionFactory connFactory,
                                           EntryResolver entryResolver,
                                           LdapConfiguration configuration) {
        switch (configuration.getType()) {
            case AD:
                return getActiveDirectoryAuthenticator(connFactory, entryResolver, configuration);
            case DIRECT:
                return getDirectBindAuthenticator(connFactory, configuration);
            case SASL:
                return getSaslAuthenticator(connFactory, configuration);
            case ANONYMOUS:
            case AUTHENTICATED:
            default:
                return getAuthenticatedOrAnonSearchAuthenticator(connFactory, entryResolver, configuration);
        }
    }

    private Authenticator getSaslAuthenticator(PooledConnectionFactory connFactory, LdapConfiguration configuration) {
        final PooledSearchDnResolver resolver = new PooledSearchDnResolver();
        resolver.setBaseDn(configuration.getBaseDn());
        resolver.setSubtreeSearch(configuration.isSubtreeSearch());
        resolver.setAllowMultipleDns(configuration.isAllowMultipleDns());
        resolver.setConnectionFactory(connFactory);
        resolver.setUserFilter(configuration.getUserFilter());
        return new Authenticator(resolver, getPooledBindAuthenticationHandler(connFactory));
    }

    private Authenticator getAuthenticatedOrAnonSearchAuthenticator(PooledConnectionFactory connFactory,
                                                                    EntryResolver entryResolver,
                                                                    LdapConfiguration configuration) {
        final PooledSearchDnResolver resolver = new PooledSearchDnResolver();
        resolver.setBaseDn(configuration.getBaseDn());
        resolver.setSubtreeSearch(configuration.isSubtreeSearch());
        resolver.setAllowMultipleDns(configuration.isAllowMultipleDns());
        resolver.setConnectionFactory(connFactory);
        resolver.setUserFilter(configuration.getUserFilter());

        final Authenticator auth;
        if (Strings.isNullOrEmpty(configuration.getUserPasswordAttribute())) {
            auth = new Authenticator(resolver, getPooledBindAuthenticationHandler(connFactory));
        } else {
            auth = new Authenticator(resolver, getPooledCompareAuthenticationHandler(connFactory, configuration));
        }
        auth.setEntryResolver(entryResolver);

        return auth;
    }

    private Authenticator getDirectBindAuthenticator(PooledConnectionFactory connFactory, LdapConfiguration configuration) {
        final FormatDnResolver resolver = new FormatDnResolver(configuration.getBaseDn());
        return new Authenticator(resolver, getPooledBindAuthenticationHandler(connFactory));
    }

    private Authenticator getActiveDirectoryAuthenticator(PooledConnectionFactory connFactory,
                                                          EntryResolver entryResolver,
                                                          LdapConfiguration configuration) {
        final FormatDnResolver resolver = new FormatDnResolver(configuration.getDnFormat());
        final Authenticator authn = new Authenticator(resolver, getPooledBindAuthenticationHandler(connFactory));
        authn.setEntryResolver(entryResolver);
        return authn;
    }

    private PooledBindAuthenticationHandler getPooledBindAuthenticationHandler(PooledConnectionFactory connFactory) {
        final PooledBindAuthenticationHandler handler = new PooledBindAuthenticationHandler(connFactory);
        handler.setAuthenticationControls(new PasswordPolicyControl());
        return handler;
    }

    private PooledCompareAuthenticationHandler getPooledCompareAuthenticationHandler(PooledConnectionFactory connFactory,
                                                                                     LdapConfiguration configuration) {
        final PooledCompareAuthenticationHandler handler = new PooledCompareAuthenticationHandler(
                connFactory);
        handler.setPasswordAttribute(configuration.getUserPasswordAttribute());
        return handler;
    }
}
