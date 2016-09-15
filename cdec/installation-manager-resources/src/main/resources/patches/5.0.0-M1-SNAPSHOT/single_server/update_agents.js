// issue https://github.com/codenvy/codenvy/issues/680

// databases
var organization = db.getSiblingDB("organization");
var factory      = db.getSiblingDB("factory");

// collections
var workspaces = organization.getCollection("workspaces2");
var stacks     = organization.getCollection("stacks");
var recipes    = organization.getCollection("recipes");
var factories  = factory.getCollection("factory");

var isDotEscaped;

// migration for workspaces
function migrateWorkspaces() {
  print("Migrating workspaces");
  isDotEscaped = false;
  workspaces.find({ "config" : { $exists : true } }).forEach(function(workspace) {
    workspace.config = adaptConfig(workspace.config, workspace._id);
    workspaces.update({ "_id" : workspace._id }, workspace);
  })
}

// migration for stacks
function migrateStacks() {
  print("Migrating stacks")
  isDotEscaped = false;
  // remove bad stack, if exists
  var aspStack = stacks.findOne({"_id" : "aspnet-default"});
  if (aspStack && !findDefaultDevMachine(aspStack.workspaceConfig)) {
    print("Found not-needed stack 'aspnet-default'")
    stacks.remove(aspStack);
    print("Stack 'aspnet-default' was successfully removed")
  }

  // do general migration
  stacks.find({ "workspaceConfig" : { $exists : true } }).forEach(function(stack) {
    adaptStackSource(stack);
    stack.workspaceConfig = adaptConfig(stack.workspaceConfig, stack._id);
    stacks.update({ "_id" : stack._id }, stack);
  })
}

// migration for factories
function migrateFactories() {
  print("Migrating factories");
  isDotEscaped = true;
  factories.find({ "factory.workspace" : { $exists : true } }).forEach(function(f) {
    f.factory.workspace = adaptConfig(f.factory.workspace, f._id);
    factories.update({ "_id" : f._id}, f)
  });
}

function adaptStackSource(stack) {
  if (stack.source && stack.source.type) {
    var devMachine = findDefaultDevMachine(stack.workspaceConfig);
    switch (stack.source.type) {
      case "image" :
        devMachine.source.type = "image";
        devMachine.source.location = stack.source.origin;
        break;
      case "location" :
        devMachine.source.type = "dockerfile";
        devMachine.source.location = stack.source.origin;
        break;
      case "dockerfile":
      case "recipe" :
        devMachine.source.type = "dockerfile";
        devMachine.source.content = stack.source.origin;
        break;
    }
  }
}

function adaptConfig(oldConfig) {
  var environments = oldConfig.environments;
  var newEnvironments = {}

  // adapt environments
  environments.forEach(function(environment) {
    var devMachine = findDevMachine(environment);
    if (devMachine && devMachine.source && devMachine.source.type) {
      var source = devMachine.source;
      var newRecipe = {}
      switch (devMachine.source.type) {
        case "recipe":
        case "dockerfile":
          newRecipe.type = "dockerfile";
          newRecipe.contentType = "text/x-dockerfile";
          if (source.content != undefined && source.content != null) {
            newRecipe.content = source.content;
          } else if (source.location != undefined && source.location != null) {
            newRecipe.location = source.location;
          }
          break;
        case "image":
          newRecipe.type = "dockerimage";
          newRecipe.location = source.location;
          break;
      }

      var newEnv = { "recipe" : newRecipe, "machines" : {} };
      environment.machineConfigs.forEach(function(machine) {
        newEnv.machines[normalizeName(machine.name)] = adaptMachine(machine);
      });
      newEnvironments[normalizeName(environment.name)] = newEnv
    }
  })

  oldConfig.environments = newEnvironments;
  oldConfig.defaultEnv = normalizeName(oldConfig.defaultEnv)
  return oldConfig;
}

function adaptMachine(machine) {
  var newMachine = {};

  // agents
  if (machine.dev || machine.isDev) {
    newMachine.agents = [ "org.eclipse.che.terminal", "org.eclipse.che.ws-agent", "org.eclipse.che.ssh"];
  }

  // ram
  if (machine.limits && machine.limits.ram) {
    if (isDotEscaped) {
      newMachine.attributes = {
        "memoryLimitBytes": machine.limits.ram * 1024 * 1024 + ""
      }
    } else {
      newMachine.attributes = [
        {
          "name": "memoryLimitBytes",
          "value": machine.limits.ram * 1024 * 1024 + ""
        }
      ]
    }
  }

  // servers
  if (machine.servers && machine.servers.length > 0) {
    var oldServers = machine.servers;
    var servers = {};
    oldServers.forEach(function(server) {
      var newServer = {};
      if (server.ref) {
        newServer.port = server.port;
        newServer.protocol = server.protocol;
        if (server.path) {
          newServer.properties = {
            "path" : server.path
          }
        }
        servers[normalizeName(server.ref)] = newServer
      };
    });
  }
  return newMachine;
}

function findDefaultDevMachine(config) {
  if (config) {
    var envs = config.environments || [];
    for(var envIdx in envs) {
      env = envs[envIdx];
      if (env.name === config.defaultEnv) {
        return findDevMachine(env);
      }
    }
  }
}

function findDevMachine(env) {
  var machineConfigs = env.machineConfigs || [];
  for (var configIdx in machineConfigs) {
    var machineCfg = machineConfigs[configIdx];
    if (machineCfg.dev || machineCfg.isDev) {
      return machineCfg;
    }
  }
}

function normalizeName(name) {
  return (name || "").split(".").join("_");
}

migrateWorkspaces();
migrateStacks();
migrateFactories();
