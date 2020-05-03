const request = require('request');
const firebase = require('firebase-admin');

const config = require('./config');
const hm = require('./here_maps_wrapper');
const serviceAccount = require("./serviceAccount.json");
firebase.initializeApp({
  credential: firebase.credential.cert(serviceAccount),
  databaseURL: "https://.firebaseio.com"
});


var getTime = (buslat, buslng, stoplat, stoplng) => {
  var startLatLng = `${buslat},${buslng}`;
  var endLatLng = `${stoplat},${stoplng}`;

  return new Promise((resolve, reject) => {
    hm.configure(config.here_maps_config).then(() => {
      hm.requestSimpleString(startLatLng, endLatLng).then(
        (body) => {
          resolve(Math.round(body.response.route[0].summary.trafficTime / 60));
        }, (err) => {
          reject(err);
        });
    }, (err) => reject(err));
  });
};

var sendRequest = (buslat, buslng, stoplat, stoplng, busStopName) => {
  if (buslat === null || buslng === null || stoplat === null || stoplng === null || busStopName === null)
    return null;
  return new Promise((resolve, reject) => {
    getTime(buslat, buslng, stoplat, stoplng).then((time) => {
      if (time <= 100) {
        var message = {
          notification: {
            title: `Your Bus is Arriving Soon`,
            body: `Your Bus is arriving in approximately ${time} minutes`
          },
          condition: `\'bus${busStopName}time${time}\' in topics`
        };
        firebase.messaging().send(message).then((response) => {
          // Response is a message ID string.
          resolve(response);
          console.log('Successfully sent message:', response);
          firebase.database().goOffline();
        })
          .catch((error) => {
            console.log('Error sending message:', error);
            firebase.database().goOffline();
          });
      }
    }).catch((err) => reject(err));
  });
};


// getTime("49.9399807,-119.395521", "49.9081381,-119.3917857").then((trafficTime) => {
//     console.log(trafficTime);
// }, (err) => {
//     console.log(err)
// });

// sendRequest("49.9399807,-119.395521", "49.9081381,-119.3917857", "UBCOA").then(() => {
//   console.log("success");
//   process.exit();
// }).catch((err) => {
//   console.log(err);
//   process.exit();
// });



module.exports = {
  getTime,
  sendRequest
};
