#!/bin/bash
#
# CODENVY CONFIDENTIAL
# ________________
#
# [2012] - [2015] Codenvy, S.A.
# All Rights Reserved.
# NOTICE: All information contained herein is, and remains
# the property of Codenvy S.A. and its suppliers,
# if any. The intellectual and technical concepts contained
# herein are proprietary to Codenvy S.A.
# and its suppliers and may be covered by U.S. and Foreign Patents,
# patents in process, and are protected by trade secret or copyright law.
# Dissemination of this information or reproduction of this material
# is strictly forbidden unless prior written permission is obtained
# from Codenvy S.A..
#

# load lib.sh from path stored in parameter 1
. $1

printAndLog "TEST CASE: Update previous version of single-node Codenvy On Premise to latest version"
vagrantUp ${SINGLE_NODE_VAGRANT_FILE}

# install previous version
installCodenvy ${PREV_CODENVY_VERSION}
validateInstalledCodenvyVersion ${PREV_CODENVY_VERSION}

# make backup
executeIMCommand "backup"
fetchJsonParameter "file"
BACKUP_PATH=${OUTPUT}

# update to latest version
executeIMCommand "download" "codenvy" "${LATEST_CODENVY_VERSION}"
executeIMCommand "install" "codenvy" "${LATEST_CODENVY_VERSION}"
validateInstalledCodenvyVersion ${LATEST_CODENVY_VERSION}

# should be an error when try to restore from backup of another version
executeIMCommand "--valid-exit-code=1" "restore" ${BACKUP_PATH}
validateExpectedString ".*\"Version.of.backed.up.artifact.'${PREV_CODENVY_VERSION}'.doesn't.equal.to.restoring.version.'${LATEST_CODENVY_VERSION}'\".*\"status\".\:.\"ERROR\".*"

printAndLog "RESULT: PASSED"
vagrantDestroy
