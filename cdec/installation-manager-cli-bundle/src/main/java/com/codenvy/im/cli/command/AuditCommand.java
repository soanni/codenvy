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
package com.codenvy.im.cli.command;
import com.codenvy.im.cli.preferences.PreferenceNotFoundException;
import com.google.common.io.Files;

import org.apache.karaf.shell.commands.Command;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * @author Igor Vinokur
 */


@Command(scope = "codenvy", name = "audit", description = "Print the list of available latest versions and installed ones")
public class AuditCommand extends AbstractIMCommand {

    @Override
    protected void doExecuteCommand() throws IOException {
        try {
            getFacade().generateAuditReport(getCodenvyOnpremPreferences().getAuthToken(), getCodenvyOnpremPreferences().getUrl());
        } catch (PreferenceNotFoundException e) {
            getConsole().printErrorAndExit("Please, login into Codenvy");
            return;
        }
        List<String> lines = Files.readLines(new File("/home/vagrant/codenvy/audit/report.txt"), Charset.defaultCharset());
        lines.forEach(line -> getConsole().print(line + "\n"));
    }
}
