/*
 *  [2015] - [2016] Codenvy, S.A.
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
'use strict';

/**
 * This class is handling the controller for the nag message
 * @author Oleksii Orel
 */
export class NagMessageCtrl {

  /**
   * Default constructor.
   * @ngInject for Dependency injection
   */
  constructor($scope, cheAPI, codenvyAPI, imsLicenseApi, nagMessageService, codenvyPermissions) {
    this.$scope = $scope;
    this.cheAPI = cheAPI;
    this.codenvyAPI = codenvyAPI;
    this.imsLicenseApi = imsLicenseApi;
    this.nagMessageService = nagMessageService;

    this.userServices = codenvyPermissions.getUserServices();
    this.numberOfFreeUsers = imsLicenseApi.getNumberOfFreeUsers();

    let licenseLegality = imsLicenseApi.getLicenseLegality();

    this.imsLicenseApi.fetchLicenseLegality().finally(() => {
      this.checkLegality(licenseLegality);
    });

    this.$scope.$watch(()=> {
      return licenseLegality.value;
    }, ()=> {
      this.checkLegality(licenseLegality);
    });
  }

  /**
   * Check the license legality
   * @param licenseLegality
   */
  checkLegality(licenseLegality) {
    if (licenseLegality && licenseLegality.value === 'false') {
      this.nagMessageService.showLicenseMessage();
    } else {
      this.nagMessageService.hideLicenseMessage();
    }
  }
}
