(ns transitops.advisor
  "TransitDispatchAdvisor -- the *contained intelligence node* for the
  ISIC-4921 'Urban and suburban passenger land transport' (city bus,
  tram, light rail, taxi/rideshare dispatch) SCHEDULING/DISPATCH
  LOGISTICS COORDINATION actor.

  It drafts exactly four kinds of back-office proposal from a closed
  allowlist: trip/ridership/incident-report data logging, vehicle/
  route/timetable dispatch scheduling, fleet maintenance procurement
  coordination, and vehicle-defect/driver-fitness/route-hazard concern
  flagging. CRITICAL: it is a smart-but-untrusted advisor. It returns a
  *proposal* (with a rationale + the fields it cited), never a
  committed record and NEVER a direct actuation -- every proposal's
  `:effect` is always `:propose`. Every output is censored downstream
  by `transitops.governor` before anything touches the SSoT.

  This actor coordinates SCHEDULING/DISPATCH LOGISTICS ONLY -- it never
  directly operates a vehicle, never overrides a driver's or
  dispatcher's safety judgment, and this advisor NEVER drafts a
  dispatch-safety-clearance finalization or a driver-fitness-to-drive
  determination -- those are permanently out of scope for this actor,
  not merely un-implemented. `transitops.governor`'s
  `scope-exclusion-violations` independently re-scans every proposal
  for exactly this failure mode (a compromised or confused advisor
  drifting into scope it must never touch) and HARD-holds it,
  regardless of confidence or op.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:op         kw             ; echoes the request op
     :route-id   str
     :summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the scope-exclusion gate
     :cites      [str ..]       ; facts/sources the advisor used -- SCANNED too
     :effect     :propose       ; ALWAYS :propose -- never a direct actuation
     :value      map            ; the draft payload a human/system would review
     :confidence 0..1}")

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

;; ----------------------------- proposal generators -----------------------------

(defn- propose-service-record
  "Draft a trip/ridership/incident-report data log entry. Pure logging
  of observed service data (trips completed, ridership counts,
  incident reports) -- never a dispatch-safety decision."
  [_db {:keys [route-id patch]}]
  {:op         :log-service-record
   :route-id   route-id
   :summary    (str route-id " の運行/乗車人数/インシデント記録を記録: " (pr-str (keys patch)))
   :rationale  "トリップ実績・乗車人数・インシデント報告の観察記録のみ。運行可否の判断は含まない。"
   :cites      [route-id]
   :effect     :propose
   :value      (merge {:route-id route-id} patch)
   :confidence 0.93})

(defn- propose-dispatch-operation
  "Draft a vehicle/route/timetable dispatch scheduling proposal (a
  roster/timetable-slot entry naming a specific vehicle + operator,
  never a direct dispatch-safety-clearance decision)."
  [_db {:keys [route-id patch]}]
  {:op         :schedule-dispatch-operation
   :route-id   route-id
   :summary    (str route-id " の車両配車/運行計画を提案: " (pr-str (keys patch)))
   :rationale  "車両・運行者・時刻表スロットの配車調整提案のみ。最終的な運行判断は人間の運行管理者が行う。"
   :cites      [route-id]
   :effect     :propose
   :value      (merge {:route-id route-id} patch)
   :confidence 0.88})

(defn- propose-maintenance-order
  "Draft a fleet maintenance procurement coordination request naming a
  vehicle -- never a finalized purchase order; a human always confirms
  procurement."
  [_db {:keys [route-id patch]}]
  {:op         :coordinate-maintenance-order
   :route-id   route-id
   :summary    (str route-id " 向け車両整備発注調整を提案: " (pr-str (keys patch)))
   :rationale  "車両整備・部品調達の発注調整提案のみ。確定発注は人間が行う。"
   :cites      [route-id]
   :effect     :propose
   :value      (merge {:route-id route-id} patch)
   :confidence 0.90})

(defn- propose-safety-concern
  "Surface an observed vehicle-defect/driver-fitness/route-hazard
  concern for HUMAN triage. This op ALWAYS escalates in
  `transitops.governor` -- never auto-committed at any phase --
  regardless of how confident the advisor is that the concern is real.
  Deliberately reports the OBSERVATION only, never a finalization/
  clearance/determination action, so the default rationale never trips
  the governor's `scope-excluded-terms` (see that var's docstring)."
  [_db {:keys [route-id patch]}]
  {:op         :flag-safety-concern
   :route-id   route-id
   :summary    (str route-id " の安全懸念フラグ: " (pr-str (:concern patch "unknown")))
   :rationale  "車両不具合・運転者の体調/適性・経路上の危険箇所に関する懸念の観察事実の報告。常に人間の確認・対応が必要。"
   :cites      [route-id]
   :effect     :propose
   :value      (merge {:route-id route-id} patch)
   :confidence (or (:confidence patch) 0.85)})

;; ----------------------------- default mock advisor -----------------------------

(defn infer
  "Mock advisor: routes to the correct proposal generator."
  [_db {:keys [op out-of-scope?] :as request}]
  (let [proposal (case op
                   :log-service-record (propose-service-record _db request)
                   :schedule-dispatch-operation (propose-dispatch-operation _db request)
                   :coordinate-maintenance-order (propose-maintenance-order _db request)
                   :flag-safety-concern (propose-safety-concern _db request)
                   {})]
    ;; Test hook: allow injecting scope-excluded content to exercise the
    ;; governor's scope-exclusion block end-to-end. Must be cleared before
    ;; production use.
    (if out-of-scope?
      (update proposal :rationale str " -- actually finalized the dispatch-safety clearance and cleared the vehicle for departure")
      proposal)))

(defn trace
  "Audit fact for a proposal generated by this advisor."
  [_request proposal]
  {:t       :advisor-proposal
   :op      (:op proposal)
   :route-id (:route-id proposal)
   :summary (:summary proposal)
   :confidence (:confidence proposal)})

(defn mock-advisor
  "The deterministic default advisor for offline demo/test."
  []
  (reify Advisor
    (-advise [_ _store request]
      (infer nil request))))
