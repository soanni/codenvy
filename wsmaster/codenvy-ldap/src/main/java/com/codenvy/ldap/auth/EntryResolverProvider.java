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

import org.ldaptive.auth.EntryResolver;
import org.ldaptive.auth.PooledSearchEntryResolver;
import org.ldaptive.pool.PooledConnectionFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Created by sj on 09.09.16.
 */
public class EntryResolverProvider implements Provider<EntryResolver> {

    private final PooledSearchEntryResolver entryResolver;

    @Inject
    public EntryResolverProvider(PooledConnectionFactory connFactory, LdapConfiguration configuration) {
        this.entryResolver = new PooledSearchEntryResolver();
        this.entryResolver.setBaseDn(configuration.getBaseDn());
        this.entryResolver.setUserFilter(configuration.getUserFilter());
        this.entryResolver.setSubtreeSearch(configuration.isSubtreeSearch());
        this.entryResolver.setConnectionFactory(connFactory);

    }

    @Override
    public EntryResolver get() {
        return entryResolver;
    }
}
