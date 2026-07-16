(ns transitops.governor
  "UrbanTransitDispatchGovernor -- the independent compliance layer
  that earns the TransitDispatchAdvisor the right to commit. The
  advisor has no notion of whether a route is actually registered and
  license-verified, whether a named dispatch vehicle is itself
  independently roadworthiness-verified, whether a named operator/
  driver actually holds a currently verified license, whether its own
  proposed `:effect` secretly claims a direct actuation instead of a
  mere proposal, or whether it has silently drifted into a permanently
  out-of-scope decision area, so this MUST be a separate system able
  to *reject* a proposal and fall back to HOLD.

  This actor's scope is deliberately narrow -- SCHEDULING/DISPATCH
  LOGISTICS COORDINATION ONLY (trip/ridership/incident-report data
  logging, vehicle/route/timetable dispatch scheduling, fleet
  maintenance procurement coordination, vehicle-defect/driver-fitness/
  route-hazard concern flagging). It NEVER performs or authorizes:
    - directly operating a vehicle
    - overriding a driver's or dispatcher's safety judgment
    - directly finalizing a dispatch-safety-clearance decision
    - determining driver fitness-to-drive

  FIVE HARD checks, ALL permanent, un-overridable by any human approval:

    1. Route unverified            -- the target route (bus/tram/
                                       light-rail line or taxi/
                                       rideshare dispatch zone) must
                                       exist AND be independently
                                       `:registered?`/`:verified?` in
                                       the store before ANY proposal
                                       referencing it may commit or
                                       even escalate. Never trusts a
                                       proposal's own claim about the
                                       route -- re-derived from the
                                       route's own record, the same
                                       'ground truth, not self-report'
                                       discipline every sibling actor's
                                       governor uses. Checked
                                       UNCONDITIONALLY on all four ops.
    2. Vehicle unverified          -- for `:schedule-dispatch-
                                       operation` ONLY, the proposal's
                                       own drafted `:value` must name a
                                       `:vehicle-id` that resolves to
                                       an independently
                                       `:registered?`/`:verified?`
                                       vehicle record (current
                                       roadworthiness inspection). A
                                       missing vehicle-id, or one that
                                       resolves to an unregistered or
                                       unverified vehicle, is a HARD
                                       block -- putting a vehicle into
                                       revenue passenger service is
                                       exactly the moment vehicle-
                                       worthiness matters most.
    3. Operator unverified         -- for `:schedule-dispatch-
                                       operation` ONLY, the proposal's
                                       own drafted `:value` must name
                                       an `:operator-id` that resolves
                                       to an independently
                                       `:registered?`/`:verified?`
                                       operator/driver record (a
                                       current, valid operator/driver
                                       license -- fitness-to-drive
                                       already independently
                                       determined by the licensing
                                       authority, NEVER by this actor).
                                       A missing operator-id, or one
                                       that resolves to an unregistered
                                       or unverified operator, is a
                                       HARD block.
    4. Effect not :propose         -- every proposal's `:effect` MUST
                                       be `:propose`. Any other effect
                                       value is, by construction, a
                                       claim to directly actuate/commit
                                       outside governance -- HARD
                                       block, not merely low-
                                       confidence.
    5. Scope exclusion             -- ANY proposal (regardless of op)
                                       whose op, summary, rationale,
                                       cites or draft value touches
                                       directly finalizing a dispatch-
                                       safety-clearance decision or a
                                       driver-fitness-to-drive
                                       determination is a HARD,
                                       PERMANENT block -- this actor's
                                       charter excludes that territory
                                       structurally, not as a rollout
                                       milestone. Evaluated
                                       UNCONDITIONALLY on every
                                       proposal. An op outside the
                                       closed four-op allowlist is the
                                       SAME failure mode (an advisor
                                       proposing something it was never
                                       authorized to propose) and is
                                       folded into this same check.
                                       `:flag-safety-concern` itself is
                                       never excluded by this check --
                                       surfacing a vehicle-defect/
                                       driver-fitness/route-hazard
                                       concern for a human is exactly
                                       this actor's job; only
                                       FINALIZING/determining/clearing
                                       that concern is excluded (see
                                       `scope-excluded-terms` below --
                                       phrased as the finalization/
                                       execution ACTION, never a bare
                                       noun like 'safety', 'dispatch'
                                       or 'fitness', so the default
                                       mock advisor's own
                                       `:flag-safety-concern` rationale
                                       never self-trips this check).

  Two ESCALATE (SOFT) gates, either forces human sign-off:
    - LLM confidence below the floor.
    - The op is `:flag-safety-concern` -- ALWAYS escalates to a human,
      regardless of confidence, regardless of how clean the proposal
      otherwise is. `transitops.phase` independently agrees:
      `:flag-safety-concern` is never a member of any phase's `:auto`
      set either -- two layers, not one.
    - A `:coordinate-maintenance-order` whose drafted `:value` names an
      `:estimated-cost` above `maintenance-cost-threshold` -- a
      large-value fleet-maintenance procurement proposal always needs a
      human sign-off, even when the governor and phase would otherwise
      allow auto-commit."
  (:require [clojure.string :as str]
            [transitops.store :as store]))

