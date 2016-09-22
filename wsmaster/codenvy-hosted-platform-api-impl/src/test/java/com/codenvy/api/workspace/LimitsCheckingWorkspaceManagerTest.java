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
package com.codenvy.api.workspace;

import com.codenvy.api.workspace.LimitsCheckingWorkspaceManager.WorkspaceCallback;
import com.google.common.collect.ImmutableList;

import org.eclipse.che.account.api.AccountManager;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.WorkspaceConfig;
import org.eclipse.che.api.environment.server.EnvironmentParser;
import org.eclipse.che.api.environment.server.compose.ComposeFileParser;
import org.eclipse.che.api.machine.server.spi.SnapshotDao;
import org.eclipse.che.api.machine.server.util.RecipeDownloader;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.api.user.server.model.impl.UserImpl;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.WorkspaceProjectStorageCleaner;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.Assert;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static com.codenvy.api.workspace.TestObjects.createConfig;
import static com.codenvy.api.workspace.TestObjects.createRuntime;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link LimitsCheckingWorkspaceManager}.
 *
 * @author Yevhenii Voevodin
 * @author Alexander Garagatyi
 */
@Listeners(MockitoTestNGListener.class)
public class LimitsCheckingWorkspaceManagerTest {
    @Mock
    private WorkspaceProjectStorageCleaner workspaceCleaner;
    @Mock
    SnapshotDao snapshotDao;

    @Mock
    RecipeDownloader recipeDownloader;

    @Mock
    AccountManager accountManager;

    ComposeFileParser composeFileParser = new ComposeFileParser();

