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

import com.codenvy.ldap.auth.AuthenticatorProvider;
import com.codenvy.ldap.auth.EntryResolverProvider;
import com.codenvy.ldap.auth.LdapAuthenticationHandler;

import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.eclipse.che.api.auth.AuthenticationException;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.ldaptive.auth.EntryResolver;
import org.ldaptive.pool.PooledConnectionFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;


public class LdapAuthenticatedSearchTest {

    private static final String BASE_DN = "dc=codenvy,dc=com";

    private MyLdapServer              server;
    private LdapAuthenticationHandler handler;
    private List<ServerEntry>         createdEntries;
    private List<Pair<String, String>> users;


    @BeforeClass
    public void startServer() throws Exception {
        server = MyLdapServer.builder()
                             .setPartitionId("codenvy")
                             .allowAnonymousAccess()
                             .setPartitionDn(BASE_DN)
                             .useTmpWorkingDir()
                             .setMaxSizeLimit(1000)
                             .build();
        server.start();

        LdapConfiguration configuration = LdapConfigurationBuilder
                .builder()
                .withBaseDn(BASE_DN)
                .withUserFilter("cn={user}")
                .withFailFast(true)
                .withLdapUrl(server.getUrl())
                .withBindDn(server.getAdminDn())
                .withBindCredential(server.getAdminPassword())
                .withMinPoolSize(3)
                .withMaxPoolSize(10)
                .withValidateOnCheckout(false)
                .withValidateOnCheckin(false)
                .withValidatePeriodically(true)
                .withPrunePeriod(10_000)
                .withValidatePeriod(30 * 60 * 1000)
                .withConnectTimeout(30_000)
                .withResponseTimeout(30_000)
                .withSubtreeSearch(true)
                .withType(LdapConfiguration.AuthenticationType.AUTHENTICATED)
                .build();
        PooledConnectionFactory conn = new LdapConnectionFactoryProvider(configuration).get();
        EntryResolver entryResolver = new EntryResolverProvider(conn, configuration).get();
        handler = new LdapAuthenticationHandler(new AuthenticatorProvider(conn, entryResolver, configuration).get());
        // create a set of users
        SSHAPasswordEncryptor passwordEncryptor = new SSHAPasswordEncryptor();
        users = new ArrayList<>();
        createdEntries = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            String password = NameGenerator.generate("pwd", 20);
            users.add(new Pair<>("name" + i, password));
            createdEntries.add(server.addDefaultLdapUser(i,
                                                         Pair.of("givenName", "test-user-first-name" + i),
                                                         Pair.of("sn", "test-user-last-name"),
                                                         Pair.of("userPassword", passwordEncryptor.encrypt(password.getBytes())),
                                                         Pair.of("telephoneNumber", "00000000" + i)));
        }

    }

    @AfterClass
    public void stopServer() throws Exception {
        server.stop();
    }

    @Test
    public void testAuthenticatedSearch() throws LdapInvalidAttributeValueException, AuthenticationException {
        for (Pair<String, String> pair : users) {
            handler.authenticate(pair.first, pair.second);
        }
    }

    @Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = "Authentication failed. Please check username and password.")
    public void testWrongPassword() throws LdapInvalidAttributeValueException, AuthenticationException {
            handler.authenticate("name1", "nwrongpass");
    }
    //Disable due https://issues.apache.org/jira/browse/DIRSERVER-1548
    @Test(enabled = false, expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = "Authentication failed. Please check username and password.")
    public void testWrongUser() throws LdapInvalidAttributeValueException, AuthenticationException {
        handler.authenticate("name23431", "nwrongpass");
    }

}
