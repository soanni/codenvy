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
package com.codenvy.workspace;

import com.codenvy.machine.backup.WorkspaceIdHashLocationFinder;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.ListLineConsumer;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.workspace.server.WorkspaceProjectStorageCleaner;
import org.eclipse.che.commons.lang.concurrent.ThreadLocalPropagateContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Component to clean up workspace project storage which can be located in the remote node.
 *
 * @author Alexander Andrienko
 */
@Singleton
public class WorkspaceProjectStorageCleanerImpl implements WorkspaceProjectStorageCleaner {

    private static final Logger LOG = LoggerFactory.getLogger(WorkspaceProjectStorageCleanerImpl.class);

    private final WorkspaceIdHashLocationFinder workspaceIdHashLocationFinder;
    private final File                          backupsRootDir;
    private final int                           cleanUpTimeOut;
    private final String                        workspaceCleanUpScript;
    private final ExecutorService               executor;

    @Inject
    public WorkspaceProjectStorageCleanerImpl(WorkspaceIdHashLocationFinder workspaceIdHashLocationFinder,
                                              @Named("che.user.workspaces.storage") File backupsRootDir,
                                              @Named("workspace.cleanup.cleanup_workspace_storage_script") String workspaceCleanUpScript,
                                              @Named("workspace.cleanup.cleanup_duration_second") int cleanUpTimeOut) {
        this.workspaceIdHashLocationFinder = workspaceIdHashLocationFinder;
        this.backupsRootDir = backupsRootDir;
        this.workspaceCleanUpScript = workspaceCleanUpScript;
        this.cleanUpTimeOut = cleanUpTimeOut;
        executor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("WorkspaceProjectStorageCleanerImpl-%d")
                                                                           .setDaemon(true)
                                                                           .build());
    }

    @Override
    public void remove(String workspaceId) {
        File backupWorkspaceFolder = workspaceIdHashLocationFinder.calculateDirPath(backupsRootDir, workspaceId);

        CommandLine commandLine = new CommandLine(workspaceCleanUpScript, backupWorkspaceFolder.getAbsolutePath());

        executor.execute(ThreadLocalPropagateContext.wrap(() -> {
            try {
                execute(commandLine.asArray(), cleanUpTimeOut);
            } catch (TimeoutException | IOException | InterruptedException e) {
                LOG.error("Failed to delete folder for workspace with id: {}. Cause: {}", workspaceId, e.getMessage());
            }
        }));
    }

    @VisibleForTesting
    void execute(String[] commandLine, int timeout) throws TimeoutException, IOException, InterruptedException {
        final ListLineConsumer outputConsumer = new ListLineConsumer();
        Process process = ProcessUtil.execute(commandLine, timeout, SECONDS, outputConsumer);

        if (process.exitValue() != 0) {
            LOG.error(outputConsumer.getText());
            throw new IOException("Process failed. Exit code " + process.exitValue());
        }
    }
}
