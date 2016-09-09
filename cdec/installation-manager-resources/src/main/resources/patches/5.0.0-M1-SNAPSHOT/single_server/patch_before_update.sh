#!/bin/bash

# tells bash that it should exit the script if any statement returns value > 0
set -e

oldLicensePath=/usr/local/codenvy/im/storage/config.properties
licensePropertyKey="codenvy-license-key"
licensePropertiesRegex="$licensePropertyKey=[a-zA-Z0-9]*$"
newLicenseDirectoryPath=/home/codenvy/codenvy-data/license
newLicensePath="$newLicenseDirectoryPath/license"

createLicenseDirectoryPath() {
  mkdir -p $newLicenseDirectoryPath
  chown -R codenvy:codenvy $newLicenseDirectoryPath
}

copyLicenseToNewLocation() {
  license=$1

  #create new license file
  touch $newLicensePath
  chown -R codenvy:codenvy $newLicensePath

  #write license key to the new license file
  echo $license >> $newLicensePath

  echo "License was successfully migrated to new location"
}

createLicenseDirectoryPath
if [ ! -e $newLicensePath ]; then
  if [ -e $oldLicensePath ]; then
      licenseProperties=""
      if licenseProperties=$(grep -x "$licensePropertiesRegex" "$oldLicensePath")
      then
        #get license value
        copyLicenseToNewLocation ${licenseProperties/$licensePropertyKey=/}
      fi
  fi
fi
