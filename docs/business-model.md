# Business Model: Urban and Suburban Passenger Land Transport Dispatch Coordination

## Classification
- Repository: `cloud-itonami-isic-4921`
- ISIC Rev.5: `4921` -- urban and suburban passenger land transport
  (city bus, tram, light rail, taxi/rideshare dispatch)
- Social impact: public-transit access, road safety, regional
  connectivity

## Customer
- independent urban/suburban transit operators (bus, tram, light rail,
  taxi/rideshare dispatch) needing an auditable dispatch-coordination
  platform
- multi-route operators needing consistent dispatch-scheduling/
  maintenance/safety governance across routes and depots
- programs that cannot accept closed, unauditable dispatch-operations
  platforms

## Offer
- trip/ridership/incident-report data logging
- vehicle/route/timetable dispatch scheduling coordination
- fleet maintenance procurement coordination
- vehicle-defect/driver-fitness/route-hazard concern flagging for
  human triage
- role-based access and immutable audit ledger

## Revenue
- self-host setup fee
- managed hosting subscription per route/depot
- support retainer with SLA

## Trust Controls
- `:urban-transit-dispatch-governor` never lets a proposal for an
  unregistered/unverified route, or a dispatch-scheduling proposal
  naming an unregistered/unverified vehicle or operator, commit or
  even escalate
- every proposal's `:effect` must be `:propose` -- a claim to directly
  actuate is a HARD, un-overridable block
- directly finalizing a dispatch-safety-clearance decision or
  determining driver fitness-to-drive is permanently out of scope, not
  a rollout milestone -- the actor may only flag a concern for a human
- a `:flag-safety-concern` proposal, and a high-cost
  `:coordinate-maintenance-order`, always require human sign-off
- sensitive passenger, driver and vehicle data stays outside Git
