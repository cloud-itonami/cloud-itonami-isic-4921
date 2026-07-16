(ns transitops.store
  "SSoT for the ISIC-4921 'Urban and suburban passenger land transport'
  (city bus, tram, light rail, taxi/rideshare dispatch) operations-
  COORDINATION actor, behind a `Store` protocol so the backend is a
  swap, not a rewrite -- the same seam every `cloud-itonami-isic-*`
  actor in this fleet uses.

  This actor coordinates the back-office SCHEDULING/DISPATCH LOGISTICS
  of an urban/suburban passenger-land-transport operator: trip/
  ridership/incident-report data logging, vehicle/route/timetable
  dispatch scheduling, fleet maintenance procurement coordination, and
  vehicle-defect/driver-fitness/route-hazard concern flagging. It NEVER
  directly operates a vehicle, NEVER finalizes a dispatch-safety-
  clearance decision, and NEVER determines driver fitness-to-drive --
  see `transitops.governor`'s `scope-exclusion-violations`, a HARD,
  permanent, un-overridable block.

  THREE independently-verified record kinds gate every proposal, never
  trusting a proposal's own self-report (the same 'ground truth, not
  self-report' discipline every sibling actor's governor uses):

    - `route`    -- a bus/tram/light-rail line or taxi/rideshare
                    dispatch zone. Must exist AND be independently
                    `:registered?`/`:verified?` (route license /
                    service-area certification with the transit
                    authority of record) before ANY proposal
                    referencing it may commit or even escalate.
                    Checked on ALL FOUR ops.
    - `vehicle`  -- a specific bus/tram/railcar/taxi. Must exist AND be
                    independently `:registered?`/`:verified?` (vehicle
                    registration + CURRENT roadworthiness inspection)
                    before it may be named in a
                    `:schedule-dispatch-operation` proposal -- putting
                    a vehicle into revenue passenger service is exactly
                    the moment vehicle-worthiness matters most. This
                    actor never sets these flags itself; only an
                    external roadworthiness-inspection system does.
    - `operator` -- a driver/vehicle operator. Must exist AND be
                    independently `:registered?`/`:verified?` (a
                    current, valid operator/driver license -- i.e.
                    fitness-to-drive already independently
                    determined) before being named in a
                    `:schedule-dispatch-operation` proposal. This actor
                    never determines driver fitness-to-drive itself;
                    only an external licensing authority does.

  `MemStore` -- atom of EDN. The deterministic default for dev/tests/
  demo (no deps). `routes`/`vehicles`/`operators` directories keyed by
  their own id STRING (never keywords -- consistent keying from the
  start, avoiding the silent-miss bug that has plagued earlier sibling
  actors).

  The ledger stays append-only: which route/vehicle/operator a
  proposal targeted, which operation, on what basis, committed/held/
  escalated and approved by whom is always a query over an immutable
  log.")

(defprotocol Store
  (route-record [s route-id] "Registered route record, or nil.
    Route map: {:route-id .. :name .. :registered? bool :verified? bool}.")
  (all-route-records [s])
  (vehicle-record [s vehicle-id] "Registered vehicle record, or nil.
    Vehicle map: {:vehicle-id .. :name .. :registered? bool :verified? bool}.")
  (all-vehicle-records [s])
  (operator-record [s operator-id] "Registered operator/driver record, or nil.
    Operator map: {:operator-id .. :name .. :registered? bool :verified? bool}.")
  (all-operator-records [s])
  (ledger [s] "the append-only immutable decision-fact log")
  (coordination-log [s] "the append-only committed coordination-proposal history")
  (commit-record! [s record] "apply a committed proposal's record to the SSoT")
  (append-ledger! [s fact] "append one immutable decision fact")
  (with-route-records [s routes] "replace/seed the route directory (map route-id->route)")
  (with-vehicle-records [s vehicles] "replace/seed the vehicle directory (map vehicle-id->vehicle)")
  (with-operator-records [s operators] "replace/seed the operator directory (map operator-id->operator)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained route/vehicle/operator directory covering
  both the happy path and the governor's own hard checks, so the actor
  + tests run offline."
  []
  {:routes
   {"route-1" {:route-id "route-1" :name "Route 12 - Downtown Loop (city bus)"
               :registered? true :verified? true}
    "route-2" {:route-id "route-2" :name "Suburban Tram Line B"
               :registered? true :verified? true}
    "route-3" {:route-id "route-3" :name "Pilot BRT Corridor (in intake)"
               :registered? true :verified? false}}
   :vehicles
   {"vehicle-1" {:vehicle-id "vehicle-1" :name "Bus Unit 204"
                 :registered? true :verified? true}
    "vehicle-2" {:vehicle-id "vehicle-2" :name "Tram Unit 12 (roadworthiness inspection expired)"
                 :registered? true :verified? false}}
   :operators
   {"operator-1" {:operator-id "operator-1" :name "Driver A. Tanaka"
                  :registered? true :verified? true}
    "operator-2" {:operator-id "operator-2" :name "Driver B. Ishikawa (license renewal pending)"
                  :registered? true :verified? false}}})

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (route-record [_ route-id] (get-in @a [:routes route-id]))
  (all-route-records [_] (sort-by :route-id (vals (:routes @a))))
  (vehicle-record [_ vehicle-id] (get-in @a [:vehicles vehicle-id]))
  (all-vehicle-records [_] (sort-by :vehicle-id (vals (:vehicles @a))))
  (operator-record [_ operator-id] (get-in @a [:operators operator-id]))
  (all-operator-records [_] (sort-by :operator-id (vals (:operators @a))))
  (ledger [_] (:ledger @a))
  (coordination-log [_] (:coordination-log @a))
  (commit-record! [_ record]
    (swap! a update :coordination-log conj record)
    record)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-route-records [s routes] (when (seq routes) (swap! a assoc :routes routes)) s)
  (with-vehicle-records [s vehicles] (when (seq vehicles) (swap! a assoc :vehicles vehicles)) s)
  (with-operator-records [s operators] (when (seq operators) (swap! a assoc :operators operators)) s))

(defn seed-db
  "A MemStore seeded with the demo route/vehicle/operator directory.
  The deterministic default."
  []
  (->MemStore (atom (assoc (demo-data) :ledger [] :coordination-log []))))

(defn mem-store
  "A MemStore seeded with explicit `routes`/`vehicles`/`operators` maps
  (id string -> record map) -- the primary test/dev entry point. Any
  may be empty (an unregistered-everywhere state)."
  ([routes] (mem-store routes {} {}))
  ([routes vehicles] (mem-store routes vehicles {}))
  ([routes vehicles operators]
   (->MemStore (atom {:routes (or routes {}) :vehicles (or vehicles {})
                       :operators (or operators {})
                       :ledger [] :coordination-log []}))))
