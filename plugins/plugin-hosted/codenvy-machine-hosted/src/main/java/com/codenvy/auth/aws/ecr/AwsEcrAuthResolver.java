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
package com.codenvy.auth.aws.ecr;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.model.AmazonECRException;
import com.amazonaws.services.ecr.model.AuthorizationData;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenResult;
import com.codenvy.auth.aws.AwsAccountCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.commons.annotation.Nullable;
import org.eclipse.che.plugin.docker.client.DockerRegistryDynamicAuthResolver;
import org.eclipse.che.plugin.docker.client.dto.AuthConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.eclipse.che.dto.server.DtoFactory.newDto;

/**
 * This class provides actual auth config for AWS ECR.
 *
 * @author Mykola Morhun
 */
@Singleton
public class AwsEcrAuthResolver implements DockerRegistryDynamicAuthResolver {
    private static final Logger LOG = LoggerFactory.getLogger(DockerRegistryDynamicAuthResolver.class);

    private final AwsEcrInitialAuthConfig awsEcrInitialAuthConfig;

    @Inject
    public AwsEcrAuthResolver(AwsEcrInitialAuthConfig awsInitialAuthConfig) {
        this.awsEcrInitialAuthConfig = awsInitialAuthConfig;
    }

    /**
     * Retrieves actual auth data for Amazon ECR.
     * If no credential found for specified registry, null will be returned.
     * <p/>
     * Note, that credentials is changed every 12 hours.
     *
     * @return actual auth credentials for given AWS ECR or null if no credentials found
     */
    @Override
    @Nullable
    public AuthConfig getXRegistryAuth(@Nullable String registry) {
        if (registry != null) {
            AwsAccountCredentials awsAccountCredentials = awsEcrInitialAuthConfig.getAuthConfigs().get(registry);
            if (awsAccountCredentials != null) { // given registry is configured
                try {
                    String authorizationToken = getAwsAuthorizationToken(awsAccountCredentials.getAccessKeyId(),
                                                                         awsAccountCredentials.getSecretAccessKey());
                    if (authorizationToken != null) {
                        String decodedAuthorizationToken = new String(Base64.getDecoder().decode(authorizationToken));
                        int colonIndex = decodedAuthorizationToken.indexOf(':');
                        if (colonIndex != -1) {
                            return newDto(AuthConfig.class).withUsername(decodedAuthorizationToken.substring(0, colonIndex))
                                                           .withPassword(decodedAuthorizationToken.substring(colonIndex + 1));
                        } else {
                            LOG.error("Cannot retrieve ECR credentials from token for {} registry", registry);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    LOG.error("Retrieved AWS ECR authorization token for {} registry has invalid format", registry);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves actual auth configs for configured Amazon ECRs.
     * If no AWS ECR credentials found, an empty map will be returned.
     *
     * @return actual AWS ECR auth config or empty map if ECR not configured
     */
    @Override
    public Map<String, AuthConfig> getXRegistryConfig() {
        Map<String, AuthConfig> dynamicAuthConfigs = new HashMap<>();

        for (String registry : awsEcrInitialAuthConfig.getAuthConfigs().keySet()) {
            AuthConfig authConfig = getXRegistryAuth(registry);
            if (authConfig != null) {
                dynamicAuthConfigs.put(registry, authConfig);
            }
        }

        return dynamicAuthConfigs;
    }

    @VisibleForTesting
    String getAwsAuthorizationToken(String accessKeyId, String secretAccessKey) {
        try {
            AWSCredentials credentials = new BasicAWSCredentials(accessKeyId, secretAccessKey);
            AmazonECRClient amazonECRClient = new AmazonECRClient(credentials);
            GetAuthorizationTokenResult tokenResult = amazonECRClient.getAuthorizationToken(new GetAuthorizationTokenRequest());
            List<AuthorizationData> authData = tokenResult.getAuthorizationData();

            if (!authData.isEmpty()) {
                return authData.get(0).getAuthorizationToken();
            }

            LOG.warn("Failed to retrieve AWS ECR token");
        } catch (AmazonECRException e) {
            LOG.warn(e.getLocalizedMessage());
        }
        return null;
    }

}
