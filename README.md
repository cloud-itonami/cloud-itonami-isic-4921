# cloud-itonami-isic-4921

Open Business Blueprint for **ISIC Rev.5 4921**: urban and suburban
passenger land transport -- city bus, tram, light rail, and taxi/
rideshare dispatch coordination.

This repository publishes an urban/suburban-passenger-transit
SCHEDULING/DISPATCH LOGISTICS COORDINATION actor -- trip/ridership/
incident-report data logging, vehicle/route/timetable dispatch
scheduling, fleet maintenance procurement coordination, and vehicle-
defect/driver-fitness/route-hazard concern flagging -- as an OSS
business that any qualified transit operator can fork, deploy, run,
improve and sell, so an independent urban/suburban transit operator
never surrenders its dispatch-operations data to a closed back-office
SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, in-mem/Datomic checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **TransitDispatchAdvisor
⊣ UrbanTransitDispatchGovernor**. This blueprint's own
`:itonami.blueprint/governor` keyword, `:urban-transit-dispatch-governor`,
is a distinct, independent build (no naming-collision precedent
question -- confirmed distinct from sibling ISIC 4911's own
`:rail-safety-governor` and every other governor keyword registered
across the `cloud-itonami` org).

> **Why an actor layer at all?** An LLM is great at drafting a service-
> record summary, a dispatch-scheduling proposal, or a maintenance-
> order request -- but it has no license to actually finalize a
> dispatch-safety-clearance decision or determine a driver's fitness
> to drive, no way to independently confirm a route, a vehicle or an
> operator is actually a registered/verified party, and no notion of
> when a "flag this concern" op quietly turns into a claim to have
> already acted on it. Letting it act directly invites an unverified
> route entering the ledger, an unregistered vehicle or an unlicensed
> driver being scheduled into revenue passenger service, or -- worst
> of all -- a fabricated claim to have cleared a vehicle for departure
> despite a reported defect, putting passengers at real risk. This
> project seals the TransitDispatchAdvisor into a single node and
> wraps it with an independent **UrbanTransitDispatchGovernor**, a
> human **approval workflow**, and an immutable **audit ledger**.

## Scope: coordination only, never vehicle operation or safety-clearance authority

This actor is **scheduling/dispatch logistics coordination only**. It
never performs or authorizes:

- directly operating a vehicle
- overriding a driver's or dispatcher's safety judgment
- directly finalizing a dispatch-safety-clearance decision
- determining driver fitness-to-drive

The governor's `scope-exclusion-violations` check re-scans every
proposal for this failure mode independently of the advisor's own
framing, and treats it as a HARD, permanent block regardless of
confidence or how clean everything else is. Flagging a vehicle-defect/
driver-fitness/route-hazard concern for a human to triage is exactly
this actor's job -- `:flag-safety-concern` is never excluded by this
check, only FINALIZING/determining/clearing that concern is.

### Actuation

