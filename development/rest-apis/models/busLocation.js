var mongoose = require('mongoose');

var BusLocation = mongoose.model('BusLocation', {
    busId: {
        type: String
    },
    location: {
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
    timestamp:{
        type: Date,
        required: true
    }
});

module.exports = {BusLocation}
