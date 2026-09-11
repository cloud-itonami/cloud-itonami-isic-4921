(ns transitops.sim
  "Demo driver -- `clojure -M:run`. Walks a clean service-record logging
  request through intake -> advise -> govern -> decide -> approval ->
  commit at phase 1 (assisted-logging, always approval), then re-runs
  the same op at phase 3 (supervised-auto, clean + high confidence ->
  auto-commit), then a dispatch-scheduling request naming a verified
  vehicle + operator and a low-cost maintenance-order coordination
  (both auto-commit clean at phase 3), then a high-cost maintenance
  order (ALWAYS escalates regardless of phase), then a safety-concern
  flag (ALWAYS escalates, at any phase -- approve, then commit), then
  HARD-hold scenarios: an unregistered route, a route registered but
  not yet verified, a dispatch-scheduling naming an unverified vehicle,
  a dispatch-scheduling naming an unverified operator, a proposal whose
  own `:effect` is not `:propose`, and a proposal that has drifted into
  the permanently-excluded dispatch-safety-clearance-finalization
  scope."
  (:require [langgraph.graph :as g]
            [transitops.advisor :as advisor]
            [transitops.store :as store]
            [transitops.operation :as op]))

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "transit-dispatch-coordinator-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        coordinator-phase-1 {:actor-id "coord-1" :actor-role :transit-dispatch-coordinator :phase 1}
        coordinator-phase-3 {:actor-id "coord-1" :actor-role :transit-dispatch-coordinator :phase 3}
        actor (op/build db)]

    (println "== log-service-record route-1 (phase 1, escalates -- human approves) ==")
    (let [r (exec-op actor "t1" {:op :log-service-record :route-id "route-1"
                                  :patch {:trip-count 18 :ridership-count 412 :incident-report nil}} coordinator-phase-1)]
      (println r)
      (println "-- human transit dispatch coordinator approves --")
      (println (approve! actor "t1")))

    (println "\n== log-service-record route-1 (phase 3, clean -- auto-commits) ==")
    (println (exec-op actor "t2" {:op :log-service-record :route-id "route-1"
                                  :patch {:trip-count 12 :ridership-count 301}} coordinator-phase-3))

    (println "\n== schedule-dispatch-operation route-1, verified vehicle+operator (phase 3, clean -- auto-commits) ==")
    (println (exec-op actor "t3" {:op :schedule-dispatch-operation :route-id "route-1"
                                  :patch {:vehicle-id "vehicle-1" :operator-id "operator-1"
                                          :timetable-slot "weekday-morning" :date "2026-07-20"}} coordinator-phase-3))

    (println "\n== coordinate-maintenance-order route-1, low cost (phase 3, clean -- auto-commits) ==")
    (println (exec-op actor "t4" {:op :coordinate-maintenance-order :route-id "route-1"
                                  :patch {:vehicle-id "vehicle-1" :item "brake-pad replacement" :estimated-cost 350.0}} coordinator-phase-3))

    (println "\n== coordinate-maintenance-order route-1, HIGH cost (ALWAYS escalates, even at phase 3) ==")
    (let [r (exec-op actor "t5" {:op :coordinate-maintenance-order :route-id "route-1"
                                 :patch {:vehicle-id "vehicle-1" :item "drivetrain overhaul" :estimated-cost 8200.0}} coordinator-phase-3)]
      (println r)
      (println "-- human transit dispatch coordinator reviews & approves --")
      (println (approve! actor "t5")))

    (println "\n== flag-safety-concern route-1 (ALWAYS escalates, even at phase 3) ==")
    (let [r (exec-op actor "t6" {:op :flag-safety-concern :route-id "route-1"
                                 :patch {:concern "vehicle-1 reported unusual brake noise on downtown loop; driver requested inspection" :confidence 0.92}} coordinator-phase-3)]
      (println r)
      (println "-- human transit dispatch coordinator reviews & approves --")
      (println (approve! actor "t6")))

    (println "\n== log-service-record route-99 (unregistered route -> HARD hold) ==")
    (println (exec-op actor "t7" {:op :log-service-record :route-id "route-99"
                                  :patch {:trip-count 0}} coordinator-phase-3))

    (println "\n== log-service-record route-3 (registered but unverified -> HARD hold) ==")
    (println (exec-op actor "t8" {:op :log-service-record :route-id "route-3"
                                  :patch {:trip-count 5}} coordinator-phase-3))

    (println "\n== schedule-dispatch-operation route-1, vehicle-2 unverified (-> HARD hold) ==")
    (println (exec-op actor "t9" {:op :schedule-dispatch-operation :route-id "route-1"
                                  :patch {:vehicle-id "vehicle-2" :operator-id "operator-1"
                                          :timetable-slot "weekday-afternoon" :date "2026-07-21"}} coordinator-phase-3))

    (println "\n== schedule-dispatch-operation route-1, operator-2 unverified (-> HARD hold) ==")
    (println (exec-op actor "t9b" {:op :schedule-dispatch-operation :route-id "route-1"
                                   :patch {:vehicle-id "vehicle-1" :operator-id "operator-2"
                                           :timetable-slot "weekday-evening" :date "2026-07-21"}} coordinator-phase-3))

    (println "\n== schedule-dispatch-operation route-1, advisor attempts direct actuation (:effect :commit) -> HARD hold ==")
    (let [actor-direct (op/build db {:advisor (reify advisor/Advisor
                                                (-advise [_ _ req]
                                                  (assoc (advisor/infer nil req) :effect :commit)))})]
      (println (exec-op actor-direct "t10" {:op :schedule-dispatch-operation :route-id "route-1"
                                           :patch {:vehicle-id "vehicle-1" :operator-id "operator-1"
                                                   :timetable-slot "weekday-night" :date "2026-07-22"}} coordinator-phase-3)))

    (println "\n== log-service-record route-1, advisor drifts into dispatch-safety-clearance-finalization scope -> HARD hold, permanent ==")
    (println (exec-op actor "t11" {:op :log-service-record :route-id "route-1"
                                   :out-of-scope? true
                                   :patch {}} coordinator-phase-3))

    (println "\n== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "\n== committed coordination log ==")
    (doseq [r (store/coordination-log db)] (println r))))
