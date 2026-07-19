# Real-world published terms vs. the Urban Transit Dispatch Governor

**As-of**: 2026-07-19
**Compares**: the archived `:tos/full-text` of 9 real operators (7
fixed-route urban-transit operators + 2 taxi/rideshare platforms) in
the `cloud-itonami-lei` catalog (all retrieved 2026-07-19) against
this repo's own `src/transitops/governor.cljc`.

| Company | LEI repo | Document type actually archived |
|---|---|---|
| Transdev Group SA | [cloud-itonami-lei-969500lmwjbg5rhvjv88](https://github.com/cloud-itonami/cloud-itonami-lei-969500lmwjbg5rhvjv88) | website Terms and Conditions of Use **+ the actual Conditions of Carriage** (added 2026-07-19, see Finding 6) |
| ComfortDelGro Corporation Limited | [cloud-itonami-lei-2549005o5pva2jch6q33](https://github.com/cloud-itonami/cloud-itonami-lei-2549005o5pva2jch6q33) | website Terms of Use |
| FirstGroup plc (First Bus brand) | [cloud-itonami-lei-549300dejzcpwa4hkm93](https://github.com/cloud-itonami/cloud-itonami-lei-549300dejzcpwa4hkm93) | website Terms of Use |
| The Go-Ahead Group Limited (Go-Ahead London brand) | [cloud-itonami-lei-2138009tf1syomqlbj60](https://github.com/cloud-itonami/cloud-itonami-lei-2138009tf1syomqlbj60) | website Terms **+ the actual Conditions of Carriage** (added 2026-07-19, see Finding 5) |
| RATP Developpement | [cloud-itonami-lei-969500j9kg4hf67vc976](https://github.com/cloud-itonami/cloud-itonami-lei-969500j9kg4hf67vc976) | website legal notice |
| Keolis SA | [cloud-itonami-lei-969500568m45lz4wyf39](https://github.com/cloud-itonami/cloud-itonami-lei-969500568m45lz4wyf39) | website legal notice (mentions légales) |
| SBS Transit Ltd | [cloud-itonami-lei-254900em62y5rrtj9771](https://github.com/cloud-itonami/cloud-itonami-lei-254900em62y5rrtj9771) | website Conditions for Use |
| Uber Technologies, Inc. | [cloud-itonami-lei-549300b2ftg34fildr98](https://github.com/cloud-itonami/cloud-itonami-lei-549300b2ftg34fildr98) | U.S. Terms of Service (taxi/rideshare) |
| Grab Holdings Inc. | [cloud-itonami-lei-549300g8zpnq5dni6a45](https://github.com/cloud-itonami/cloud-itonami-lei-549300g8zpnq5dni6a45) | Singapore Terms of Service: Transport, Delivery and Logistics (taxi/rideshare) |

## Methodology and an honest limitation (read this before the findings)

**Originally (2026-07-19, first pass), none of these documents was the
operator's actual Conditions of Carriage or passenger charter** —
every one was a generic *corporate website* terms-of-use/legal-notice
page, the kind of document that governs browsing the operator's
website, not riding its buses. **This has since been partly fixed**:
The Go-Ahead Group's real Conditions of Carriage was located and
added to its LEI repo the same day (see Finding 5 below), alongside
Flix SE's Conditions of Carriage in the sibling
[`cloud-itonami-isic-4922`](https://github.com/cloud-itonami/cloud-itonami-isic-4922)
repo. The remaining companies (Transdev, ComfortDelGro, FirstGroup,
RATP Dev, Keolis, SBS Transit) are still website-only, so Findings 1–2
below draw on a *mix* of website boilerplate and, for Go-Ahead
specifically, real carriage terms — read Finding 5 for the stronger
evidence and the earlier findings for the still-honest weaker-evidence
caveat on the rest. Every quoted clause below is copied verbatim from
the archived journal entries (`80-data/public/tos.journal.edn` in each
linked repo) — nothing here is paraphrased from memory or invented.

## Finding 1: even generic website liability clauses preserve an unlimited-liability carve-out for bodily injury/gross negligence — matching the governor's own permanent safety exclusion

Two of the seven documents state this explicitly, in nearly identical
substance despite different jurisdictions:

FirstGroup/First Bus (English law):

> "Nothing in these terms of use excludes or limits our liability for
> **death or personal injury** arising from our negligence, or our
> fraud or fraudulent misrepresentation, or any other liability that
> cannot be excluded or limited by English law."

Keolis SA (French law):

> "En tous les cas, la responsabilité de la Société Keolis ne saurait
> être limitée et/ou exclue en cas de **dommage corporel**, faute
> lourde ou bien encore faute dolosive." — "In all cases, Keolis's
> liability may **not** be limited and/or excluded in the event of
> **bodily injury**, gross negligence, or willful misconduct."

The other five (Transdev, ComfortDelGro, Go-Ahead, RATP Dev) exclude
liability broadly for website-content/informational matters but do not
carry an explicit bodily-injury carve-out in the specific page
archived — plausibly because those pages are shorter/more generic, not
because the underlying law differs (English and French law both make
this carve-out a mandatory floor regardless of what any specific page
says; Singapore's Consumer Protection framework has an analogous
floor).

This directly parallels `transitops.governor`'s own architecture:
`scope-exclusion-violations` is a HARD, PERMANENT block — "directly
finalizing a dispatch-safety-clearance decision" and "determining
driver fitness-to-drive" are excluded "structurally, not as a rollout
milestone" (the governor's own docstring), evaluated unconditionally
on every proposal regardless of confidence or how clean everything
else is. Two independently-arrived-at principles — one from consumer/
tort law, one from this actor's own governed-actor design — converge
on the same shape: **safety-critical determinations are the one
category of decision that cannot be limited, delegated, or automated
away**, whether by a carrier's own contract terms or by an AI
proposal.

## Finding 2: jurisdiction diversity across 7 real operators validates the generic, forkable blueprint design

| Company | Governing law (as actually stated) |
|---|---|
| Transdev Group SA | French law; Nanterre courts exclusive |
| ComfortDelGro | Laws of the Republic of Singapore; Singapore courts exclusive |
| FirstGroup/First Bus | English law; England & Wales courts (non-exclusive for consumers, exclusive for businesses); Scotland/NI residents may sue locally |
| Go-Ahead London | Laws of England and Wales; English courts exclusive |
| RATP Developpement | French law; French courts |
| Keolis SA | French law (droit français) |
| SBS Transit | *(no governing-law clause present on the specific page archived — honestly noted, not assumed)* |

Six real, large urban-transit operators already span three distinct
national legal regimes (France, England & Wales, Singapore) just for
their *website* terms — the actual passenger-carriage/safety
regulatory regime each operates under (UK PSV licensing, French
transport-authority contracts, Singapore's Public Transport Council
rules, etc.) is a further, deeper layer of jurisdiction-specific law
this survey did not attempt to catalog. This is concrete evidence for
why `cloud-itonami-isic-4921` is deliberately a **generic** blueprint:
`transitops.store`'s independent route/vehicle/operator verification
model (registered-and-verified, re-derived from the store, never
trusted from a proposal) is jurisdiction-agnostic by design specifically
*because* no single hard-coded national compliance ruleset could serve
operators this legally diverse — each fork is expected to seed its own
real route/vehicle/operator registry against its own jurisdiction's
actual licensing authority.

## Finding 4: taxi/rideshare platforms' own terms explicitly disclaim partner-suitability verification — a CONTRAST with the governor's design, not a confirmation

This actor's own scope (README) names "taxi/rideshare dispatch"
alongside fixed-route bus/tram service, so the catalog also archives
two real ride-hailing platforms. Unlike Findings 1–3, this one is a
genuine **contrast**, not a parallel — worth recording precisely
because an honest analysis reports what it actually finds, not only
the confirmations.

Grab Holdings Inc., real published Terms of Service §21.2:

> "GRAB DOES NOT WARRANT OR REPRESENT THAT IT ASSESSES OR MONITORS THE
> **SUITABILITY, LEGALITY, ABILITY**, MOVEMENT OR LOCATION OF ANY
> CONSUMERS OR PARTNERS... AND YOU EXPRESSLY WAIVE AND RELEASE GRAB
> FROM ANY AND ALL LIABILITY... ARISING FROM OR IN ANY WAY RELATED TO
> THE CONSUMERS OR PARTNERS."

Uber Technologies, Inc.'s own Terms of Service similarly frame the
company primarily as a technology-platform intermediary between riders
and independent transportation providers (§8), with disputes routed to
binding individual arbitration (§2) rather than litigated on the
platform's own duty of care.

`transitops.governor` takes the structurally opposite position:
`vehicle-unverified-violations` and `operator-unverified-violations`
are HARD, permanent blocks — a `:schedule-dispatch-operation` proposal
naming an unregistered/unverified vehicle or operator can **never**
commit or even escalate, full stop, re-derived from the store's own
records and never trusted from the proposal. Where a real, large
ride-hailing platform's own contract explicitly disclaims assessing
partner suitability, this actor's architecture makes that exact
assessment a non-negotiable gate. This is worth stating plainly as a
genuine **design differentiator** this blueprint could point to (an
operator or regulator comparing the two models would find this actor
architecturally stricter on this specific point) — not as a criticism
of Grab or Uber's real legal posture, which reflects their own
platform-liability strategy, not a defect.

## Finding 5: The Go-Ahead Group's real Conditions of Carriage closes this analysis's own stated gap

The Methodology section above states plainly that none of the original
7 urban-transit documents was an actual Conditions of Carriage. That
gap is now partly closed: The Go-Ahead Group's real, group-wide
Conditions of Carriage (published via its Go South Coast subsidiary
site, applying to Go-Ahead Group member companies' UK bus/coach
services generally, not just the website) was located and archived
into the existing `cloud-itonami-lei-2138009tf1syomqlbj60` repo
alongside its original website Terms of Use.

Two real clauses from this document directly strengthen Findings 1
and 2:

> "We reserve the right to refuse entry and travel of any person onto
> our buses and coaches if that person is considered to be
> undesirable, a **security or safety risk**... or who may otherwise
> cause a nuisance or disturbance."

— the same "human staff, on the vehicle, exercising real-time safety
judgment" pattern already found in Flix SE's Conditions of Carriage
(§9.2, `cloud-itonami-isic-4922`'s analysis) — now confirmed in a
*second*, independent, UK urban-bus operator's actual carriage terms,
not just an intercity-coach operator's.

> "However, we are unable to accept any responsibility... unless such
> loss, damage, injury, inconvenience or cost can be proven to be due
> to the negligence of either us or our staff... **Your statutory
> rights as a consumer are, though, not excluded or limited.**"

— the same non-excludable statutory floor pattern already found in
FirstGroup's and Keolis's website terms (Finding 1), now confirmed in
an actual Conditions of Carriage document rather than a website ToU
page — meaningfully stronger evidence for Finding 1's core claim,
since this is the document that actually governs the ride, not the
website.

## Finding 6: a third independent operator's real carriage terms confirm the same human-safety-judgment pattern

Transdev Travel's real, currently-published Conditions of Carriage
(a Transdev group coach-travel subsidiary) was located and added to
the existing Transdev Group SA LEI repo alongside its original
website terms. Two more real, verbatim clauses:

> "The company reserves the right for itself or its representative to
> refuse carriage or to disembark a passenger when, on reasonable
> grounds, this decision appears necessary for **reasons of safety**
> or when the mental or physical behaviour of the passenger is such
> as to create a nuisance or **present a danger to other
> passengers**."
>
> "The carrier's vehicles are **insured for unlimited civil liability**
> in respect of the passengers carried."

This is now the **third independent real operator** (after Flix SE's
Conditions of Carriage in `cloud-itonami-isic-4922`, and Go-Ahead's
in Finding 5 above) whose actual carriage contract assigns safety-
based refusal authority to human staff on reasonable grounds, and
carries liability for passenger harm that is not capped the way
luggage/property damage is. Three independent operators across three
countries (Germany, UK, France) converging on the identical structural
pattern is meaningfully stronger evidence than any single instance —
this is not a coincidence of one carrier's drafting style, it is how
real passenger-carriage law and practice actually works across
jurisdictions, and it is the same principle
`transitops.governor`'s permanent scope-exclusion encodes.

## What this analysis does NOT claim

- Not a legal opinion, and not a compliance certification for any
  fork of this actor.
- These are website ToU pages, not Conditions of Carriage — the
  actual passenger-safety liability regime for any of these 7
  companies was not analyzed here (see the honest limitation above).
  The one true Conditions-of-Carriage comparison in this catalog is
  Flix SE's, analyzed in `cloud-itonami-isic-4922`'s own copy of this
  document.
- Finding 1's convergence is evidence the governor's safety-exclusion
  design is well-founded, not evidence that the governor was modeled
  on any of these companies' specific terms.

## Related

- The 7 LEI repos linked in the table above (source documents)
- `cloud-itonami-isic-4922/docs/real-world-tos-governor-analysis.md` (the Flix SE Conditions-of-Carriage analysis, the stronger evidence)
- `src/transitops/governor.cljc` (the compared implementation)
