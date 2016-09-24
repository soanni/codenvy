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


import com.codenvy.api.dao.authentication.AuthenticationHandler;
import com.codenvy.ldap.LdapUserIdNormalizer;
import com.codenvy.ldap.sync.UserMapper;

import org.eclipse.che.api.auth.AuthenticationException;
import org.ldaptive.Credential;
import org.ldaptive.LdapException;
import org.ldaptive.auth.AuthenticationRequest;
import org.ldaptive.auth.AuthenticationResponse;
import org.ldaptive.auth.AuthenticationResultCode;
import org.ldaptive.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * LDAP authentication handler that uses the ldaptive {@code Authenticator} component underneath.
 * This handler provides simple attribute resolution machinery by reading attributes from the entry
 * corresponding to the DN of the bound user (in the bound security context) upon successful authentication.
 *
 * @author Sergii Kabashniuk
 */
public class LdapAuthenticationHandler implements AuthenticationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LdapAuthenticationHandler.class);

    public static final String TYPE = "ldap";

    private final Authenticator        ldapAuthenticator;
    private final LdapUserIdNormalizer idNormalizer;
    private final String[]             returnAttributes;

    @Inject
    public LdapAuthenticationHandler(Authenticator ldapAuthenticator, LdapUserIdNormalizer idNormalizer) {
        this.ldapAuthenticator = ldapAuthenticator;
        this.idNormalizer = idNormalizer;
        this.returnAttributes = new String[] {idNormalizer.getIdAttributeName()};
    }

    @Override
    public String authenticate(String login, String password) throws AuthenticationException {

        final AuthenticationResponse response;
        try {
            LOG.debug("Attempting LDAP authentication for: {}", login);
            final AuthenticationRequest request = new AuthenticationRequest(login,
                                                                            new Credential(password));
            request.setReturnAttributes(returnAttributes);
            response = this.ldapAuthenticator.authenticate(request);
        } catch (final LdapException e) {
            throw new AuthenticationException(401, "Unexpected LDAP error");
        }
        LOG.debug("LDAP response: {}", response);

        if (!response.getResult()) {
            throw new AuthenticationException(401, "Authentication failed. Please check username and password.");
        }

        if (AuthenticationResultCode.DN_RESOLUTION_FAILURE == response.getAuthenticationResultCode()) {
            throw new AuthenticationException(login + "  is not found");
        }
        LOG.debug("Account state {}", response.getAccountState());
        return idNormalizer.retrieveAndNormalize(response.getLdapEntry());
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
