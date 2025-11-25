const { getDefaultConfig } = require('expo/metro-config');
const path = require('path');

const projectRoot = __dirname;
const workspaceRoot = path.resolve(projectRoot, '..');

const config = getDefaultConfig(projectRoot);

// Watch the parent module directory for changes
config.watchFolders = [workspaceRoot];

// Resolve the parent module properly
config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, 'node_modules'),
  path.resolve(workspaceRoot, 'node_modules'),
];

// Ensure Metro can follow symlinks
config.resolver.extraNodeModules = {
  '@amehmeto/tied-siren-blocking-overlay': workspaceRoot,
};

module.exports = config;
