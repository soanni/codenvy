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

import org.ldaptive.sasl.Mechanism;
import org.ldaptive.sasl.QualityOfProtection;
import org.ldaptive.sasl.SecurityStrength;

/**
 * Created by sj on 13.09.16.
 */
public final class LdapConfigurationBuilder {
    private String                               ldapUrl;
    private String                               baseDn;
    private String                               userFilter;
    private boolean                              allowMultipleDns;
    private boolean                              subtreeSearch;
    private String                               bindDn;
    private String                               bindCredential;
    private int                                  minPoolSize;
    private int                                  maxPoolSize;
    private boolean                              validateOnCheckout;
    private boolean                              validateOnCheckin;
    private boolean                              validatePeriodically;
    private long                                 validatePeriod;
    private boolean                              failFast;
    private long                                 idleTime;
    private long                                 prunePeriod;
    private long                                 blockWaitTime;
    private long                                 connectTimeout;
    private long                                 responseTimeout;
    private String                               dnFormat;
    private String                               userPasswordAttribute;
    private boolean                              useSsl;
    private boolean                              useStartTls;
    private LdapConfiguration.AuthenticationType type;
    private String                               providerClass;
    private String                               trustCertificates;
    private String                               keystore;
    private String                               keystorePassword;
    private String                               keystoreType;
    private Mechanism                            saslMechanism;
    private String                               saslRealm;
    private String                               saslAuthorizationId;
    private SecurityStrength                     saslSecurityStrength;
    private Boolean                              saslMutualAuth;
    private QualityOfProtection                  saslQualityOfProtection;

    private LdapConfigurationBuilder() {
    }

    public static LdapConfigurationBuilder builder() {
        return new LdapConfigurationBuilder();
    }

    public LdapConfigurationBuilder withLdapUrl(String ldapUrl) {
        this.ldapUrl = ldapUrl;
        return this;
    }

    public LdapConfigurationBuilder withBaseDn(String baseDn) {
        this.baseDn = baseDn;
        return this;
    }

    public LdapConfigurationBuilder withUserFilter(String userFilter) {
        this.userFilter = userFilter;
        return this;
    }

    public LdapConfigurationBuilder withAllowMultipleDns(boolean allowMultipleDns) {
        this.allowMultipleDns = allowMultipleDns;
        return this;
    }

    public LdapConfigurationBuilder withSubtreeSearch(boolean subtreeSearch) {
        this.subtreeSearch = subtreeSearch;
        return this;
    }

    public LdapConfigurationBuilder withBindDn(String bindDn) {
        this.bindDn = bindDn;
        return this;
    }

    public LdapConfigurationBuilder withBindCredential(String bindCredential) {
        this.bindCredential = bindCredential;
        return this;
    }

    public LdapConfigurationBuilder withMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
        return this;
    }

    public LdapConfigurationBuilder withMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }

    public LdapConfigurationBuilder withValidateOnCheckout(boolean validateOnCheckout) {
        this.validateOnCheckout = validateOnCheckout;
        return this;
    }

    public LdapConfigurationBuilder withValidateOnCheckin(boolean validateOnCheckin) {
        this.validateOnCheckin = validateOnCheckin;
        return this;
    }

    public LdapConfigurationBuilder withValidatePeriodically(boolean validatePeriodically) {
        this.validatePeriodically = validatePeriodically;
        return this;
    }

    public LdapConfigurationBuilder withValidatePeriod(long validatePeriod) {
        this.validatePeriod = validatePeriod;
        return this;
    }

    public LdapConfigurationBuilder withFailFast(boolean failFast) {
        this.failFast = failFast;
        return this;
    }

    public LdapConfigurationBuilder withIdleTime(long idleTime) {
        this.idleTime = idleTime;
        return this;
    }

    public LdapConfigurationBuilder withPrunePeriod(long prunePeriod) {
        this.prunePeriod = prunePeriod;
        return this;
    }

    public LdapConfigurationBuilder withBlockWaitTime(long blockWaitTime) {
        this.blockWaitTime = blockWaitTime;
        return this;
    }

    public LdapConfigurationBuilder withConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public LdapConfigurationBuilder withResponseTimeout(long responseTimeout) {
        this.responseTimeout = responseTimeout;
        return this;
    }

    public LdapConfigurationBuilder withDnFormat(String dnFormat) {
        this.dnFormat = dnFormat;
        return this;
    }

    public LdapConfigurationBuilder withUserPasswordAttribute(String userPasswordAttribute) {
        this.userPasswordAttribute = userPasswordAttribute;
        return this;
    }

    public LdapConfigurationBuilder withUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
        return this;
    }

    public LdapConfigurationBuilder withUseStartTls(boolean useStartTls) {
        this.useStartTls = useStartTls;
        return this;
    }

    public LdapConfigurationBuilder withType(LdapConfiguration.AuthenticationType type) {
        this.type = type;
        return this;
    }

    public LdapConfigurationBuilder withProviderClass(String providerClass) {
        this.providerClass = providerClass;
        return this;
    }

    public LdapConfigurationBuilder withTrustCertificates(String trustCertificates) {
        this.trustCertificates = trustCertificates;
        return this;
    }

    public LdapConfigurationBuilder withKeystore(String keystore) {
        this.keystore = keystore;
        return this;
    }

    public LdapConfigurationBuilder withKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
        return this;
    }

    public LdapConfigurationBuilder withKeystoreType(String keystoreType) {
        this.keystoreType = keystoreType;
        return this;
    }

    public LdapConfigurationBuilder withSaslMechanism(Mechanism saslMechanism) {
        this.saslMechanism = saslMechanism;
        return this;
    }

    public LdapConfigurationBuilder withSaslRealm(String saslRealm) {
        this.saslRealm = saslRealm;
        return this;
    }

    public LdapConfigurationBuilder withSaslAuthorizationId(String saslAuthorizationId) {
        this.saslAuthorizationId = saslAuthorizationId;
        return this;
    }

    public LdapConfigurationBuilder withSaslSecurityStrength(SecurityStrength saslSecurityStrength) {
        this.saslSecurityStrength = saslSecurityStrength;
        return this;
    }

    public LdapConfigurationBuilder withSaslMutualAuth(Boolean saslMutualAuth) {
        this.saslMutualAuth = saslMutualAuth;
        return this;
    }

    public LdapConfigurationBuilder withSaslQualityOfProtection(QualityOfProtection saslQualityOfProtection) {
        this.saslQualityOfProtection = saslQualityOfProtection;
        return this;
    }

    public LdapConfiguration build() {
        LdapConfiguration ldapConfiguration =
                new LdapConfiguration(ldapUrl, baseDn, type,
                                      providerClass, dnFormat,
                                      userPasswordAttribute, userFilter, allowMultipleDns, subtreeSearch, bindDn, bindCredential,
                                      minPoolSize,
                                      maxPoolSize, validateOnCheckout, validateOnCheckin, validatePeriodically, validatePeriod, failFast,
                                      idleTime, prunePeriod, blockWaitTime, connectTimeout, responseTimeout, useSsl, useStartTls,
                                      trustCertificates, keystore,
                                      keystorePassword, keystoreType, saslMechanism, saslRealm, saslAuthorizationId, saslSecurityStrength,
                                      saslMutualAuth, saslQualityOfProtection);
        return ldapConfiguration;
    }


}
