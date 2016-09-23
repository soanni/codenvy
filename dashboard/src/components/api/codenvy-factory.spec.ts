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
 * Test of the Codenvy Factory API
 */
describe('CodenvyFactory', () => {


  /**
   * User Factory for the test
   */
  let factory;

  /**
   * API builder.
   */
  let apiBuilder;

  /**
   * Backend for handling http operations
   */
  let httpBackend;

  /**
   * Codenvy backend
   */
  let codenvyBackend;

  /**
   *  setup module
   */
  beforeEach(angular.mock.module('codenvyDashboard'));

  /**
   * Inject factory and http backend
   */
  beforeEach(inject((codenvyFactory, codenvyAPIBuilder, codenvyHttpBackend) => {

    factory = codenvyFactory;
    apiBuilder = codenvyAPIBuilder;
    codenvyBackend = codenvyHttpBackend;
    httpBackend = codenvyHttpBackend.getHttpBackend();
  }));

  /**
   * Check assertion after the test
   */
  afterEach(() => {
    httpBackend.verifyNoOutstandingExpectation();
    httpBackend.verifyNoOutstandingRequest();
  });


  /**
   * Check that we're able to fetch factory data
   */
  it('Fetch factories', () => {
      // setup tests objects
      let maxItem = 3;
      let skipCount = 0;
      let testUser = apiBuilder.getUserBuilder().withId('testUserId').build();
      let testFactory1 = apiBuilder.getFactoryBuilder().withId('testId1').withName('testName1').withCreatorEmail('testEmail1').build();
      let testFactory2 = apiBuilder.getFactoryBuilder().withId('testId2').withName('testName2').withCreatorEmail('testEmail2').build();
      let testFactory3 = apiBuilder.getFactoryBuilder().withId('testId3').withName('testName3').withCreatorEmail('testEmail3').build();
      let testFactory4 = apiBuilder.getFactoryBuilder().withId('testId4').withName('testName4').withCreatorEmail('testEmail4').build();

      // providing requests
      // add test objects on Http backend
      codenvyBackend.setDefaultUser(testUser);
      codenvyBackend.addUserFactory(testFactory1);
      codenvyBackend.addUserFactory(testFactory2);
      codenvyBackend.addUserFactory(testFactory3);
      codenvyBackend.addUserFactory(testFactory4);
      codenvyBackend.setPageMaxItem(maxItem);
      codenvyBackend.setPageSkipCount(skipCount);

      // setup backend for factories
      codenvyBackend.factoriesBackendSetup();

      // fetch factory
      factory.fetchFactories(maxItem, skipCount);

      // expecting GETs
      httpBackend.expectGET('/api/user');

      httpBackend.expectGET('/api/factory/find?creator.userId=' + testUser.id + '&maxItems=' + maxItem + '&skipCount=' + skipCount);
      httpBackend.expectGET('/api/factory/' + testFactory1.id);
      httpBackend.expectGET('/api/factory/' + testFactory2.id);
      httpBackend.expectGET('/api/factory/' + testFactory3.id);

      // flush command
      httpBackend.flush();

      // now, check factories
      let pageFactories = factory.getPageFactories();
      let factory1 = factory.getFactoryById(testFactory1.id);

      let factory2 = factory.getFactoryById(testFactory2.id);
      let factory3 = factory.getFactoryById(testFactory3.id);

      // check id, name and email of pge factories
      expect(pageFactories.length).toEqual(maxItem);
      expect(factory1.id).toEqual(testFactory1.id);
      expect(factory1.name).toEqual(testFactory1.name);
      expect(factory1.creator.email).toEqual(testFactory1.creator.email);
      expect(factory2.id).toEqual(testFactory2.id);
      expect(factory2.name).toEqual(testFactory2.name);
      expect(factory2.creator.email).toEqual(testFactory2.creator.email);
      expect(factory3.id).toEqual(testFactory3.id);
      expect(factory3.name).toEqual(testFactory3.name);
      expect(factory3.creator.email).toEqual(testFactory3.creator.email);
    }
  );

  /**
   * Check that we're able to fetch factory data by id
   */
  it('Fetch factor by id', () => {
      // setup tests objects
      let testFactory = apiBuilder.getFactoryBuilder().withId('testId').withName('testName').withCreatorEmail('testEmail').build();

      // providing request
      // add test factory on Http backend
      codenvyBackend.addUserFactory(testFactory);

      // setup backend
      codenvyBackend.factoriesBackendSetup();

      // fetch factory
      factory.fetchFactoryById(testFactory.id);

      // expecting GETs
      httpBackend.expectGET('/api/factory/' + testFactory.id);

      // flush command
      httpBackend.flush();

      // now, check factory
      let targetFactory = factory.getFactoryById(testFactory.id);

      // check id, name and email of factory
      expect(targetFactory.id).toEqual(testFactory.id);
      expect(targetFactory.name).toEqual(testFactory.name);
      expect(targetFactory.creator.email).toEqual(testFactory.creator.email);
    }
  );

  /**
   * Check that we're able to delete factor by id
   */
  it('Delete factor by id', () => {
      // setup tests objects
      let testFactory = apiBuilder.getFactoryBuilder().withId('testId').withName('testName').withCreatorEmail('testEmail').build();

      // providing request
      // add test factory on Http backend
      codenvyBackend.addUserFactory(testFactory);

      // setup backend
      codenvyBackend.factoriesBackendSetup();

      // delete factory
      factory.deleteFactoryById(testFactory.id);

      // expecting GETs
      httpBackend.expectDELETE('/api/factory/' + testFactory.id);

      // flush command
      httpBackend.flush();
    }
  );

  /**
   * Gets factory page object from response
   */
  it('Gets factory page object from response', () => {
      let testFactory1 = apiBuilder.getFactoryBuilder().withId('testId1').withName('testName1').withCreatorEmail('testEmail1').build();
      let testFactory2 = apiBuilder.getFactoryBuilder().withId('testId2').withName('testName2').withCreatorEmail('testEmail2').build();
      let factories = [testFactory1, testFactory2];

      let test_link_1 = '/api/factory/find?creator.userId=testUserId&skipCount=0&maxItems=5';
      let test_rel_1 = 'first';
      let test_link_2 = '/api/factory/find?creator.userId=testUserId&skipCount=20&maxItems=5';
      let test_rel_2 = 'last';
      let test_link_3 = '/api/factory/find?creator.userId=testUserId&skipCount=5&maxItems=5';
      let test_rel_3 = 'next';

      let headersLink = '\<' + test_link_1 + '\>' + '; rel="' + test_rel_1 + '",' +
        '\<' + test_link_2 + '\>' + '; rel="' + test_rel_2 + '",' +
        '\<' + test_link_3 + '\>' + '; rel="' + test_rel_3 + '"';

      codenvyBackend.factoriesBackendSetup();

      // gets page
      let pageObject = factory._getPageFromResponse(factories, headersLink);

      httpBackend.flush();

      // check page factories and links
      expect(pageObject.factories).toEqual(factories);
      expect(pageObject.links.get(test_rel_1)).toEqual(test_link_1);
      expect(pageObject.links.get(test_rel_2)).toEqual(test_link_2);
      expect(pageObject.links.get(test_rel_3)).toEqual(test_link_3);
    }
  );

  /**
   * Gets maxItems and skipCount from link params
   */
  it('Gets maxItems and skipCount from link params', () => {
      let skipCount = 20;
      let maxItems = 5;
      let test_link = '/api/factory/find?creator.userId=testUserId&skipCount=' + skipCount + '&maxItems=' + maxItems;

      codenvyBackend.factoriesBackendSetup();

      // gets page
      let pageParams = factory._getPageParamByLink(test_link);

      httpBackend.flush();

      // check page factories and links
      expect(parseInt(pageParams.maxItems, 10)).toEqual(maxItems);
      expect(parseInt(pageParams.skipCount, 10)).toEqual(skipCount);
    }
  );

});
