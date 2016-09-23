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
 * This class is handling the controller for the admins user management
 * @author Oleksii Orel
 */
export class AdminsUserManagementCtrl {

  /**
   * Default constructor.
   * @ngInject for Dependency injection
   */
  constructor($q, lodash, $document, $mdDialog, codenvyAPI, cheNotification) {
    'ngInject';

    this.$q = $q;
    this.lodash = lodash;
    this.$document = $document;
    this.$mdDialog = $mdDialog;
    this.codenvyAPI = codenvyAPI;
    this.cheNotification = cheNotification;

    this.isLoading = false;

    this.maxItems = 12;
    this.skipCount = 0;

    this.users = [];
    this.usersMap = codenvyAPI.getUser().getUsersMap();

    this.userOrderBy = 'name';
    this.userFilter = {name: ''};
    this.usersSelectedStatus = {};
    this.isNoSelected = true;
    this.isAllSelected = false;
    this.isBulkChecked = false;

    if (this.usersMap && this.usersMap.size > 1) {
      this.updateUsers();
    } else {
      this.isLoading = true;
      codenvyAPI.getUser().fetchUsers(this.maxItems, this.skipCount).then(() => {
        this.isLoading = false;
        this.updateUsers();
      }, (error) => {
        this.isLoading = false;
        if (error && error.status !== 304) {
          this.cheNotification.showError(error.data && error.data.message ? error.data.message : 'Failed to retrieve the list of users.');
        }
      });
    }

    this.pagesInfo = codenvyAPI.getUser().getPagesInfo();
  }

  /**
   * Check all users in list
   */
  selectAllUsers() {
    this.users.forEach((user) => {
      this.usersSelectedStatus[user.id] = true;
    });
  }

  /**
   * Uncheck all users in list
   */
  deselectAllUsers() {
    this.users.forEach((user) => {
      this.usersSelectedStatus[user.id] = false;
    });
  }

  /**
   * Change bulk selection value
   */
  changeBulkSelection() {
    if (this.isBulkChecked) {
      this.deselectAllUsers();
      this.isBulkChecked = false;
    } else {
      this.selectAllUsers();
      this.isBulkChecked = true;
    }
    this.updateSelectedStatus();
  }

  /**
   * Update users selected status
   */
  updateSelectedStatus() {
    this.isNoSelected = true;
    this.isAllSelected = true;

    this.users.forEach((user) => {
      if (this.usersSelectedStatus[user.id]) {
        this.isNoSelected = false;
      } else {
        this.isAllSelected = false;
      }
    });

    if (this.isNoSelected) {
      this.isBulkChecked = false;
      return;
    }

    if (this.isAllSelected) {
      this.isBulkChecked = true;
    }
  }

  /**
   * User clicked on the - action to remove the user. Show the dialog
   * @param  event - the $event
   * @param user - the selected user
   */
  removeUser(event, user) {
    let confirm = this.$mdDialog.confirm()
      .title('Would you like to remove user ' + user.email + ' ?')
      .content('Please confirm for the user removal.')
      .ariaLabel('Remove user')
      .ok('Remove')
      .cancel('Cancel')
      .clickOutsideToClose(true)
      .targetEvent(event);
    this.$mdDialog.show(confirm).then(() => {
      this.isLoading = true;
      let promise = this.codenvyAPI.getUser().deleteUserById(user.id);
      promise.then(() => {
        this.isLoading = false;
        this.updateUsers();
      }, (error) => {
        this.isLoading = false;
        this.cheNotification.showError(error.data && error.data.message ? error.data.message : 'Delete user failed.');
      });
    });
  }

