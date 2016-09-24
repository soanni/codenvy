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

import com.codenvy.ldap.LdapUserIdNormalizer;
import com.codenvy.ldap.EmbeddedLdapServer;

import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.eclipse.che.api.auth.AuthenticationException;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.lang.Pair;
import org.ldaptive.auth.Authenticator;
import org.ldaptive.auth.EntryResolver;
import org.ldaptive.pool.PooledConnectionFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;


@Test
public class LdapAuthenticatedSearchTest {

    private static final String BASE_DN            = "dc=codenvy,dc=com";
    private static final String USER_FILTER        = "cn={user}";
    private static final String SUBTREE_SEARCH     = "false";
    private static final String ALLOW_MULTIPLE_DNS = "false";
    private EmbeddedLdapServer        server;
    private LdapAuthenticationHandler handler;

    private List<ServerEntry>          createdEntries;
    private List<Pair<String, String>> users;


    @BeforeMethod
    public void startServer() throws Exception {
        server = EmbeddedLdapServer.builder()
                                   .setPartitionId("codenvy")
                                   .setPartitionDn(BASE_DN)
                                   .useTmpWorkingDir()
                                   .setMaxSizeLimit(1000)
                                   .build();
        server.start();


        PooledConnectionFactory connectionFactory = server.getConnectionFactory();
        EntryResolver entryResolverProvider =
                new EntryResolverProvider(connectionFactory,
                                          server.getBaseDn(),
                                          USER_FILTER,
                                          SUBTREE_SEARCH).get();
        Authenticator authenticator =
                new AuthenticatorProvider(connectionFactory,
                                          entryResolverProvider,
                                          server.getBaseDn(),
                                          AuthenticationType.AUTHENTICATED.toString(),
                                          null, null,
                                          USER_FILTER,
                                          ALLOW_MULTIPLE_DNS, SUBTREE_SEARCH).get();
        handler = new LdapAuthenticationHandler(authenticator, new LdapUserIdNormalizer("uid"));


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

    @AfterMethod
    public void stopServer() throws Exception {
        server.shutdown();
    }

    @Test
    public void testAuthenticatedSearch() throws LdapInvalidAttributeValueException, AuthenticationException {
        for (int i = 0; i < 2; i++) {
            Pair<String, String> pair = users.get(i);
            assertEquals(handler.authenticate(pair.first, pair.second), "id" + i);
        }
    }

    @Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = "Authentication failed. Please check username and password.")
    public void testWrongPassword() throws LdapInvalidAttributeValueException, AuthenticationException {
        handler.authenticate("name1", "nwrongpass");
    }

    //Disable due https://issues.apache.org/jira/browse/DIRSERVER-1548
    @Test(expectedExceptions = AuthenticationException.class, expectedExceptionsMessageRegExp = "Authentication failed. Please check username and password.")
    public void testWrongUser() throws LdapInvalidAttributeValueException, AuthenticationException {
        handler.authenticate("name23431", "nwrongpass");
    }

}