(def confidence-floor 0.6)

(def maintenance-cost-threshold
  "Example single-order fleet-maintenance procurement threshold
  (USD-equivalent units, domain-illustrative -- not a universal
  cross-domain constant). A `:coordinate-maintenance-order` proposal
  citing an `:estimated-cost` above this value ALWAYS escalates to
  human sign-off, regardless of confidence or rollout phase."
  2000.0)

(def allowed-ops
  "The closed proposal-op allowlist -- an op outside this set is a
  scope violation by construction (see `scope-exclusion-violations`)."
  #{:log-service-record :schedule-dispatch-operation
    :coordinate-maintenance-order :flag-safety-concern})

(def always-escalate-ops
  "Ops that ALWAYS require human sign-off, clean or not."
  #{:flag-safety-concern})

(def scope-excluded-terms
  "Case-insensitive substrings that mark a proposal as touching a
  permanently out-of-scope decision area -- directly finalizing a
  dispatch-safety-clearance decision or determining driver
  fitness-to-drive, or otherwise directly operating a vehicle /
  overriding a driver's or dispatcher's safety judgment rather than
  merely coordinating dispatch logistics around it. Scanned across the
  proposal's op/summary/rationale/cites/value, never trusting the
  advisor's own framing of its intent.

  CRITICAL: every term here is phrased as the finalization/execution
  ACTION (e.g. 'finalized the dispatch-safety clearance', 'determined
  driver fitness to drive'), never a bare noun like 'safety',
  'dispatch', 'clearance' or 'fitness' -- a bare noun would
  accidentally match inside this actor's own legitimate
  `:flag-safety-concern` default proposal text (whose whole job is to
  talk about vehicle-defect/driver-fitness/route-hazard concerns) and
  self-block the happy path. See
  `transitops.governor-test/default-mock-advisor-proposals-never-self-trip-scope-exclusion`
  for the regression test."
  ["finalize the dispatch-safety clearance" "finalized the dispatch-safety clearance"
   "finalizing the dispatch-safety clearance" "finalize the dispatch safety clearance"
   "issue the dispatch-safety clearance" "issued the dispatch-safety clearance"
   "issuing the dispatch-safety clearance"
   "clear the vehicle for departure" "cleared the vehicle for departure"
   "clearing the vehicle for departure"
   "clear for dispatch despite" "cleared for dispatch despite" "clearing for dispatch despite"
   "authorize departure despite" "authorized departure despite" "authorizing departure despite"
   "determine driver fitness to drive" "determined driver fitness to drive"
   "determining driver fitness to drive"
   "determine the driver fit to drive" "determined the driver fit to drive"
   "certify the driver as fit to drive" "certified the driver as fit to drive"
   "certifying the driver as fit to drive"
   "override the roadworthiness inspection" "overrode the roadworthiness inspection"
   "overriding the roadworthiness inspection"
   "bypass the roadworthiness inspection" "bypassed the roadworthiness inspection"
   "bypassing the roadworthiness inspection"
   "override the safety inspection" "overrode the safety inspection"
   "bypass the safety inspection" "bypassed the safety inspection"
   "override the driver's safety judgment" "overrode the driver's safety judgment"
   "override the dispatcher's safety judgment" "overrode the dispatcher's safety judgment"
   "directly operate the vehicle" "directly operated the vehicle"
   "運行可否を最終決定した" "運行可否を最終決定する" "運行可否を最終決定して"
   "運転者の乗務適性を最終判定した" "運転者の乗務適性を最終判定する"
   "出庫の最終承認を行った" "出庫の最終承認を行う"
   "点検未了のまま出庫を許可した" "点検未了のまま出庫を許可する"
   "安全確認を経ずに出庫を許可した" "安全確認を経ずに出庫を許可する"
   "運転可否の適性判定を確定した" "運転可否の適性判定を確定する"])

;; ----------------------------- checks -----------------------------

(defn- route-unverified-violations
  "The target route must exist AND be independently
  `:registered?`/`:verified?` in the store -- never trust the
  proposal's own `:route-id` claim without a route lookup."
  [{:keys [route-id]} st]
  (let [r (store/route-record st route-id)]
    (when-not (and r (:registered? r) (:verified? r))
      [{:rule :route-unverified
        :detail (str route-id " は未登録または未検証の路線/系統 -- いかなる提案も進められない")}])))

(defn- vehicle-unverified-violations
  "For `:schedule-dispatch-operation` ONLY, the proposal's own drafted
  `:value` must name a `:vehicle-id` that resolves to an independently
  `:registered?`/`:verified?` vehicle record -- never trust the
  proposal's own vehicle claim without a store lookup, the SAME
  'ground truth, not self-report' discipline as
  `route-unverified-violations`, reapplied to the physical vehicle."
  [proposal st]
  (when (= :schedule-dispatch-operation (:op proposal))
    (let [vehicle-id (get-in proposal [:value :vehicle-id])
          v (and vehicle-id (store/vehicle-record st vehicle-id))]
      (when-not (and v (:registered? v) (:verified? v))
        [{:rule :vehicle-unverified
          :detail (str (or vehicle-id "(vehicle-id missing)")
                        " は未登録または未検証(点検切れ)の車両 -- 配車提案を進められない")}]))))

(defn- operator-unverified-violations
  "For `:schedule-dispatch-operation` ONLY, the proposal's own drafted
  `:value` must name an `:operator-id` that resolves to an
  independently `:registered?`/`:verified?` operator/driver record --
  never trust the proposal's own operator claim without a store
  lookup, the SAME 'ground truth, not self-report' discipline as
  `vehicle-unverified-violations`, reapplied to the driver."
  [proposal st]
  (when (= :schedule-dispatch-operation (:op proposal))
    (let [operator-id (get-in proposal [:value :operator-id])
          o (and operator-id (store/operator-record st operator-id))]
      (when-not (and o (:registered? o) (:verified? o))
        [{:rule :operator-unverified
          :detail (str (or operator-id "(operator-id missing)")
                        " は未登録または未検証(免許更新未了)の運転者 -- 配車提案を進められない")}]))))

(defn- effect-not-propose-violations
  "`:effect` must ALWAYS be `:propose` -- any other value is a claim to
  directly actuate/commit outside governance."
  [proposal]
  (when (not= :propose (:effect proposal))
    [{:rule :effect-not-propose
      :detail (str ":effect は :propose のみ許可されるが " (pr-str (:effect proposal)) " が提案された")}]))

(defn- text-blob
  "Flatten every advisor-authored field on a proposal into one
  lower-cased blob the scope-exclusion scan checks."
  [proposal]
  (str/lower-case (pr-str (select-keys proposal [:op :summary :rationale :cites :value]))))

(defn- scope-exclusion-violations
  "HARD, PERMANENT block: a proposal outside the closed op allowlist,
  or one whose content touches directly finalizing a dispatch-safety-
  clearance decision or determining driver fitness-to-drive, regardless
  of confidence or how clean every other check is. Evaluated
  UNCONDITIONALLY on every proposal."
  [proposal]
  (let [op (:op proposal)
        blob (text-blob proposal)]
    (cond
      (not (contains? allowed-ops op))
      [{:rule :op-not-allowed
        :detail (str (pr-str op) " は許可された操作(closed allowlist)に含まれない")}]

      (some #(str/includes? blob %) scope-excluded-terms)
      [{:rule :scope-excluded
        :detail "運行可否の最終判定・運転者の適性最終判定・車両の直接操作など安全確定行為(dispatch-safety-clearance finalization / driver-fitness-to-drive determination)に触れる提案は永久に禁止"}])))

(defn- high-cost-maintenance-order?
  "A `:coordinate-maintenance-order` proposal citing an
  `:estimated-cost` above `maintenance-cost-threshold` -- always needs
  human sign-off (SOFT escalate, not a hard block: the order itself is
  in scope, only its size requires a human)."
  [proposal]
  (and (= :coordinate-maintenance-order (:op proposal))
       (some-> proposal :value :estimated-cost (> maintenance-cost-threshold))))

(defn check
  "Censors a TransitDispatchAdvisor proposal against the governor
  rules. Returns {:ok? bool :violations [..] :confidence c :escalate?
  bool :high-stakes? bool :hard? bool}."
  [request _context proposal store]
  (let [route-id (or (:route-id proposal) (:route-id request))
        hard (into []
                   (concat (route-unverified-violations {:route-id route-id} store)
                           (vehicle-unverified-violations proposal store)
                           (operator-unverified-violations proposal store)
                           (effect-not-propose-violations proposal)
                           (scope-exclusion-violations proposal)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (or (always-escalate-ops (:op proposal))
                              (high-cost-maintenance-order? proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :route-id   (:route-id request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
