# Governance

`cloud-itonami-isic-4921` is an OSS open-business blueprint for urban
and suburban passenger land transport dispatch coordination (ISIC
Rev.5 4921 -- city bus, tram, light rail, taxi/rideshare dispatch).

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a proposal for an unverified/unregistered route, or a dispatch-
  scheduling proposal naming an unverified/unregistered vehicle or
  operator, can never commit.
- the UrbanTransitDispatchGovernor remains independent of the advisor.
- hard policy violations (non-`:propose` effect, dispatch-safety-
  clearance-finalization or driver-fitness-to-drive-determination
  content, an op outside the closed allowlist) cannot be overridden by
  human approval.
- this actor never directly operates a vehicle and never overrides a
  driver's or dispatcher's safety judgment.
- every service-record log, dispatch-scheduling proposal, maintenance-
  order coordination and safety-concern flag is auditable.
- passenger, driver and vehicle data stays outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or
license should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is
a separate trust mark and should require security, audit and data-flow
review.

Certified operators can lose certification for:
- bypassing service-record, dispatch-scheduling, maintenance-order or
  safety-concern policy checks
- mishandling passenger, driver or vehicle data
- misrepresenting certification status
- failing to respond to security or safety incidents