    EnvironmentParser environmentParser = new EnvironmentParser(composeFileParser, recipeDownloader);

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "The maximum workspaces allowed per user is set to '2' and you are currently at that limit. " +
                                            "This value is set by your admin with the 'limits.user.workspaces.count' property")
    public void shouldNotBeAbleToCreateNewWorkspaceIfLimitIsExceeded() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2, // <- workspaces max count
                                                                                              "2gb",
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));
        doReturn(ImmutableList.of(mock(WorkspaceImpl.class), mock(WorkspaceImpl.class))) // <- currently used 2
                .when(manager)
                .getByNamespace(anyString());

        manager.checkCountAndPropagateCreation("user123", null);
    }

    @Test
    public void shouldNotCheckAllowedWorkspacesPerUserWhenItIsSetToMinusOne() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(-1, // <- workspaces max count
                                                                                              "2gb",
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));
        doReturn(ImmutableList.of(mock(WorkspaceImpl.class), mock(WorkspaceImpl.class))) // <- currently used 2
                                                                                         .when(manager)
                                                                                         .getByNamespace(anyString());
        final WorkspaceCallback callback = mock(WorkspaceCallback.class);

        manager.checkCountAndPropagateCreation("user123", callback);

        verify(callback).call();
        verify(manager, never()).getWorkspaces(any());
    }


    @Test
    public void shouldCallCreateCallBackIfEverythingIsOkayWithLimits() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2, // <- workspaces max count
                                                                                              "2gb",
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));
        doReturn(emptyList()).when(manager).getByNamespace(anyString()); // <- currently used 0

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkCountAndPropagateCreation("user123", callback);

        verify(callback).call();
    }

    @Test(expectedExceptions = LimitExceededException.class,
            expectedExceptionsMessageRegExp = "There are 1 running workspaces consuming 2GB RAM. Your current RAM " +
                                              "limit is 2GB. This workspaces requires an additional 1GB. You can stop other workspaces to free resources.")
    public void shouldNotBeAbleToStartNewWorkspaceIfRamLimitIsExceeded() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "2gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb

        manager.checkRamAndPropagateStart(createConfig("1gb"), null, "user123", null);
    }

    @Test
    public void shouldSkipWorkspacesRamCheckIfItIsSetToMinusOne() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "-1", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb
        final WorkspaceCallback callback = mock(WorkspaceCallback.class);

        manager.checkRamAndPropagateStart(createConfig("1gb"), null, "user123", callback);

        verify(callback).call();
        verify(manager, never()).getWorkspaces(any());
    }


    @Test
    public void shouldCallStartCallbackIfEverythingIsOkayWithLimits() throws Exception {
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));
        doReturn(singletonList(createRuntime("1gb", "1gb"))).when(manager).getByNamespace(anyString()); // <- currently running 2gb

        final WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkRamAndPropagateStart(createConfig("1gb"), null, "user123", callback);

        verify(callback).call();
    }

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "The maximum RAM per workspace is set to '2048mb' and you requested '3072mb'. " +
                                            "This value is set by your admin with the 'limits.workspace.env.ram' property")
    public void shouldNotBeAbleToCreateWorkspaceWhichExceedsRamLimit() throws Exception {
        final WorkspaceConfig config = createConfig("3gb");
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "2gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));

        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldNotCheckWorkspaceRamLimitIfItIsSetToMinusOne() throws Exception {
        final WorkspaceConfig config = createConfig("3gb");
        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "-1", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));

        manager.checkMaxEnvironmentRam(config);
    }

    @Test(expectedExceptions = LimitExceededException.class,
          expectedExceptionsMessageRegExp = "The maximum RAM per workspace is set to '2048mb' and you requested '2304mb'. " +
                                            "This value is set by your admin with the 'limits.workspace.env.ram' property")
    public void shouldNotBeAbleToCreateWorkspaceWithMultipleMachinesWhichExceedsRamLimit() throws Exception {
        final WorkspaceConfig config = createConfig("1gb", "1gb", "256mb");

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "2gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));
        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldBeAbleToCreateWorkspaceWithMultipleMachinesWhichDoesNotExceedRamLimit() throws Exception {
        final WorkspaceConfig config = createConfig("1gb", "1gb", "256mb");

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "3gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));
        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldCheckRamLimitOfCreatorUserInsteadOfCurrent() throws Exception {
        final UserManager userManager = mock(UserManager.class);
        final WorkspaceImpl ws = createRuntime("1gb", "1gb");
        final UserImpl user = new UserImpl("id", "email", ws.getNamespace());
        user.setName(ws.getNamespace());
        doReturn(user).when(userManager).getByName(eq(ws.getNamespace()));

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "2gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              userManager,
                                                                                              snapshotDao,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));

        doReturn(ws).when(manager).getWorkspace(anyString()); // <- currently running 2gb
        doReturn(ws).when(manager).checkRamAndPropagateStart(anyObject(), anyString(), anyString(), anyObject());

        manager.startWorkspace(ws.getId(), "envName", true);

        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        verify(manager).checkRamAndPropagateStart(anyObject(), anyString(), argument.capture(), anyObject());
        verify((WorkspaceManager)manager).startWorkspace(ws.getId(), "envName", true);
        Assert.assertEquals(argument.getValue(), ws.getNamespace());
    }

    @Test(expectedExceptions = ServerException.class,
          expectedExceptionsMessageRegExp = "Unable to start workspace .*, because its namespace owner is " +
                                            "unavailable and it is impossible to check resources consumption.")
    public void shouldPreventStartIfCreatorNotExistsAnymore() throws Exception {
        final UserManager userManager = mock(UserManager.class);
        final WorkspaceImpl ws = createRuntime("1gb", "1gb");
        doThrow(new NotFoundException("Nope")).when(userManager).getByName(eq(ws.getNamespace()));


        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "2gb", // <- workspaces ram limit
                                                                                              "1gb",
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              userManager,
                                                                                              snapshotDao,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner));
        doReturn(ws).when(manager).getWorkspace(anyString()); // <- currently running 2gb
        manager.startWorkspace(ws.getId(), null, false);
    }

    @Test
    public void shouldBeAbleToCreateWorkspaceWithMultipleMachinesIncludingMachineWithoutLimitsWhichDoesNotExceedRamLimit() throws Exception {
        final WorkspaceConfig config = createConfig("256mb", "256mb", null);

        final LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                              "3gb",
                                                                                              "3gb", // <- workspaces env ram limit
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              null,
                                                                                              environmentParser,
                                                                                              false,
                                                                                              false,
                                                                                              2000,
                                                                                              workspaceCleaner)); // <- default limit for machines without set limit
        manager.checkMaxEnvironmentRam(config);
    }

    @Test
    public void shouldBeAbleToStartWorkspaceWithMultipleMachinesIncludingMachineWithoutLimitsWhichDoesNotExceedRamLimit() throws Exception {
        LimitsCheckingWorkspaceManager manager = spy(new LimitsCheckingWorkspaceManager(2,
                                                                                        "3gb",
                                                                                        "1gb", // <- workspaces ram limit
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                        null,
                                                                                        environmentParser,
                                                                                        false,
                                                                                        false,
                                                                                        2000,
                                                                                        workspaceCleaner));
        doReturn(singletonList(createRuntime("256mb", "256mb", null))).when(manager)
                                                                      .getByNamespace(anyString()); // <- currently running 2gb

        WorkspaceCallback callback = mock(WorkspaceCallback.class);
        manager.checkRamAndPropagateStart(createConfig("256mb", "256mb", null), null, "user123", callback);

        verify(callback).call();
    }
}
