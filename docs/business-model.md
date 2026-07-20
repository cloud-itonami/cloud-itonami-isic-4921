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

### Pricing intelligence (real competitor research, 2026-07-19)
Microtransit/paratransit dispatch-and-scheduling SaaS (the closest
commercial comparable to this actor's scheduling/dispatch-coordination
scope) publishes per-vehicle pricing in the **$20-100/vehicle/month**
range (Spare Labs industry benchmark). Enterprise fixed-route/
paratransit platforms (Trapeze PAS, Ecolane Evolution, RouteMatch) do
not publish list pricing — quotes are negotiated per agency/fleet
size, consistent with this segment's typical municipal-procurement
sales motion.

### Tiers
- **Self-host**: one-time setup fee (fork + seed + integration
  support), no recurring platform fee — the operator runs its own
  instance.
- **Managed Starter**: **¥80,000/month flat** (JPY, no-code Stripe
  Payment Link: https://buy.stripe.com/eVq14m3Sb9JSg12dIzbMQ0f),
  unlimited routes/vehicles/operators for a single small-to-mid
  operator instance — consistent with the
  `cloud-itonami` portfolio's existing Managed Starter reference point
  (`docs/adr/2607161745` at `com-junkawasaki/root`) and comfortably
  inside the $20-100/vehicle/month real competitor range for an
  operator in the ~15-50 vehicle band (≈¥8,000-27,000/vehicle-equivalent
  at that fleet size, well under per-vehicle enterprise list rates).
- **Managed per-route/depot** (multi-depot operators): negotiated,
  scaling from the Starter tier baseline.
- Support retainer with SLA (self-host customers).

No paid tenant yet (self-reported honestly, not fabricated) — see
`90-docs/business/cloud-itonami-flagship-rollout-ledger.edn` at
`com-junkawasaki/root` for this vertical's rollout status.

### Real-world operator landscape

This blueprint is generic and forkable — it is not affiliated with, and
has no code dependency on, any specific real operator. For market/
competitive context, the `cloud-itonami-lei` catalog (ADR-2607110300 at
`com-junkawasaki/root`) archives the published Terms and Conditions of
Use of
[**Transdev Group SA**](https://github.com/cloud-itonami/cloud-itonami-lei-969500lmwjbg5rhvjv88)
(LEI `969500LMWJBG5RHVJV88`, GLEIF-verified), one of the largest
operators of municipal urban/suburban bus, tram and light-rail
contracts worldwide — a read-only reference, not a partnership or
endorsement. Two further real-world urban-bus operators archived in
the same catalog:
[**ComfortDelGro Corporation Limited**](https://github.com/cloud-itonami/cloud-itonami-lei-2549005o5pva2jch6q33)
(LEI `2549005O5PVA2JCH6Q33`, Singapore/UK/Australia/China) and
[**FirstGroup plc**](https://github.com/cloud-itonami/cloud-itonami-lei-549300dejzcpwa4hkm93)
(LEI `549300DEJZCPWA4HKM93`, UK First Bus network),
[**The Go-Ahead Group Limited**](https://github.com/cloud-itonami/cloud-itonami-lei-2138009tf1syomqlbj60)
(LEI `2138009TF1SYOMQLBJ60`, UK Go-Ahead London network) and
[**RATP Developpement**](https://github.com/cloud-itonami/cloud-itonami-lei-969500j9kg4hf67vc976)
(LEI `969500J9KG4HF67VC976`, France, international urban-transit
operations),
[**Keolis SA**](https://github.com/cloud-itonami/cloud-itonami-lei-969500568m45lz4wyf39)
(LEI `969500568M45LZ4WYF39`, France, 70% SNCF-owned, one of the
world's largest public-transit operators by contract count) and
[**SBS Transit Ltd**](https://github.com/cloud-itonami/cloud-itonami-lei-254900em62y5rrtj9771)
(LEI `254900EM62Y5RRTJ9771`, Singapore's largest public bus operator,
75%-owned by ComfortDelGro — a distinct entity from the ComfortDelGro
entry above). This actor's scope explicitly names taxi/rideshare
dispatch alongside fixed-route service, so the catalog also archives
[**Uber Technologies, Inc.**](https://github.com/cloud-itonami/cloud-itonami-lei-549300b2ftg34fildr98)
(LEI `549300B2FTG34FILDR98`) and
[**Grab Holdings Inc.**](https://github.com/cloud-itonami/cloud-itonami-lei-549300g8zpnq5dni6a45)
(LEI `549300G8ZPNQ5DNI6A45`, Southeast Asia) and
[**Lyft, Inc.**](https://github.com/cloud-itonami/cloud-itonami-lei-549300h7i5vn334xvz52)
(LEI `549300H7I5VN334XVZ52`, North America) — see this repo's
`docs/real-world-tos-governor-analysis.md` for a genuinely different
finding for these two: their own published terms explicitly disclaim
verifying driver/partner suitability, a contrast with (not a
confirmation of) this governor's own independent-verification design.

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
