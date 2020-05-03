var serverless = require('serverless-http');
var {app} = require('./app');

module.exports.handler = serverless(app);
