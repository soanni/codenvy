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
package com.codenvy.api.license.server;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import com.codenvy.api.license.server.license.CodenvyLicense;
import com.codenvy.api.license.server.license.CodenvyLicenseFactory;
import com.codenvy.api.license.server.license.CodenvyLicenseManager;
import com.codenvy.api.license.server.license.InvalidLicenseException;
import com.codenvy.api.license.server.license.LicenseException;
import com.codenvy.api.license.server.license.LicenseFeature;
import com.codenvy.api.license.server.license.LicenseNotFoundException;
import com.codenvy.swarm.client.SwarmDockerConnector;
import com.google.common.collect.ImmutableMap;

import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.dto.server.JsonStringMapImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.valueOf;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 * @author Alexander Andrienko
 */
@Path("/license")
@Api(value = "license", description = "License manager")
public class LicenseService {
    private static final Logger LOG                                 = LoggerFactory.getLogger(LicenseService.class);
    public static final  String CODENVY_LICENSE_PROPERTY_IS_EXPIRED = "isExpired";
    private static final String VALUE                               = "value";

    private final CodenvyLicenseManager licenseManager;
    private final CodenvyLicenseFactory licenseFactory;
    private final SwarmDockerConnector  dockerConnector;
    private final UserManager           userManager;

    @Inject
    public LicenseService(CodenvyLicenseManager licenseManager,
                          CodenvyLicenseFactory licenseFactory,
                          UserManager userManager,
                          SwarmDockerConnector dockerConnector) {
        this.licenseManager = licenseManager;
        this.licenseFactory = licenseFactory;
        this.userManager = userManager;
        this.dockerConnector = dockerConnector;
    }

    @DELETE
    @ApiOperation(value = "Deletes Codenvy license")
    @ApiResponses(value = {@ApiResponse(code = 204, message = "The license successfully removed"),
                           @ApiResponse(code = 500, message = "Server error"),
                           @ApiResponse(code = 404, message = "License not found")})
    public void deleteLicense() throws ApiException {
        try {
            licenseManager.delete();
        } catch (LicenseNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (LicenseException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Gets Codenvy license")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
                           @ApiResponse(code = 409, message = "Invalid license"),
                           @ApiResponse(code = 404, message = "License not found"),
                           @ApiResponse(code = 500, message = "Server error")})
    public Response getLicense() throws ApiException {
        try {
            CodenvyLicense codenvyLicense = licenseManager.load();
            return status(OK).entity(codenvyLicense.getLicenseText()).build();
        } catch (InvalidLicenseException e) {
            throw new ConflictException(e.getMessage());
        } catch (LicenseNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (LicenseException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        }
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Stores valid Codenvy license in the system")
    @ApiResponses(value = {@ApiResponse(code = 201, message = "OK"),
                           @ApiResponse(code = 409, message = "Invalid license"),
                           @ApiResponse(code = 500, message = "Server error")})
    public Response storeLicense(String license) throws ApiException {
        try {
            CodenvyLicense codenvyLicense = licenseFactory.create(license);
            licenseManager.store(codenvyLicense);
            return status(CREATED).build();
        } catch (InvalidLicenseException e) {
            throw new ConflictException(e.getMessage());
        } catch (LicenseException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        }
    }

    @GET
    @Path("/properties")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Loads Codenvy license properties")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
                           @ApiResponse(code = 404, message = "License not found"),
                           @ApiResponse(code = 409, message = "Invalid license"),
                           @ApiResponse(code = 500, message = "Server error")})
    public Response getLicenseProperties() throws ApiException {
        try {
            CodenvyLicense codenvyLicense = licenseManager.load();
            Map<LicenseFeature, String> features = codenvyLicense.getFeatures();

            Map<String, String> properties = features
                    .entrySet()
                    .stream()
                    .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey().toString(), entry.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            boolean licenseExpired = codenvyLicense.isExpired();
            properties.put(CODENVY_LICENSE_PROPERTY_IS_EXPIRED, valueOf(licenseExpired));

            return status(OK).entity(new JsonStringMapImpl<>(properties)).build();
        } catch (LicenseNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (InvalidLicenseException e) {
            throw new ConflictException(e.getMessage());
        } catch (LicenseException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        }
    }

    @GET
    @Path("/legality")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check if Codenvy usage matches Codenvy License constraints, or free usage rules.")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
                           @ApiResponse(code = 500, message = "Server error")})
    public Response isCodenvyUsageLegal() throws ApiException {
        long actualUsers;
        int actualServers;

        try {
            //TODO Rework getting total items count after merge with jpa integration
            actualUsers = userManager.getAll(1, 0).getTotalItemsCount();
            actualServers = dockerConnector.getAvailableNodes().size();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        } catch (ServerException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException("Failed to get amount of users for license validation. ", e);
        }

        try {
            CodenvyLicense codenvyLicense = licenseManager.load();
            boolean isLicenseUsageLegal = codenvyLicense.isLicenseUsageLegal(actualUsers, actualServers);
            Map<String, String> isCodenvyUsageLegalResponse = ImmutableMap.of(VALUE, String.valueOf(isLicenseUsageLegal));
            return status(OK).entity(new JsonStringMapImpl<>(isCodenvyUsageLegalResponse)).build();

        } catch (LicenseException e) {
            boolean isFreeUsageLegal = CodenvyLicense.isFreeUsageLegal(actualUsers, actualServers);
            Map<String, String> isFreeUsageLegalResponse = ImmutableMap.of(VALUE, String.valueOf(isFreeUsageLegal));
            return status(OK).entity(new JsonStringMapImpl<>(isFreeUsageLegalResponse)).build();
        }
    }

    @GET
    @Path("/legality/node")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Check if Codenvy node usage matches Codenvy License constraints, or free usage rules.",
                  notes = "If nodeNumber parameter is absent then actual number of machine nodes will be validated")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "OK"),
                           @ApiResponse(code = 500, message = "Server error")})
    public Response isCodenvyNodesUsageLegal(@ApiParam("Node number for legality validation")
                                             @QueryParam("nodeNumber")
                                             Integer nodeNumber) throws ApiException {
        try {
            if (nodeNumber == null) {
                nodeNumber = dockerConnector.getAvailableNodes().size();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            throw new ServerException(e.getMessage(), e);
        }

        try {
            CodenvyLicense codenvyLicense = licenseManager.load();

            boolean isLicenseNodesUsageLegal = codenvyLicense.isLicenseNodesUsageLegal(nodeNumber);
            return status(OK).entity(new JsonStringMapImpl<>(ImmutableMap.of(VALUE, valueOf(isLicenseNodesUsageLegal)))).build();
        } catch (LicenseException e) {
            boolean isFreeUsageLegal = CodenvyLicense.isFreeUsageLegal(0, nodeNumber);
            return status(OK).entity(new JsonStringMapImpl<>(ImmutableMap.of(VALUE, valueOf(isFreeUsageLegal)))).build();
        }
    }
}
