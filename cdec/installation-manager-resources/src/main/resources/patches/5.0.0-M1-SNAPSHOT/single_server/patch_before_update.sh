#!/bin/bash

# tells bash that it should exit the script if any statement returns value > 0
set -e

CURRENT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
LOG_FILE="$CODENVY_IM_BASE/migration.log"
rm -f $LOG_FILE

oldLicensePath=/usr/local/codenvy/im/storage/config.properties
licensePropertyKey="codenvy-license-key"
licensePropertiesRegex="$licensePropertyKey=[a-zA-Z0-9]*$"
newLicenseDirectoryPath=/home/codenvy/codenvy-data/license
newLicensePath="$newLicenseDirectoryPath/license"

createLicenseDirectoryPath() {
  sudo mkdir -p $newLicenseDirectoryPath
  sudo chown -R codenvy:codenvy $newLicenseDirectoryPath
}

copyLicenseToNewLocation() {
  license=$1

  #create new license file
  sudo touch $newLicensePath
  sudo chown -R codenvy:codenvy $newLicensePath

  #write license key to the new license file
  echo $license | sudo tee --append $newLicensePath &> /dev/null

  echo "License was successfully migrated from $oldLicensePath to $newLicensePath" >> $LOG_FILE
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

#### fix manifest > $machine_ws_agent_run_command property
# replace `$machine_ws_agent_run_command = "bla bla bla && sleep 5 && mkdir -p ~/che && rm -rf ~/che/* && unzip -q /mnt/che/ws-agent.zip -d ~/che/ws-agent && ~/che/ws-agent/bin/catalina.sh run"`
# on      `$machine_ws_agent_run_command = "bla bla bla && ~/che/ws-agent/bin/catalina.sh run"`

createFileBackup() {
    if [[ -n $1 ]]; then
        local currentTimeInMillis=$(($(date +%s%N)/1000000))
        sudo cp -f $1 $1.$currentTimeInMillis
    fi
}

createFileBackup "$PATH_TO_MANIFEST"

if [[ '$machine_ws_agent_run_command' =~ .*sleep.5.&&.mkdir.-p.~/che.&&.rm.-rf.~/che/*.&&.unzip.-q./mnt/che/ws-agent.zip.-d.~/che/ws-agent.&&.~/che/ws-agent/bin/catalina.sh.run ]]; then
    sudo sed -i 's|sleep 5 && mkdir -p ~/che && rm -rf ~/che/[*] && unzip -q /mnt/che/ws-agent.zip -d ~/che/ws-agent && ~/che/ws-agent/bin/catalina.sh run|~/che/ws-agent/bin/catalina.sh run|g' "$PATH_TO_MANIFEST" &>> $LOG_FILE
fi


#### fix mongoDB
CURRENT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
LOG_FILE="$CODENVY_IM_BASE/migration.log"

echo >> $LOG_FILE
echo "------ fix mongoDB :: agents -----" >> $LOG_FILE

mongo -u$mongo_admin_user_name -p$mongo_admin_pass --authenticationDatabase admin "${CURRENT_DIR}/update_agents.js" &>> $LOG_FILE

