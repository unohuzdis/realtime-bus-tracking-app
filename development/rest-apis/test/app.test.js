const expect = require('expect');
const request = require('supertest');

const {app} = require('./../app');
const {Trigger} = require('./../models/trigger');

beforeEach((done) => {
    Trigger.remove({}).then(() => done());
});

describe('POST /triggers', () => {
    it('should create a new trigger', (done) => {
        var beaconTrigger = 'sd8293923hd83da';

        request(app)
            .post('/triggers')
            .send({beaconTrigger})
            .expect(200)
            .expect((res) => {
            expect(res.body.beaconTrigger).toBe(beaconTrigger);
        })
            .end((err, res) => {
            if (err) {
                return done(err);
            }

            Trigger.find().then((triggers) => {
                expect(triggers.length).toBe(1);
                expect(triggers[0].beaconTrigger).toBe(beaconTrigger);
                done();
            }).catch((e) => done(e));
        });
    });

    it('should not create trigger with invalid body data', (done) => {
        request(app)
            .post('/triggers')
            .send({})
            .expect(400)
            .end((err, res) => {
            if (err) {
                return done(err);
            }

            Trigger.find().then((triggers) => {
                expect(triggers.length).toBe(0);
                done();
            }).catch((e) => done(e));
        });
    });
});
