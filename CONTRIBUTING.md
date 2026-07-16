# Contributing

`cloud-itonami-isic-4921` accepts contributions to the OSS blueprint,
capability bindings, policy tests, documentation and operator model.

## Development

```bash
clojure -M:test
clojure -M:lint
```

## Rules
- Do not commit real passenger, driver, vehicle or safety-incident
  data.
- Keep service-record logging, dispatch scheduling, maintenance-order
  coordination and safety-concern flagging behind the
  UrbanTransitDispatchGovernor.
- Treat transit-dispatch workflows as high-risk (passenger-safety
  dimension): add tests for route/vehicle/operator verification,
  effect discipline, scope exclusion, escalation and audit logging.
- Never phrase a governor scope-exclusion term as a bare noun (e.g.
  "safety", "dispatch", "fitness") -- phrase it as the finalization/
  execution ACTION (e.g. "finalized the dispatch-safety clearance",
  "determined driver fitness to drive"), and add/extend the
  `default-mock-advisor-proposals-never-self-trip-scope-exclusion`
  regression test for any new term. A bare-noun term will self-trip
  this actor's own legitimate `:flag-safety-concern` happy path -- see
  `transitops.governor/scope-excluded-terms`'s docstring.
- Never add an op that directly finalizes a dispatch-safety-clearance
  decision or a driver-fitness-to-drive determination to the closed
  op-allowlist. This actor is scheduling/dispatch logistics
  coordination ONLY.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests
PRs should describe: what behavior changed, which policy invariant is
affected, how it was tested, whether operator or certification docs need
updates.
