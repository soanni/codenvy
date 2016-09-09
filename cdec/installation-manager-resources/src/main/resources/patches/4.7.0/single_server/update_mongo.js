var fixWorkspaces = function() {
    var organizationDb = db.getSiblingDB('organization');   
    var wsFound = organizationDb.workspaces2.find({"config.environments.machineConfigs.isDev": { $exists: true }}).count();
    var wsUpdated = 0;
    organizationDb.workspaces2.find({"config.environments.machineConfigs": { $exists: true }}).snapshot().forEach(
        function(doc) {
        doc.config.environments.forEach(
                function(env) {
                    env.machineConfigs.forEach(
                        function(config) {
                            if (typeof config.isDev !== 'undefined') {
                            config.dev = config.isDev;
                            delete config.isDev;
                                wsUpdated++;
                            }
                        }
                    )
                }
            )
            organizationDb.workspaces2.save(doc);
        }
    )
    
    print("Workspaces: Workspaces with 'isDev' field: " + wsFound)
    print("Workspaces: Workspace configs updated from 'isDev' to 'dev': " + wsUpdated);
    if (wsFound = organizationDb.workspaces2.find({"config.environments.machineConfigs.isDev": { $exists: true }}).count() > 0) {
        print("Workspaces :WARNING, there are configs left with isDev field: " + wsFound + "!")
    }
}

var fixStacks = function() {
    var organizationDb = db.getSiblingDB('organization');
    var stacksFound = organizationDb.stacks.find({"workspaceConfig.environments.machineConfigs.isDev": { $exists: true }}).count();
    var stacksUpdated = 0;
    organizationDb.stacks.find({"workspaceConfig.environments.machineConfigs": { $exists: true }}).snapshot().forEach(
        function(doc) {
        doc.workspaceConfig.environments.forEach(
                function(env) {
                    env.machineConfigs.forEach(
                        function(config) {
                            if (typeof config.isDev !== 'undefined') {
                            config.dev = config.isDev;
                            delete config.isDev;
                                stacksUpdated++;
                            }
                        }
                    )
                }
            )
            organizationDb.stacks.save(doc);
        }
    )

    print("Stacks: Stacks with 'isDev' field: " + stacksFound)
    print("Stacks: Workspace configs updated from 'isDev' to 'dev': " + stacksUpdated);
    if (stacksFound = organizationDb.stacks.find({"workspaceConfig.environments.machineConfigs.isDev": { $exists: true }}).count() > 0) {
        print("Stacks: WARNING, there are configs left with isDev field: " + stacksFound + "!")
    }
}

var fixFactories = function() {
    var factoryDb = db.getSiblingDB('factory');
    var factoryFound = factoryDb.factory.find({"factory.workspace.environments.machineConfigs.isDev": { $exists: true }}).count();
    var factoryUpdated = 0;
    factoryDb.factory.find({"factory.workspace.environments.machineConfigs": { $exists: true }}).snapshot().forEach(
        function(doc) {
        doc.factory.workspace.environments.forEach(
                function(env) {
                    env.machineConfigs.forEach(
                        function(config) {
                            if (typeof config.isDev !== 'undefined') {
                            config.dev = config.isDev;
                            delete config.isDev;
                                factoryUpdated++;
                            }
                        }
                    )
                }
            )
            factoryDb.factory.save(doc);
        }
    )

    print("Factory: Factories with 'isDev' field: " + factoryFound)
    print("Factory: Workspace configs updated from 'isDev' to 'dev': " + factoryUpdated);
    if (factoryFound = factoryDb.factories.find({"factory.workspace.environments.machineConfigs.isDev": { $exists: true }}).count() > 0) {
        print("Factory: WARNING, there are configs left with isDev field: " + factoryFound + "!")
    }
}

fixWorkspaces();
fixStacks();
fixFactories();
