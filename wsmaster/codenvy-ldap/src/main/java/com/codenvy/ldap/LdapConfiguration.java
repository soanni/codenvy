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

import org.eclipse.che.commons.annotation.Nullable;
import org.ldaptive.sasl.Mechanism;
import org.ldaptive.sasl.QualityOfProtection;
import org.ldaptive.sasl.SecurityStrength;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;

/**
 * Ldap configuration.
 * Used to organise single place for ldap configuration in container.
 *
 * @author Sergii Kabashniuk
 */
public class LdapConfiguration {


    /**
     * The enum Authentication types.
     */
    public enum AuthenticationType {
        /**
         * Active Directory.
         */
        AD,
        /**
         * Authenticated Search.
         */
        AUTHENTICATED,
        /**
         * Direct Bind.
         */
        DIRECT,
        /**
         * Anonymous Search.
         */
        ANONYMOUS,
        /**
         * SASL bind search.
         */
        SASL
    }


    /**
     *  Url to ldap server. Configured over ldap.url property.
     */
    private final String ldapUrl;
    /**
     * DN to search; An empty value searches the rootDSE;
     * Configured over ldap.basedn property.
     */
    private final String baseDn;

    /**
     * search filter to execute; e.g. (mail={user}) Configured over ldap.userfilter.
     */
    private final String  userFilter;
    /**
     * whether to throw an exception if multiple entries are found with the search filter. Configured over ldap.allowmultipledns
     */
    private final boolean allowMultipleDns;
    /**
     * whether a subtree search should be performed.
     * Configured over ldap.subtreesearch
     */
    private final boolean subtreeSearch;
    /**
     * DN to bind as before performing operations.
     * Configured over ldap.bind.dn
     */
    private final String  bindDn;
    /**
     * Credential for the bind DN.
     * Configured over ldap.bind.password
     */
    private final String  bindCredential;
    /**
     * 	size the pool should be initialized to and pruned to.
     *  Configured over ldap.pool.minsize
     */
    private final int     minPoolSize;
    /**
     * maximum size the pool can grow to.
     * Configured over ldap.pool.maxsize
     */
    private final int     maxPoolSize;
    /**
     * whether connections should be validated when loaned out from the pool.
     * Configured over ldap.pool.validate.oncheckout
     */
    private final boolean validateOnCheckout;
    /**
     * whether connections should be validated when returned to the pool.
     * Configured over ldap.pool.validate.oncheckin
     */
    private final boolean validateOnCheckin;
    /**
     * whether connections should be validated periodically when the pool is idle
     * Configured over ldap.pool.validate.periodically
     */
    private final boolean validatePeriodically;
    /**
     * period in seconds at which pool should be validated.
     * Configured over ldap.pool.validate.period_ms
     */
    private final long    validatePeriod;

    /**
     * Whether should throw if pooling configuration requirements are not met.
     *
     * Configured over ldap.pool.failfast
     */
    private final boolean failFast;

    /**
     * Time  at which a connection should be considered idle and become
     * a candidate for removal from the pool.
     *
     * Configured over ldap.pool.idle_ms
     */
    private final long idleTime;
    /**
     * 	Period at which pool should be pruned.
     *
     * 	Configured over ldap.pool.prune_ms
     */
    private final long prunePeriod;
    /**
     * Duration to wait for an available connection
     *
     * 	Configured over ldap.pool.blockwait_ms
     */
    private final long blockWaitTime;
    /**
     * Maximum amount of time that connects will block.
     * Configured over ldap.connecttimeout_ms
     */
    private final long connectTimeout;
    /**
     * Sets the maximum amount of time that operations will wait for a response.
     * Configured over ldap.responsetimeout_ms
     */
    private final long responseTimeout;

