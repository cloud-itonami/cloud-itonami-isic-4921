# Real-world published terms vs. the Urban Transit Dispatch Governor

**As-of**: 2026-07-19
**Compares**: the archived `:tos/full-text` of 7 real urban-transit
operators in the `cloud-itonami-lei` catalog (all retrieved 2026-07-19)
against this repo's own `src/transitops/governor.cljc`.

| Company | LEI repo | Document type actually archived |
|---|---|---|
| Transdev Group SA | [cloud-itonami-lei-969500lmwjbg5rhvjv88](https://github.com/cloud-itonami/cloud-itonami-lei-969500lmwjbg5rhvjv88) | website Terms and Conditions of Use |
| ComfortDelGro Corporation Limited | [cloud-itonami-lei-2549005o5pva2jch6q33](https://github.com/cloud-itonami/cloud-itonami-lei-2549005o5pva2jch6q33) | website Terms of Use |
| FirstGroup plc (First Bus brand) | [cloud-itonami-lei-549300dejzcpwa4hkm93](https://github.com/cloud-itonami/cloud-itonami-lei-549300dejzcpwa4hkm93) | website Terms of Use |
| The Go-Ahead Group Limited (Go-Ahead London brand) | [cloud-itonami-lei-2138009tf1syomqlbj60](https://github.com/cloud-itonami/cloud-itonami-lei-2138009tf1syomqlbj60) | website Terms |
| RATP Developpement | [cloud-itonami-lei-969500j9kg4hf67vc976](https://github.com/cloud-itonami/cloud-itonami-lei-969500j9kg4hf67vc976) | website legal notice |
| Keolis SA | [cloud-itonami-lei-969500568m45lz4wyf39](https://github.com/cloud-itonami/cloud-itonami-lei-969500568m45lz4wyf39) | website legal notice (mentions légales) |
| SBS Transit Ltd | [cloud-itonami-lei-254900em62y5rrtj9771](https://github.com/cloud-itonami/cloud-itonami-lei-254900em62y5rrtj9771) | website Conditions for Use |

## Methodology and an honest limitation (read this before the findings)

**None of these 7 documents is the operator's actual Conditions of
Carriage or passenger charter** — every one is a generic *corporate
website* terms-of-use/legal-notice page, the kind of document that
governs browsing the operator's website, not riding its buses. The
one archived document in the whole `cloud-itonami-lei` catalog that
*is* an actual Conditions of Carriage is Flix SE's (analyzed in the
sibling repo
[`cloud-itonami-isic-4922`](https://github.com/cloud-itonami/cloud-itonami-isic-4922)'s
own `docs/real-world-tos-governor-analysis.md`). The findings below
are therefore weaker evidence than that sibling analysis — they show
a real, consistent, cross-jurisdiction pattern in these operators'
*general liability boilerplate*, not their actual passenger-carriage
liability regime. Every quoted clause below is copied verbatim from
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
