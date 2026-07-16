# Operator Guide

## First Deployment
1. Register operator, routes, vehicles and drivers; independently
   confirm each route's service-area/route license, each vehicle's
   roadworthiness inspection, and each driver's operator license
   before seeding `transitops.store`.
2. Import existing trip/ridership/incident-report history.
3. Run read-only service-record-logging dry-runs (Phase 0-1).
4. Configure the rollout phase and the `coordinate-maintenance-order`
   cost-escalation threshold for human sign-off paths.
5. Publish a dry-run safety-concern flag and audit export.

## Minimum Production Controls
- route-registration/verification check before ANY proposal for that
  route
- vehicle-registration/verification (current roadworthiness
  inspection) check before ANY `:schedule-dispatch-operation` proposal
- operator-registration/verification (current operator/driver license)
  check before ANY `:schedule-dispatch-operation` proposal
- governor gate on every proposal before commit
- human sign-off for `:flag-safety-concern` (always) and high-cost
  `:coordinate-maintenance-order` proposals
- audit export for every commit, hold and approval
- backup manual dispatch process

## Certification
Certified operators must prove route/vehicle/operator-verification
discipline, governor-bypass resistance, evidence-backed safety-concern
reporting and human review for every escalation-gated action.
