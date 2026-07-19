# Operator Quickstart: Urban and suburban passenger land transport dispatch coordination

## Prerequisites

1. **Clojure CLI** (`clojure` ≥ 1.11.0). [Install here](https://clojure.org/guides/install_clojure).
2. **If running inside the monorepo**: sibling paths in `deps.edn`'s `:dev` alias resolve `langgraph`/`langchain` via `:local/root`. A standalone fork should drop the `:dev` alias override and pin `io.github.kotoba-lang/langgraph` (already the root `:deps` entry) via its own Git coordinates.
3. **A text editor** for reading `src/transitops/*.cljc` as you explore.

## Run the demo

Walk through the full disposition set — clean auto-commits, an always-escalating safety-concern flag, a high-cost maintenance order requiring sign-off, and four distinct HARD-hold reasons (unregistered route, registered-but-unverified route, unverified vehicle, unverified operator):

```bash
clojure -M:run
```

The demo driver (`src/transitops/sim.cljc`) shows the OperationActor, the Urban Transit Dispatch Governor, and how a safety-concern flag or a high-cost maintenance order never auto-commits.

## Regenerate the live operator console

The same actor stack, driven through a trimmed scenario, renders
`docs/samples/operator-console.html` (published via GitHub Pages,
regenerated nightly by `.github/workflows/regenerate.yml`):

```bash
clojure -M:dev:render-html
```

## Run tests

```bash
clojure -M:test
```

Key test modules:
- `test/transitops/governor_test.clj` — the five HARD checks (route-unverified, vehicle-unverified, operator-unverified, effect-not-propose, scope-excluded) and the scope-exclusion self-trip regression test
- `test/transitops/phase_test.clj` — Phase 0→3 invariants; `:flag-safety-concern` never auto-eligible at any phase
- `test/transitops/advisor_test.clj` — advisor proposal shape and consistency
- `test/transitops/governor_contract_test.clj` — full graph integration, audit trail
- `test/transitops/store_contract_test.clj` — Store protocol and MemStore implementation

## Lint

```bash
clojure -M:lint
```

## Fork and seed your own operator data

1. Independently confirm each route's service-area/route license, each
   vehicle's roadworthiness inspection, and each driver's operator
   license with your own transit authority / DMV / licensing body
   BEFORE seeding — this actor never determines any of these itself.
2. Replace `transitops.store/demo-data` (or call
   `store/mem-store routes vehicles operators` directly) with your own
   route/vehicle/operator directory, keyed by your own real ids.
3. Import existing trip/ridership/incident-report history via
   `store/commit-record!`/`store/append-ledger!` for historical parity,
   or start clean.
4. Run read-only service-record-logging dry-runs at Phase 0-1 before
   advancing to Phase 3 (auto-commit).
5. Configure the `coordinate-maintenance-order` cost-escalation
   threshold (`transitops.governor/maintenance-cost-threshold`) for
   your own fleet's procurement sign-off policy.
6. Publish a dry-run safety-concern flag and audit export before going
   live, per `docs/operator-guide.md`'s certification checklist.

## Governor location

The **Urban Transit Dispatch Governor** sits at:

```
src/transitops/governor.cljc
```

Five HARD checks (all permanent, non-overrideable): route-unverified,
vehicle-unverified (dispatch-scheduling only), operator-unverified
(dispatch-scheduling only), effect-not-propose, scope-excluded
(dispatch-safety-clearance finalization / driver-fitness-to-drive
determination / any op outside the closed four-op allowlist). Two
SOFT escalation gates force human sign-off: `:flag-safety-concern`
(always) and a `:coordinate-maintenance-order` above the cost
threshold.

## Architecture overview

| File | Role |
|---|---|
| `src/transitops/store.cljc` | Store protocol (MemStore); string-keyed route/vehicle/operator directories; append-only audit ledger |
| `src/transitops/advisor.cljc` | TransitDispatchAdvisor (contained intelligence node; mock or real-LLM seam) |
| `src/transitops/governor.cljc` | Urban Transit Dispatch Governor — independent compliance layer, five HARD checks |
| `src/transitops/phase.cljc` | Phase table (0→3): read-only → assisted logging → assisted scheduling → supervised-auto |
| `src/transitops/operation.cljc` | OperationActor (langgraph StateGraph) |
| `src/transitops/sim.cljc` | Demo driver |
| `src/transitops/render_html.clj` | Build-time HTML renderer for the live operator console |

## Business model & operations

See `docs/business-model.md` for the revenue model and pricing, and
`docs/operator-guide.md` for first-deployment steps and minimum
production controls.

## Certification

Operators must prove:
- Independent route/vehicle/operator verification discipline (never
  trusting a proposal's own claim)
- Governor-bypass resistance
- Evidence-backed safety-concern reporting, always routed to a human
- Human review for every escalation-gated action

## Next steps

1. **Read the README** (`../README.md`) for full architecture and context.
2. **Run the demo**: `clojure -M:run`
3. **Explore the Governor**: `src/transitops/governor.cljc` and its tests
4. **Fork and seed**: replace the demo route/vehicle/operator directory with your own, following the steps above

---

Built on [langgraph](https://github.com/kotoba-lang/langgraph) StateGraph runtime. Sibling to [cloud-itonami-isic-4911](https://github.com/cloud-itonami/cloud-itonami-isic-4911) (interurban passenger rail) and [cloud-itonami-isic-4922](https://github.com/cloud-itonami/cloud-itonami-isic-4922) (intercity coach operations) in the passenger-land-transport fleet.

License: AGPL-3.0-or-later
