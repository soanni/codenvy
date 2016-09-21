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

/* global FormData */

/**
 * This class is handling the factory retrieval
 * @author Florent Benoit
 * @author Oleksii Orel
 */
export class CodenvyFactory {

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor($resource, $q, codenvyUser, lodash) {
    // keep resource
    this.$resource = $resource;
    this.$q = $q;

    this.codenvyUser = codenvyUser;
    this.lodash = lodash;

    this.factoriesById = new Map();// factories details by id
    this.parametersFactories = new Map();
    this.factoryContentsByWorkspaceId = new Map();

    // remote calls
    this.remoteFactoryFindAPI = this.$resource('/api/factory/find', {}, {
      getFactories: {
        method: 'GET',
        url: '/api/factory/find?creator.userId=:userId&maxItems=:maxItems&skipCount=:skipCount',
        isArray: false,
        responseType: 'json',
        transformResponse: (data, headersGetter) => {
          return this._getPageFromResponse(data, headersGetter('link'));
        }
      }
    });

    this.remoteFactoryAPI = this.$resource('/api/factory/:factoryId', {factoryId: '@id'}, {
      put: {method: 'PUT', url: '/api/factory/:factoryId'},
      getFactoryContentFromWorkspace: {method: 'GET', url: '/api/factory/workspace/:workspaceId'},
      getParametersFactory: {method: 'POST', url: '/api/factory/resolver/'},
      createFactoryByContent: {
        method: 'POST',
        url: '/api/factory',
        isArray: false,
        headers: {'Content-Type': undefined},
        transformRequest: angular.identity
      }
    });

    //paging
    this.pageFactories = [];// all current page factories
    this.factoryPagesMap = new Map();// page factories by relative link
    this.pageInfo = {};//pages info
  }


  _getPageFromResponse(data, headersLink) {
    let links = new Map();
    if (!headersLink) {
      //TODO remove it after adding headers paging links on server side
      let user = this.codenvyUser.getUser();
      if (!this.itemsPerPage || !user) {
        return {factories: data};
      }
      this.pageInfo.currentPageNumber = this.pageInfo.currentPageNumber ? this.pageInfo.currentPageNumber : 1;
      let link = '/api/factory/find?creator.userId=' + user.id + '&maxItems=' + this.itemsPerPage;
      links.set('first', link + '&skipCount=0');
      if (data && data.length > 0) {
        links.set('next', link + '&skipCount=' + this.pageInfo.currentPageNumber * this.itemsPerPage);
      }
      if (this.pageInfo.currentPageNumber > 1) {
        links.set('prev', link + '&skipCount=' + (this.pageInfo.currentPageNumber - 2) * this.itemsPerPage);
      }
      return {
        factories: data,
        links: links
      };
    }
    let pattern = new RegExp('<([^>]+?)>.+?rel="([^"]+?)"', 'g');
    let result;
    while (result = pattern.exec(headersLink)) { //look for pattern
      links.set(result[2], result[1]);//add link
    }
    return {
      factories: data,
      links: links
    };
  }

  _getPageParamByLink(pageLink) {
    let pageParamMap = new Map();
    let pattern = new RegExp('([_\\w]+)=([\\w]+)', 'g');
    let result;
    while (result = pattern.exec(pageLink)) {
      pageParamMap.set(result[1], result[2]);
    }

    let skipCount = pageParamMap.get('skipCount');
    let maxItems = pageParamMap.get('maxItems');
    if (!maxItems || maxItems === 0) {
      return null;
    }
    return {
      maxItems: maxItems,
      skipCount: skipCount ? skipCount : 0
    };
  }

  _updateCurrentPage() {
    let pageData = this.factoryPagesMap.get(this.pageInfo.currentPageNumber);
    if (!pageData) {
      return;
    }
    this.pageFactories.length = 0;
    if (!pageData.factories) {
      return;
    }
    pageData.factories.forEach((factory) => {
      factory.name = factory.name ? factory.name : '';
      this.pageFactories.push(factory);
    });
  }

  _updateCurrentPageFactories(factories) {
    this.pageFactories.length = 0;
    if (!factories) {
      return;
    }
    factories.forEach((factory) => {
      factory.name = factory.name ? factory.name : '';
      this.pageFactories.push(factory);
    });
  }

