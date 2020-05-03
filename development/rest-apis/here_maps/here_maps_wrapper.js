const request = require('request');
// const config = request('./config');
var id, code;
var configure = (configuration) => {
    return new Promise((resolve, reject) => {
        if(configuration.app_id && configuration.app_code){
            id = configuration.app_id;
            code = configuration.app_code;
            resolve();
        } else {
            reject("Error");
        }
    });
};
var requestSimpleString = (waypoint0, waypoint1) => {
    return new Promise((resolve, reject) => {
    request({url: `https://route.api.here.com/routing/7.2/calculateroute.json?app_id=${id}&app_code=${code}&waypoint0=geo!${waypoint0}&waypoint1=geo!${waypoint1}&mode=fastest;car;traffic:enabled`, json:true}, (error, response, body) => {
            if(!error){
                resolve(body);
            } else {
                reject(error);
            }
    });
});
};


module.exports = {
    configure,
    requestSimpleString
};

// https://route.api.here.com/routing/7.2/calculateroute.json
// ?app_id={YOUR_APP_ID}
// &app_code={YOUR_APP_CODE}
// &waypoint0=geo!52.5,13.4
// &waypoint1=geo!52.5,13.45
// &mode=fastest;car;traffic:disabled