    /**
     * Resolves an entry DN by using String#format. This resolver is typically used when an entry DN
     * can be formatted directly from the user identifier. For instance, entry DNs of the form
     * uid=dfisher,ou=people,dc=ldaptive,dc=org could be formatted
     * from uid=%s,ou=people,dc=ldaptive,dc=org.
     *
     * Configured over ldap.dnformat
     */
    private final String              dnFormat;
    /**
     * Configuration of PooledCompareAuthenticationHandler.
     * Authenticates an entry DN by performing an LDAP compare operation on the userPassword attribute.
     * This authentication handler should be used in cases where you do not have authorization to perform
     * binds, but do have authorization to read the userPassword attribute.
     *
     * Configured over ldap.userpasswordattribute
     */
    private final String              userPasswordAttribute;
    /**
     * When transmitting sensitive data to or from an LDAP it’s important to use a secure connection.
     * Connect to LDAP using SSL protocol.
     *
     * Configured over ldap.usessl
     */
    private final boolean             useSsl;
    /**
     * StartTLS allows the client to upgrade and downgrade the security of the connection as needed
     * Connect to LDAP using startTLS.
     *
     * Configured over ldap.usestarttls
     */
    private final boolean             useStartTls;
    /**
     *  Type authentication to use.
     *  AD - Active Directory. Users authenticate with sAMAccountName.
     *  AUTHENTICATED - Authenticated Search.  Manager bind/search followed by user simple bind.
     *  ANONYMOUS -  Anonymous search followed by user simple bind.
     *  DIRECT -  Direct Bind. Compute user DN from format string and perform simple bind.
     *  SASL - SASL bind search.
     *
     * Configured over ldap.authenticationtype
     */
    private final AuthenticationType  type;
    /**
     * Ldaptive does not implement any of the LDAP protocol.
     * Instead, LDAP operations are delegated to what we call a provider.
     * This allows developers and deployers to change the underlying library
     * that provides the LDAP implementation without modifying any code.
     * By default the JNDI provider is used, but a provider can be specified programmatically
     *
     * Configured over ldap.provider
     */
    private final String              providerClass;
    /**
     * Name of the trust certificates to use for the SSL connection.
     *
     * Configured over ldap.ssl.trustcertificates
     *
     */
    private final String              trustCertificates;
    /**
     * Name of the keystore to use for the SSL connection
     *
     * Configured over ldap.ssl.keystore.name
     */
    private final String              keystore;
    /**
     * Password needed to open the keystore.
     *
     * Configured over ldap.ssl.keystore.password
     */
    private final String              keystorePassword;
    /**
     * Keystore type.
     *
     * Configured over ldap.ssl.keystore.type
     */
    private final String              keystoreType;
    /**
     * SASL mechanisms
     *
     * Configured over ldap.sasl.mechanism
     */
    private final Mechanism           saslMechanism;
    /**
     * Sasl realm. Configuration data for SASL GSSAPI authentication.
     *
     * Configured over ldap.sasl.realm
     */
    private final String              saslRealm;
    /**
     * Sasl authorization id
     *
     * Configured over ldap.sasl.authorizationid
     */
    private final String              saslAuthorizationId;
    /**
     * sasl security strength.
     *
     * Configured over ldap.sasl.securitystrength
     */
    private final SecurityStrength    saslSecurityStrength;
    /**
     * Sasl perform mutual authentication.
     *
     * Configured over ldap.sasl.mutualauth
     */
    private final Boolean             saslMutualAuth;
    /**
     * sasl quality of protection
     *
     * Configured over ldap.sasl.qualityofprotection
     */
    private final QualityOfProtection saslQualityOfProtection;