  /**
   * Delete all selected users
   */
  deleteSelectedUsers() {
    let usersSelectedStatusKeys = Object.keys(this.usersSelectedStatus);
    let checkedUsersKeys = [];

    if (!usersSelectedStatusKeys.length) {
      this.cheNotification.showError('No such users.');
      return;
    }

    usersSelectedStatusKeys.forEach((key) => {
      if (this.usersSelectedStatus[key] === true) {
        checkedUsersKeys.push(key);
      }
    });

    let queueLength = checkedUsersKeys.length;
    if (!queueLength) {
      this.cheNotification.showError('No such user.');
      return;
    }

    let confirmationPromise = this.showDeleteUsersConfirmation(queueLength);

    confirmationPromise.then(() => {

      let numberToDelete = queueLength;
      let isError = false;
      let deleteUserPromises = [];
      let currentUserId;

      checkedUsersKeys.forEach((userId) => {
        currentUserId = userId;
        this.usersSelectedStatus[userId] = false;

        let promise = this.codenvyAPI.getUser().deleteUserById(userId);
        promise.then(() => {
          queueLength--;
        }, (error) => {
          isError = true;
          this.$log.error('Cannot delete user: ', error);
        });
        deleteUserPromises.push(promise);
      });

      this.$q.all(deleteUserPromises).finally(() => {
        this.isLoading = true;
        let promise = this.codenvyAPI.getUser().fetchUsersPage(this.pagesInfo.currentPageNumber);

        promise.then(() => {
          this.isLoading = false;
          this.updateUsers();
          this.updateSelectedStatus();
        }, (error) => {
          this.isLoading = false;
          this.$log.error(error);
        });
        if (isError) {
          this.cheNotification.showError('Delete failed.');
        } else {
          if (numberToDelete === 1) {
            let currentUser = this.lodash.find(this.users, (user) => {
              return user.id === currentUserId;
            });
            this.cheNotification.showInfo(currentUser ? currentUser.email + 'has been removed.' : 'Selected user has been removed.');
          } else {
            this.cheNotification.showInfo('Selected users have been removed.');
          }
        }
      });
    });
  }

  /**
   * Show confirmation popup before delete
   * @param numberToDelete
   * @returns {*}
   */
  showDeleteUsersConfirmation(numberToDelete) {
    let confirmTitle = 'Would you like to delete ';
    if (numberToDelete > 1) {
      confirmTitle += 'these ' + numberToDelete + ' users?';
    } else {
      confirmTitle += 'this selected user?';
    }
    let confirm = this.$mdDialog.confirm()
      .title(confirmTitle)
      .ariaLabel('Remove users')
      .ok('Delete!')
      .cancel('Cancel')
      .clickOutsideToClose(true);

    return this.$mdDialog.show(confirm);
  }

  /**
   * Update users array
   */
  updateUsers() {
    //update users array
    this.users.length = 0;
    this.usersMap.forEach((user) => {
      this.users.push(user);
    });
  }

  /**
   * Ask for loading the users page in asynchronous way
   * @param pageKey - the key of page
   */
  fetchUsersPage(pageKey) {
    this.isLoading = true;
    let promise = this.codenvyAPI.getUser().fetchUsersPage(pageKey);

    promise.then(() => {
      this.isLoading = false;
      this.updateUsers();
    }, (error) => {
      this.isLoading = false;
      if (error.status === 304) {
        this.updateUsers();
      } else {
        this.cheNotification.showError(error.data && error.data.message ? error.data.message : 'Update information failed.');
      }
    });
  }

  /**
   * Returns true if the next page is exist.
   * @returns {boolean}
   */
  hasNextPage() {
    return this.pagesInfo.currentPageNumber < this.pagesInfo.countOfPages;
  }

  /**
   * Returns true if the previous page is exist.
   * @returns {boolean}
   */
  hasPreviousPage() {
    return this.pagesInfo.currentPageNumber > 1;
  }

  /**
   * Returns true if we have more then one page.
   * @returns {boolean}
   */
  isPagination() {
    return this.pagesInfo.countOfPages > 1;
  }

  /**
   * Add a new user. Show the dialog
   * @param  event - the $event
   */
  showAddUserDialog(event) {
    let parentEl = angular.element(this.$document.body);

    this.$mdDialog.show({
      targetEvent: event,
      bindToController: true,
      clickOutsideToClose: true,
      controller: 'AdminsAddUserCtrl',
      controllerAs: 'adminsAddUserCtrl',
      locals: {callbackController: this},
      parent: parentEl,
      templateUrl: 'app/admin/user-management/add-user/add-user.html'
    });
  }
}
