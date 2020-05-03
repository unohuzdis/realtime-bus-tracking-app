//var serverless = require('serverless-http');
const cors = require("cors");
const express = require('express');
const bodyParser = require('body-parser');
const _ = require('underscore');
const {ObjectID} = require('mongodb');

var {getTime} = require('./here_maps/here_maps_trigger');
var {sendRequest} = require('./here_maps/here_maps_trigger');
//var moment = require('moment');

var {mongoose} = require('./db/mongoose');

var {Trigger} = require('./models/trigger');
var {BusLocation} = require('./models/busLocation');
var {TripRequest} = require('./models/tripRequest');

var app = express();

app.use(cors());

// parse application/x-www-form-urlencoded
app.use(bodyParser.urlencoded({ extended: false }));

app.use(bodyParser.json());

// Gets user location and timestamp when they get on the bus
app.post('/triggers', (req, res) => {
    var trigger = new Trigger({
        userId: req.body.userId,
        tripId: req.body.tripId,
        beaconTrigger: req.body.beaconTrigger,
        busId: req.body.busId,
        startedAt: new Date().getTime() ,
        startLocation: req.body.startLocation     
    });

    trigger.save().then((doc) => {
        res.send(doc);
    }, (e) => {
        res.status(400).send(e);
    });
});

// Gets user location and timestamp when they get off the bus
app.patch('/triggers/:id', (req, res) => {
    var id = req.params.id;
    var body = _.pick(req.body, 'endLocation' );

    if (!ObjectID.isValid(id)) {
        return res.status(404).send('Please provide correct id');
    }

    if (body.endLocation.coordinates) {
        body.completedAt = new Date().getTime();

        Trigger.findByIdAndUpdate(id, {$set: body}, {new: true}).then((newTrigger) => {
            if (!newTrigger) {
                return res.status(404).send();
            }

            res.send({newTrigger});
        }).catch((e) => {
            res.status(400).send(e);
        })
    } else {
        return res.status(404).send('Please check your coordinates');
    }

});


//starts sending bus location
app.post('/buslocation', (req, res) => {
    var busLocation = new BusLocation({
        busId: req.body.busId,
        location: {
            type: req.body.location.type,
            coordinates: req.body.location.coordinates
        },
        timestamp: new Date().getTime()
    });



    busLocation.save().then((doc) => {
        res.send(doc);
    }, (e) => {
        res.status(400).send(e);
    });
});

//When a user makes a trip request (clicks on track button from the client side)
app.post('/triprequest', (req, res) => {
    var tripRequest = new TripRequest({
        userId: req.body.userId,
        tripId: req.body.tripId,
        busId: req.body.busId,
        busStop: req.body.busStop,
        stopLocation: {
            type: req.body.stopLocation.type,
            coordinates: req.body.stopLocation.coordinates
        },
        requestedTime: new Date().getTime(),
        reminderTime: req.body.reminderTime  
    });

    tripRequest.save().then((doc) => {
        res.send(doc);
    }, (e) => {
        res.status(400).send(e);
    });
});

// To get the latest bus location
app.get('/buslocation', (req, res) => {
    BusLocation.find().sort({timestamp:-1}).limit(1).then((buslocation) => {
        res.send({buslocation});
        //    console.log(buslocation);
    }, (e) => {
        res.status(400).send(e);
    });
});

// To check if there's an user at a certain bus stop
// checking if there's a request in last 100 mins
app.get('/triprequest', (req, res) => {
    TripRequest.find({
        requestedTime : 
        { $gte :  new Date(new Date().getTime() - 1000 * 60 * 100) }
    }).then((tripreqs) => {
        const uniqueLocations = _.uniq(tripreqs, (unique) => unique.busStop);

        res.send({uniqueLocations});
    }, (e) => {
        res.status(400).send(e);
    });
});

//returns individual bus stop info, if a passenger has planned a trip in last 100 mins
app.get('/triprequest/:busstop', (req, res) => {

    TripRequest.find({
        requestedTime : 
        { $gte :  new Date(new Date().getTime() - 1000 * 60 * 100) }
    }).then((tripreqs) => {

        const passengerWaiting = tripreqs.filter((user) => {
            var requestedStop = null;

            if (req.params.busstop == "ubco-a"){
                requestedStop = "UBCO A";
            } else if (req.params.busstop == "ubco-b"){
                requestedStop = "UBCO B";
            }

            return user.busStop === requestedStop;

        });
        //        console.log('reqs', passengerWaiting);

        res.send({passengerWaiting});
    }).catch((e) => {
        res.status(400).send(e);
    })
});

// Get latest bus ETA
app.get('/bustime/:busstop', (req, res) => {

    BusLocation.find().sort({ timestamp: -1 }).limit(1).then((buslocation) => {
        var busLong = buslocation[0].location.coordinates[0];
        var busLat = buslocation[0].location.coordinates[1];
        
        var stopLong = (req.params.busstop == "ubco-a") ? "-119.394334" : "-119.401581";
        var stopLat = (req.params.busstop == "ubco-a") ? "49.939073" : "49.934023";
        
        getTime(busLat, busLong, stopLat, stopLong).then((trafficTime) => {
//            console.log(trafficTime);
            res.send({trafficTime});
        }, (err) => {
            console.log(err)
        });

    }, (e) => {
        res.status(400).send(e);
    });
});

//updates bus location
app.patch('/buslocation/:id', (req, res) => {
    var id = req.params.id;
    var body = _.pick(req.body, 'location');
    //    console.log('bd', body.location.coordinates);
    // TODO: add trigger Here
    var busLong = buslocation[0].location.coordinates[0];
    var busLat = buslocation[0].location.coordinates[1];

    var stopLong = "-119.394334";
    var stopLat = "49.939073";

    sendRequest(busLat, busLong, stopLat, stopLong, "ubcoa");

    var stopLong = "-119.401581";
    var stopLat = "49.934023";
    sendRequest(busLat, busLong, stopLat, stopLong, "ubcob")

    if (!ObjectID.isValid(id)) {
        return res.status(404).send('Please provide correct id');
    }

    if (body.location.coordinates) {
        body.timestamp = new Date().getTime();
        
        BusLocation.findByIdAndUpdate(id, {$set: body}, {new: true}).then((newLocation) => {
            if (!newLocation) {
                return res.status(404).send();
            }
            res.send({newLocation});
        }).catch((e) => {
            res.status(400).send(e);
        })
    } else {
        return res.status(404).send('Please check your coordinates');
    }

});

//Cancels a trip request
app.delete('/triprequest/:id', (req, res) => {
    var id = req.params.id;

    if (!ObjectID.isValid(id)) {
        return res.status(404).send('Please provide correct id');
    }

    TripRequest.findByIdAndRemove(id).then((trip) => {
        if (!trip) {
            return res.status(404).send('The trip request was already deleted or cannot be found');
        }

        res.send({trip});
    }).catch((e) => {
        res.status(400).send(e);
    });
});


//app.listen(3000, () => {
//    console.log('Started on port 3000');
//});

module.exports = {app};

//module.exports.handler = serverless(app);