**Every proposal this actor generates is `:effect :propose`, never a
direct actuation.** Two independent layers enforce this
(`transitops.governor`'s `effect-not-propose-violations` HARD check
and `transitops.phase`'s phase table, which never puts
`:flag-safety-concern` in any phase's `:auto` set). A human dispatch/
operations coordinator is always the one who actually acts on a
flagged concern or confirms a high-cost maintenance order.

## The core contract

```
route/vehicle/operator registration + dispatch-coordination request
        |
        v
   ┌───────────────────────┐   proposal      ┌────────────────────────────┐
   │ TransitDispatch-       │ ─────────────▶ │ UrbanTransitDispatch-       │  (independent system)
   │ Advisor (sealed)       │  + citations    │ Governor                    │
   └───────────────────────┘                 │ route-unverified ·          │
          │                 commit ◀┼ vehicle-unverified (dispatch only) · │
          │                         │ operator-unverified (dispatch only)·│
    record + ledger        escalate ┼ effect-not-propose ·                │
          │              (ALWAYS for│ scope-excluded (dispatch-safety-    │
          │       :flag-safety-     │ clearance / driver-fitness          │
          │       concern/high-cost │ finalization) · op-not-allowed      │
          │       maintenance order)└────────────────────────────┘
          ▼
      human approval
```

**The TransitDispatchAdvisor never commits a proposal the
UrbanTransitDispatchGovernor would reject, and a safety-concern flag
or a high-cost maintenance order never commits without a human
sign-off.** Hard violations (an unregistered/unverified route; a
dispatch-scheduling proposal naming an unregistered/unverified vehicle
or operator; a non-`:propose` effect; content touching dispatch-
safety-clearance-finalization or driver-fitness-to-drive-determination;
an op outside the closed allowlist) force **hold** and *cannot* be
approved past.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
may perform physical domain work** (here: none directly -- this
vertical's physical work, driving a bus/tram/railcar/taxi, is
performed by a licensed human operator, not a robot) under human/robot
floor operations gated by transit-authority policy. This actor itself
does not dispatch robot/hardware actions and does not operate a
vehicle -- it is strictly the scheduling/dispatch-logistics
coordination layer (service-record logging, dispatch scheduling,
maintenance-order coordination, safety-concern flagging) any physical-
operations layer could eventually feed proposals into, always gated
the same way by the independent UrbanTransitDispatchGovernor.

## Features

- **Closed proposal-op allowlist**: `log-service-record`,
  `schedule-dispatch-operation`, `coordinate-maintenance-order`,
  `flag-safety-concern` (all `:effect :propose`).
- **Five HARD governor checks** (permanent, un-overridable):
  1. **Route unverified** -- the target route's own registration/
     license must exist AND be independently registered/verified in
     the store. Checked on ALL FOUR ops.
  2. **Vehicle unverified** -- for `:schedule-dispatch-operation` ONLY,
     the named vehicle must exist AND be independently registered/
     verified (current roadworthiness inspection) -- putting a
     vehicle into revenue passenger service is exactly the moment
     vehicle-worthiness matters most.
  3. **Operator unverified** -- for `:schedule-dispatch-operation`
     ONLY, the named operator/driver must exist AND be independently
     registered/verified (a current, valid operator/driver license).
     This actor never determines driver fitness-to-drive itself; only
     an external licensing authority does.
  4. **Effect is :propose** -- any other `:effect` value is rejected.
  5. **Scope exclusion** -- directly finalizing a dispatch-safety-
     clearance decision or determining driver fitness-to-drive, and an
     op outside the closed allowlist, are both permanently blocked.
- **Two ESCALATE (SOFT) gates**, either forces human sign-off:
  - `:flag-safety-concern` -- ALWAYS escalates, regardless of
    confidence or phase. A "flag a concern" op is never auto-commit
    eligible and never finalizes a dispatch-safety-clearance decision
    or a driver-fitness-to-drive determination itself -- it only
    surfaces the concern for a human.
  - `:coordinate-maintenance-order` above a cost threshold -- a
    large-value fleet-maintenance procurement proposal always needs a
    human sign-off.
  - (LLM confidence below the floor also escalates, as with every
    sibling actor.)
- **Staged rollout** (Phase 0→3):
  - Phase 0: read-only
  - Phase 1: service-record logging only (approval-gated)
  - Phase 2: + dispatch-scheduling, maintenance-order proposals
    (approval-gated)
  - Phase 3: auto-commits clean, high-confidence, low-cost proposals
    (safety concerns and high-cost maintenance orders always escalate)
- **Append-only audit ledger** -- every decision is an immutable log
  entry.
- **langgraph-clj StateGraph** -- one request = one supervised run;
  human-in-the-loop via `interrupt-before`.

### Development

```bash
# Install dependencies (if inside the superproject, use :dev alias for local overrides)
clojure -M:dev -P

# Run tests
clojure -M:test

# Run linter
clojure -M:lint

# Run demo
clojure -M:run
```

### Test suite

- `test/transitops/governor_test.clj` -- unit tests of governor hard
  checks, scope exclusion, and the self-trip regression test
- `test/transitops/advisor_test.clj` -- advisor proposal shape and
  consistency
- `test/transitops/phase_test.clj` -- rollout phase logic
- `test/transitops/governor_contract_test.clj` -- full graph
  integration, audit trail
- `test/transitops/store_contract_test.clj` -- Store protocol and
  MemStore implementation

### Modules

- `transitops.store` -- SSoT (MemStore, String-keyed route/vehicle/
  operator directories, append-only ledger)
- `transitops.advisor` -- contained intelligence node (mock +
  real-LLM seam)
- `transitops.governor` -- independent compliance layer
- `transitops.phase` -- staged rollout (0→3)
- `transitops.operation` -- langgraph-clj StateGraph
- `transitops.sim` -- demo driver

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`4921`).

## Business-process coverage (honest)

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Trip/ridership/incident-report data logging (`:log-service-record`) | Real AVL/ITS fleet-telemetry integration |
| Vehicle/route/timetable dispatch scheduling coordination, HARD-gated on independent route/vehicle/operator verification (`:schedule-dispatch-operation`) | Real dispatch-console/signal-priority-system integration |
| Fleet maintenance procurement coordination (`:coordinate-maintenance-order`) | Real maintenance-management-system (EAM) integration |
| Vehicle-defect/driver-fitness/route-hazard concern flagging, ALWAYS human-gated (`:flag-safety-concern`) | Directly finalizing any dispatch-safety-clearance decision or driver-fitness-to-drive determination -- permanently out of scope, not a gap |
| Immutable audit ledger for every log/schedule/order/flag decision | Fare collection / farebox reconciliation -- a follow-up slice, not in this R0 |

Extending coverage is additive: add the next op (e.g. a passenger-
accessibility-accommodation request or a service-disruption-
notification check) as its own governed op with its own HARD checks
and tests, following the SAME "an independent governor re-verifies
against the actor's own records before any real-world act" pattern
this repo's flagship checks already establish.

## Maturity

`:implemented` -- `TransitDispatchAdvisor` + `UrbanTransitDispatchGovernor`
run as real, tested code (see `Development` above), following the SAME
governed-actor architecture as every prior actor across this fleet,
with its own distinct, independently-named governor and its own
three-entity (route/vehicle/operator) verification chain.

## License

Code and implementation templates are AGPL-3.0-or-later.