  /**
   * Update factory page links by relative direction ('first', 'prev', 'next', 'last')
   */
  _updatePagesData(data) {
    if (!data.links) {
      return;
    }

    let firstPageLink = data.links.get('first');
    if (firstPageLink) {
      let firstPageData = {link: firstPageLink};
      if (this.pageInfo.currentPageNumber === 1) {
        firstPageData.factories = data.factories;
      }
      if (!this.factoryPagesMap.get(1) || firstPageData.factories) {
        this.factoryPagesMap.set(1, firstPageData);
      }
    }
    let lastPageLink = data.links.get('last');
    if (lastPageLink) {
      let pageParam = this._getPageParamByLink(lastPageLink);
      this.pageInfo.countOfPages = pageParam.skipCount / pageParam.maxItems + 1;
      this.pageInfo.count = pageParam.skipCount;
      let lastPageData = {link: lastPageLink};
      if (this.pageInfo.currentPageNumber === this.pageInfo.countOfPages) {
        lastPageData.factories = data.factories;
      }
      if (!this.factoryPagesMap.get(this.pageInfo.countOfPages) || lastPageData.factories) {
        this.factoryPagesMap.set(this.pageInfo.countOfPages, lastPageData);
      }
    }
    let prevPageLink = data.links.get('prev');
    let prevPageNumber = this.pageInfo.currentPageNumber - 1;
    if (prevPageNumber > 0 && prevPageLink) {
      let prevPageData = {link: prevPageLink};
      if (!this.factoryPagesMap.get(prevPageNumber)) {
        this.factoryPagesMap.set(prevPageNumber, prevPageData);
      }
    }
    let nextPageLink = data.links.get('next');
    let nextPageNumber = this.pageInfo.currentPageNumber + 1;
    if (nextPageNumber) {
      let nextPageData = {link: nextPageLink};
      if (!this.factoryPagesMap.get(nextPageNumber)) {
        this.factoryPagesMap.set(nextPageNumber, nextPageData);
      }
    }
  }

  /**
   * Gets the pageInfo
   * @returns {Object}
   */
  getPagesInfo() {
    return this.pageInfo;
  }

  /**
   * Ask for loading the factories in asynchronous way
   * If there are no changes, it's not updated
   * @param maxItems - the max number of items to return
   * @param skipCount - the number of items to skip
   * @returns {*} the promise
   */
  fetchFactories(maxItems, skipCount) {
    this.itemsPerPage = maxItems;
    let promise = this._getFactories({maxItems: maxItems, skipCount: skipCount});

    return promise.then((data) => {
      this.pageInfo.currentPageNumber = skipCount / maxItems + 1;
      this._updateCurrentPageFactories(data.factories);
      this._updatePagesData(data);
    });
  }

  /**
   * Ask for loading the factories page in asynchronous way
   * If there are no changes, it's not updated
   * @param pageKey - the key of page ('first', 'prev', 'next', 'last'  or '1', '2', '3' ...)
   * @returns {*} the promise
   */
  fetchFactoryPage(pageKey) {
    let deferred = this.$q.defer();
    let pageNumber;
    if ('first' === pageKey) {
      pageNumber = 1;
    } else if ('prev' === pageKey) {
      pageNumber = this.pageInfo.currentPageNumber - 1;
    } else if ('next' === pageKey) {
      pageNumber = this.pageInfo.currentPageNumber + 1;
    } else if ('last' === pageKey) {
      pageNumber = this.pageInfo.countOfPages;
    } else {
      pageNumber = parseInt(pageKey, 10);
    }
    if (pageNumber < 1) {
      pageNumber = 1;
    } else if (pageNumber > this.pageInfo.countOfPages) {
      pageNumber = this.pageInfo.countOfPages;
    }
    let pageData = this.factoryPagesMap.get(pageNumber);
    if (pageData.link) {
      this.pageInfo.currentPageNumber = pageNumber;
      let queryData = this._getPageParamByLink(pageData.link);
      if (!queryData) {
        deferred.reject({data: {message: 'Error. No necessary link.'}});
        return deferred.promise;
      }
      let promise = this._getFactories(queryData);
      promise.then((data) => {
        this._updatePagesData(data);
        pageData.factories = data.factories;
        this._updateCurrentPage();
        deferred.resolve(data);
      }, (error) => {
        if (error && error.status === 304) {
          this._updateCurrentPage();
        }
        deferred.reject(error);
      });
    } else {
      deferred.reject({data: {message: 'Error. No necessary link.'}});
    }
    return deferred.promise;
  }