    public LdapConfiguration(String ldapUrl, String baseDn, AuthenticationType type, String providerClass, String dnFormat, String userPasswordAttribute, String userFilter,
                             boolean allowMultipleDns, boolean subtreeSearch,
                             String bindDn, String bindCredential, int minPoolSize, int maxPoolSize, boolean validateOnCheckout,
                             boolean validateOnCheckin, boolean validatePeriodically, long validatePeriod, boolean failFast, long idleTime,
                             long prunePeriod, long blockWaitTime, long connectTimeout, long responseTimeout, boolean useSsl,
                             boolean useStartTls,
                             String trustCertificates, String keystore,
                             String keystorePassword, String keystoreType, Mechanism saslMechanism, String saslRealm,
                             String saslAuthorizationId, SecurityStrength saslSecurityStrength, Boolean saslMutualAuth,
                             QualityOfProtection saslQualityOfProtection) {
        this.ldapUrl = ldapUrl;
        this.baseDn = baseDn;
        this.userFilter = userFilter;
        this.allowMultipleDns = allowMultipleDns;
        this.subtreeSearch = subtreeSearch;
        this.bindDn = bindDn;
        this.bindCredential = bindCredential;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.validateOnCheckout = validateOnCheckout;
        this.validateOnCheckin = validateOnCheckin;
        this.validatePeriodically = validatePeriodically;
        this.validatePeriod = validatePeriod;
        this.failFast = failFast;
        this.idleTime = idleTime;
        this.prunePeriod = prunePeriod;
        this.blockWaitTime = blockWaitTime;
        this.connectTimeout = connectTimeout;
        this.responseTimeout = responseTimeout;
        this.dnFormat = dnFormat;
        this.userPasswordAttribute = userPasswordAttribute;
        this.useSsl = useSsl;
        this.useStartTls = useStartTls;
        this.type = type;
        this.providerClass = providerClass;
        this.trustCertificates = trustCertificates;
        this.keystore = keystore;
        this.keystorePassword = keystorePassword;
        this.keystoreType = keystoreType;
        this.saslMechanism = saslMechanism;
        this.saslRealm = saslRealm;
        this.saslAuthorizationId = saslAuthorizationId;
        this.saslSecurityStrength = saslSecurityStrength;
        this.saslMutualAuth = saslMutualAuth;
        this.saslQualityOfProtection = saslQualityOfProtection;
    }

    @Inject
    public LdapConfiguration(@NotNull @Named("ldap.url") String ldapUrl,
                             @NotNull @Named("ldap.base_dn") String baseDn,
                             @Nullable @Named("ldap.auth.authentication_type") String type,
                             @Nullable @Named("ldap.auth.provider") String providerClass,
                             @Nullable @Named("ldap.auth.dn_format") String dnFormat,
                             @Nullable @Named("ldap.auth.user_password_attribute") String userPasswordAttribute,
                             @Nullable @Named("ldap.auth.user.filter") String userFilter,
                             @Nullable @Named("ldap.auth.allow_multiple_dns") boolean allowMultipleDns,
                             @Nullable @Named("ldap.auth.subtree_search") boolean subtreeSearch,
                             @Nullable @Named("ldap.connection.bind.dn") String bindDn,
                             @Nullable @Named("ldap.connection.bind.password") String bindCredential,
                             @Nullable @Named("ldap.connection.use_ssl") boolean useSsl,
                             @Nullable @Named("ldap.connection.use_start_tls") boolean useStartTls,
                             @Nullable @Named("ldap.connection.pool.min_size") int minPoolSize,
                             @Nullable @Named("ldap.connection.pool.max_size") int maxPoolSize,
                             @Nullable @Named("ldap.connection.pool.validate.on_checkout") boolean validateOnCheckout,
                             @Nullable @Named("ldap.connection.pool.validate.on_checkin") boolean validateOnCheckin,
                             @Nullable @Named("ldap.connection.pool.validate.period_ms") long validatePeriod,
                             @Nullable @Named("ldap.connection.pool.validate.periodically") boolean validatePeriodically,
                             @Nullable @Named("ldap.connection.pool.fail_fast") boolean failFast,
                             @Nullable @Named("ldap.connection.pool.idle_ms") long idleTime,
                             @Nullable @Named("ldap.connection.pool.prune_ms") long prunePeriod,
                             @Nullable @Named("ldap.connection.pool.block_wait_ms") long blockWaitTime,
                             @Nullable @Named("ldap.connection.connect_timeout_ms") long connectTimeout,
                             @Nullable @Named("ldap.connection.response_timeout_ms") long responseTimeout,
                             @Nullable @Named("ldap.connection.ssl.trust_certificates") String trustCertificates,
                             @Nullable @Named("ldap.connection.ssl.keystore.name") String keystore,
                             @Nullable @Named("ldap.connection.ssl.keystore.password") String keystorePassword,
                             @Nullable @Named("ldap.connection.ssl.keystore.type") String keystoreType,
                             @Nullable @Named("ldap.connection.sasl.realm") String saslRealm,
                             @Nullable @Named("ldap.connection.sasl.mechanism") String saslMechanism,
                             @Nullable @Named("ldap.connection.sasl.authorization_id") String saslAuthorizationId,
                             @Nullable @Named("ldap.connection.sasl.security_strength") String saslSecurityStrength,
                             @Nullable @Named("ldap.connection.sasl.mutual_auth") boolean saslMutualAuth,
                             @Nullable @Named("ldap.connection.sasl.quality_of_protection") String saslQualityOfProtection) {


        this(ldapUrl,
             baseDn,
             AuthenticationType.valueOf(type),
             providerClass,
             dnFormat,
             userPasswordAttribute,
             userFilter,
             allowMultipleDns,
             subtreeSearch,
             bindDn,
             bindCredential,
             minPoolSize,
             maxPoolSize,
             validateOnCheckout,
             validateOnCheckin,
             validatePeriodically,
             validatePeriod,
             failFast,
             idleTime,
             prunePeriod,
             blockWaitTime,
             connectTimeout,
             responseTimeout,
             useSsl,
             useStartTls,
             trustCertificates,
             keystore,
             keystorePassword,
             keystoreType,
             saslMechanism != null ? Mechanism.valueOf(saslMechanism) : null,
             saslRealm,
             saslAuthorizationId,
             saslSecurityStrength !=null ? SecurityStrength.valueOf(saslSecurityStrength) : null,
             saslMutualAuth,
             saslQualityOfProtection != null ? QualityOfProtection.valueOf(saslQualityOfProtection) : null
            );

    }


