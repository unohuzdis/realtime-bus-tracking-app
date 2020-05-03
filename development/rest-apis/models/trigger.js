var mongoose = require('mongoose');

var Trigger = mongoose.model('Trigger', {
    userId: {
        type: String
    },
    tripId: {
        type: Number
    },
    beaconTrigger: {
        type: String,
        required: true,
        trim: true,
        minlength: 1
    },
    busId: {
        type: String
    },
    startedAt: {
        type: Date,
        required: true
    },
    completedAt: {
        type: Date,
        default: null
    },
    startLocation: {
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
    endLocation: {
        type: {
            type: String, // Don't do `{ location: { type: String } }`
            enum: ['Point'], // 'location.type' must be 'Point'
            default: 'Point'
        },
        coordinates: {
            type: [Number],
            default: null
        }
    }
});

module.exports = {Trigger}