  _getFactories(queryData) {
    let deferred = this.$q.defer();
    let user = this.codenvyUser.getUser();

    if (user) {
      queryData.userId = user.id;
      this.remoteFactoryFindAPI.getFactories(queryData).$promise.then((data) => {
        this._updateFactoriesDetails(data.factories).then((factoriesDetails) => {
          data.factories = factoriesDetails;//update factories
          deferred.resolve(data);
        }, (error) => {
          deferred.reject(error);
        });
      }, (error) => {
        deferred.reject(error);
      });
    } else {
      this.codenvyUser.fetchUser().then((user) => {
        queryData.userId = user.id;
        this.remoteFactoryFindAPI.getFactories(queryData).$promise.then((data) => {
          this._updateFactoriesDetails(data.factories).then((factoriesDetails) => {
            data.factories = factoriesDetails;//update factories
            deferred.resolve(data);
          }, (error) => {
            deferred.reject(error);
          });
        }, (error) => {
          deferred.reject(error);
        });
      });
    }

    return deferred.promise;
  }

  _updateFactoriesDetails(factories) {
    let deferred = this.$q.defer();
    let factoriesDetails = [];

    if (!factories || factories.length === 0) {
      deferred.resolve(factoriesDetails);
    }

    let promises = [];
    factories.forEach((factory) => {
      let factoryPromise = this.fetchFactoryById(factory.id);//ask the factory details
      factoryPromise.then((factoryDetails) => {
        factoriesDetails.push(factoryDetails);
      });
      promises.push(factoryPromise);
    });
    this.$q.all(promises).then(() => {
      deferred.resolve(factoriesDetails);
    }, (error) => {
      deferred.reject(error);
    });

    return deferred.promise;
  }

  /**
   * Gets the factory service path.
   * @returns {string}
   */
  getFactoryServicePath() {
    return 'factory';
  }

  /**
   * Ask for loading the factory content in asynchronous way
   * If there are no changes, it's not updated
   * @param workspace
   * @returns {*} the promise
   */
  fetchFactoryContentFromWorkspace(workspace) {
    let deferred = this.$q.defer();

    let factoryContent = this.factoryContentsByWorkspaceId.get(workspace.id);
    if (factoryContent) {
      deferred.resolve(factoryContent);
    }

    let promise = this.remoteFactoryAPI.getFactoryContentFromWorkspace({
      workspaceId: workspace.id
    }).$promise;

    promise.then((factoryContent) => {
      //update factoryContents map
      this.factoryContentsByWorkspaceId.set(workspace.id, factoryContent);
      deferred.resolve(factoryContent);
    }, (error) => {
      if (error.status === 304) {
        let findFactoryContent = this.factoryContentsByWorkspaceId.get(workspace.id);
        deferred.resolve(findFactoryContent);
      } else {
        deferred.reject(error);
      }
    });

    return deferred.promise;
  }

  /**
   * Get factory from project
   * @param workspace
   * @return the factory content
   * @returns factoryContent
   */
  getFactoryContentFromWorkspace(workspace) {
    return this.factoryContentsByWorkspaceId.get(workspace.workspaceId);
  }

  /**
   * Create factory by content
   * @param factoryContent  the factory content
   * @returns {*} the promise
   */
  createFactoryByContent(factoryContent) {
    let formDataObject = new FormData();
    formDataObject.append('factory', factoryContent);

    return this.remoteFactoryAPI.createFactoryByContent({}, formDataObject).$promise;
  }

  /**
   * Gets the current page factories
   * @returns {Array}
   */
  getPageFactories() {
    return this.pageFactories;
  }

  /**
   * Ask for loading the factory in asynchronous way
   * If there are no changes, it's not updated
   * @param factoryId the factory ID
   * @returns {*} the promise
   */
  fetchFactoryById(factoryId) {
    let deferred = this.$q.defer();

    let promise = this.remoteFactoryAPI.get({factoryId: factoryId}).$promise;
    promise.then((factory) => {
      factory.name = factory.name ? factory.name : '';
      this.factoriesById.set(factoryId, factory);
      deferred.resolve(factory);
    }, (error) => {
      if (error.status === 304) {
        deferred.resolve(this.factoriesById.get(factoryId));
      } else {
        deferred.reject(error);
      }
    });

    return deferred.promise;
  }