    public boolean isAllowMultipleDns() {
        return allowMultipleDns;
    }

    public AuthenticationType getType() {
        return type;
    }

    public String getDnFormat() {
        return dnFormat;
    }

    public String getUserPasswordAttribute() {
        return userPasswordAttribute;
    }

    public boolean isSubtreeSearch() {
        return subtreeSearch;
    }

    public String getBaseDn() {
        return baseDn;
    }

    public String getUserFilter() {
        return userFilter;
    }

    public String getTrustCertificates() {
        return trustCertificates;
    }

    public String getKeystore() {
        return keystore;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public String getKeystoreType() {
        return keystoreType;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public boolean isValidateOnCheckout() {
        return validateOnCheckout;
    }

    public boolean isValidateOnCheckin() {
        return validateOnCheckin;
    }

    public boolean isValidatePeriodically() {
        return validatePeriodically;
    }

    public long getValidatePeriod() {
        return validatePeriod;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public long getIdleTime() {
        return idleTime;
    }

    public long getPrunePeriod() {
        return prunePeriod;
    }

    public long getBlockWaitTime() {
        return blockWaitTime;
    }

    public String getLdapUrl() {
        return ldapUrl;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public boolean isUseStartTls() {
        return useStartTls;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public long getResponseTimeout() {
        return responseTimeout;
    }


    public String getProviderClass() {
        return providerClass;
    }

    public String getBindDn() {
        return bindDn;
    }

    public String getBindCredential() {
        return bindCredential;
    }

    public String getSaslRealm() {
        return saslRealm;
    }

    public Mechanism getSaslMechanism() {
        return saslMechanism;
    }

    public String getSaslAuthorizationId() {
        return saslAuthorizationId;
    }

    public SecurityStrength getSaslSecurityStrength() {
        return saslSecurityStrength;
    }

    public Boolean getSaslMutualAuth() {
        return saslMutualAuth;
    }

    public QualityOfProtection getSaslQualityOfProtection() {
        return saslQualityOfProtection;
    }
}
