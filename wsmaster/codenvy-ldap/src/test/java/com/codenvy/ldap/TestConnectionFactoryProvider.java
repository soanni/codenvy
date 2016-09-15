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

/**
 * @author Yevhenii Voevodin
 */
public class TestConnectionFactoryProvider extends LdapConnectionFactoryProvider {

    public TestConnectionFactoryProvider(MyLdapServer server) {
        super(LdapConfigurationBuilder.builder()
                                      .withBaseDn(server.getBaseDn())
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
                                      .build());
    }
}