  /**
   * Ask for getting parameter the factory in asynchronous way
   * If there are no changes, it's not updated
   * @param parameters the factory parameters
   * @returns {*} the promise
   */
  fetchParameterFactory(parameters) {
    let deferred = this.$q.defer();

    let promise = this.remoteFactoryAPI.getParametersFactory({}, parameters).$promise;
    promise.then((factory) => {
      factory.name = factory.name ? factory.name : '';
      this.parametersFactories.set(parameters, factory);
      deferred.resolve(factory);
    }, (error) => {
      if (error.status === 304) {
        deferred.resolve(this.parametersFactories.get(parameters));
      } else {
        deferred.reject(error);
      }
    });

    return deferred.promise;
  }

  /**
   * Detects links for factory acceptance (with id and named one)
   * @param factory factory to detect links
   * @returns [*] links acceptance links
   */
  detectLinks(factory) {
    let links = [];

    if (!factory || !factory.links) {
      return links;
    }

    this.lodash.find(factory.links, (link) => {
      if (link.rel === 'accept' || link.rel === 'accept-named') {
        links.push(link.href);
      }
    });

    return links;
  }

  /**
   * Get the factory from factoryMap by factoryId
   * @param factoryId the factory ID
   * @returns factory
   */
  getFactoryById(factoryId) {
    return this.factoriesById.get(factoryId);
  }

  /**
   * Set the factory
   * @param factory
   * @returns {*} the promise
   */
  setFactory(factory) {
    let deferred = this.$q.defer();
    // check factory
    if (!factory || !factory.id) {
      deferred.reject({data: {message: 'Read factory error.'}});
      return deferred.promise;
    }

    let promise = this.remoteFactoryAPI.put({factoryId: factory.id}, factory).$promise;
    // check if was OK or not
    promise.then((factory) => {
      factory.name = factory.name ? factory.name : '';
      this.fetchFactoryPage(this.pageInfo.currentPageNumber);
      deferred.resolve(factory);
    }, (error) => {
      deferred.reject(error);
    });

    return deferred.promise;
  }

  /**
   * Set the factory content by factoryId
   * @param factoryId  the factory ID
   * @param factoryContent  the factory content
   * @returns {*} the promise
   */
  setFactoryContent(factoryId, factoryContent) {
    let deferred = this.$q.defer();

    let promise = this.remoteFactoryAPI.put({factoryId: factoryId}, factoryContent).$promise;
    // check if was OK or not
    promise.then(() => {
      let fetchFactoryPromise = this.fetchFactoryById(factoryId);
      //check if was OK or not
      fetchFactoryPromise.then((factory) => {
        this.fetchFactoryPage(this.pageInfo.currentPageNumber);
        deferred.resolve(factory);
      }, (error) => {
        deferred.reject(error);
      });
    }, (error) => {
      deferred.reject(error);
    });

    return deferred.promise;
  }

  /**
   * Performs factory deleting by the given factoryId.
   * @param factoryId the factory ID
   * @returns {*} the promise
   */
  deleteFactoryById(factoryId) {
    let promise = this.remoteFactoryAPI.delete({factoryId: factoryId}).$promise;
    //check if was OK or not
    return promise.then(() => {
      this.factoriesById.delete(factoryId);//update factories map
      if (this.pageInfo && this.pageInfo.currentPageNumber) {
        this.fetchFactoryPage(this.pageInfo.currentPageNumber);
      }
    });
  }

  /**
   * Helper method that extract the factory ID from a factory URL
   * @param factoryURL the factory URL to analyze
   * @returns the stringified ID of a factory
   */
  getIDFromFactoryAPIURL(factoryURL) {
    let index = factoryURL.lastIndexOf('/factory/');
    if (index > 0) {
      return factoryURL.slice(index + '/factory/'.length, factoryURL.length);
    }
  }

  /**
   * Returns the factory url based on id.
   * @returns {link.href|*} link value
   */
  getFactoryIdUrl(factory) {
    let link = this.lodash.find(factory.links, (link) => {
      return 'accept' === link.rel;
    });
    return link ? link.href : 'No value';
  }

  /**
   * Returns the factory url based on name.
   *
   * @returns {link.href|*} link value
   */
  getFactoryNamedUrl(factory) {
    let link = this.lodash.find(factory.links, (link) => {
      return 'accept-named' === link.rel;
    });

    return link ? link.href : null;
  }
}
