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

import com.google.common.base.Strings;

import org.ldaptive.BindConnectionInitializer;
import org.ldaptive.ConnectionConfig;
import org.ldaptive.Credential;
import org.ldaptive.DefaultConnectionFactory;
import org.ldaptive.ad.extended.FastBindOperation;
import org.ldaptive.pool.BlockingConnectionPool;
import org.ldaptive.pool.IdlePruneStrategy;
import org.ldaptive.pool.PoolConfig;
import org.ldaptive.pool.PooledConnectionFactory;
import org.ldaptive.pool.SearchValidator;
import org.ldaptive.sasl.CramMd5Config;
import org.ldaptive.sasl.DigestMd5Config;
import org.ldaptive.sasl.ExternalConfig;
import org.ldaptive.sasl.GssApiConfig;
import org.ldaptive.sasl.SaslConfig;
import org.ldaptive.ssl.KeyStoreCredentialConfig;
import org.ldaptive.ssl.SslConfig;
import org.ldaptive.ssl.X509CredentialConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Duration;

@Singleton
public class LdapConnectionFactoryProvider implements Provider<PooledConnectionFactory> {
    private static final Logger LOG = LoggerFactory.getLogger(LdapConnectionFactoryProvider.class);

    private final PooledConnectionFactory connFactory;

    @Inject
    public LdapConnectionFactoryProvider(LdapConfiguration configuration) {
        final PoolConfig pc = new PoolConfig();
        pc.setMinPoolSize(configuration.getMinPoolSize());
        pc.setMaxPoolSize(configuration.getMaxPoolSize());
        pc.setValidateOnCheckOut(configuration.isValidateOnCheckout());
        pc.setValidateOnCheckIn(configuration.isValidateOnCheckin());
        pc.setValidatePeriodically(configuration.isValidatePeriodically());
        pc.setValidatePeriod(Duration.ofMillis(configuration.getValidatePeriod()));

        final ConnectionConfig cc = new ConnectionConfig();
        cc.setLdapUrl(configuration.getLdapUrl());
        cc.setUseSSL(configuration.isUseSsl());
        cc.setUseStartTLS(configuration.isUseStartTls());
        cc.setConnectTimeout(Duration.ofMillis(configuration.getConnectTimeout()));
        cc.setResponseTimeout(Duration.ofMillis(configuration.getResponseTimeout()));

        if (configuration.getTrustCertificates() != null) {
            final X509CredentialConfig cfg = new X509CredentialConfig();
            cfg.setTrustCertificates(configuration.getTrustCertificates());
            cc.setSslConfig(new SslConfig());
        } else if (configuration.getKeystore() != null) {
            final KeyStoreCredentialConfig cfg = new KeyStoreCredentialConfig();
            cfg.setKeyStore(configuration.getKeystore());
            cfg.setKeyStorePassword(configuration.getKeystorePassword());
            cfg.setKeyStoreType(configuration.getKeystoreType());
            cc.setSslConfig(new SslConfig(cfg));
        } else {
            cc.setSslConfig(new SslConfig());
        }

        if (configuration.getSaslMechanism() != null) {
            final BindConnectionInitializer bc = new BindConnectionInitializer();
            final SaslConfig sc;
            switch (configuration.getSaslMechanism()) {
                case DIGEST_MD5:
                    sc = new DigestMd5Config();
                    ((DigestMd5Config)sc).setRealm(configuration.getSaslRealm());
                    break;

                case CRAM_MD5:
                    sc = new CramMd5Config();
                    break;

                case EXTERNAL:
                    sc = new ExternalConfig();
                    break;

                case GSSAPI:
                    sc = new GssApiConfig();
                    ((GssApiConfig)sc).setRealm(configuration.getSaslRealm());
                    break;

                default:
                    throw new IllegalArgumentException("Unknown SASL mechanism " + configuration.getSaslMechanism().name());

            }

            sc.setAuthorizationId(configuration.getSaslAuthorizationId());
            sc.setMutualAuthentication(configuration.getSaslMutualAuth());
            sc.setQualityOfProtection(configuration.getSaslQualityOfProtection());
            sc.setSecurityStrength(configuration.getSaslSecurityStrength());
            bc.setBindSaslConfig(sc);
            cc.setConnectionInitializer(bc);
        } else if ("*".equals(configuration.getBindCredential()) && "*".equals(configuration.getBindDn())) {
            cc.setConnectionInitializer(new FastBindOperation.FastBindConnectionInitializer());
        } else if (!Strings.isNullOrEmpty(configuration.getBindDn()) && Strings.isNullOrEmpty(configuration.getBindCredential())) {
            cc.setConnectionInitializer(new BindConnectionInitializer(configuration.getBindDn(),
                                                                      new Credential(configuration.getBindCredential())));
        }

        final DefaultConnectionFactory bindCf = new DefaultConnectionFactory(cc);

        if (configuration.getProviderClass() != null) {
            try {
                final Class clazz = Class.forName(configuration.getProviderClass());
                bindCf.setProvider(org.ldaptive.provider.Provider.class.cast(clazz.newInstance()));
            } catch (final Exception e) {
                LOG.error(e.getMessage(), e);
            }
        }
        final BlockingConnectionPool cp = new BlockingConnectionPool(pc, bindCf);

        cp.setBlockWaitTime(Duration.ofMillis(configuration.getBlockWaitTime()));
        cp.setPoolConfig(pc);

        final IdlePruneStrategy strategy = new IdlePruneStrategy();
        strategy.setIdleTime(Duration.ofMillis(configuration.getIdleTime()));
        strategy.setPrunePeriod(Duration.ofMillis(configuration.getPrunePeriod()));

        cp.setPruneStrategy(strategy);
        cp.setValidator(new SearchValidator());
        cp.setFailFastInitialize(configuration.isFailFast());
        cp.initialize();
        connFactory = new PooledConnectionFactory(cp);
    }


    @Override
    public PooledConnectionFactory get() {
        return connFactory;
    }


}
