var mongoose = require('mongoose');

var TripRequest = mongoose.model('TripRequest', {
    userId: {
        type: String
    },
    tripId: {
        type: Number
    },
    busId: {
        type: String
    },
    busStop: {
        type: String,
        required: true
    },
    stopLocation: {
        type: {
            type: String, // Don't do `{ location: { type: String } }`
            enum: ['Point'], // 'location.type' must be 'Point'
            required: true
        },
        coordinates: {
            type: [Number],
            required: true
        }
    },
    requestedTime: {
        type: Date,
        required: true
    },
    reminderTime: {
        type: Number,
        required: true
    }
});

module.exports = {TripRequest}
