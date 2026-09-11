(ns transitops.governor-contract-test
  "Integration tests: full OperationActor graph exercising the
  governor's hard checks, escalation logic, and audit trail."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [transitops.advisor :as advisor]
            [transitops.store :as store]
            [transitops.operation :as op]))

(defn exec-request [actor tid request ctx]
  (g/run* actor {:request request :context ctx} {:thread-id tid}))

(defn resume-approval [actor tid status]
  (g/run* actor {:approval {:status status :by "coordinator"}} {:thread-id tid :resume? true}))

(deftest service-record-logging-full-flow
  (testing "clean service-record proposal -> auto-commit at phase 3"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-1" :phase 3}
          result (exec-request actor "t1"
                               {:op :log-service-record :route-id "route-1" :patch {:trip-count 18}}
                               ctx)]
      (is (some? result))
      (is (> (count (store/ledger db)) 0)
          "commit must append audit facts to ledger")
      (is (> (count (store/coordination-log db)) 0)
          "commit must append record to coordination-log"))))

(deftest safety-concern-always-escalates
  (testing ":flag-safety-concern escalates for human approval, regardless of phase/confidence"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-2" :phase 3}
          result (exec-request actor "t2"
                               {:op :flag-safety-concern :route-id "route-1"
                                :patch {:concern "unusual brake noise" :confidence 0.99}}
                               ctx)]
      (is (some? result))
      ;; At this point the actor is paused for approval, not yet committed
      (is (= 0 (count (store/coordination-log db)))
          "safety concern must not auto-commit, must wait for approval")
      ;; Now approve it
      (resume-approval actor "t2" :approved)
      (is (> (count (store/coordination-log db)) 0)
          "after approval, record must be committed"))))

(deftest high-cost-maintenance-order-always-escalates
  (testing "a high-cost :coordinate-maintenance-order escalates for human approval, even at phase 3 clean"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-2b" :phase 3}
          result (exec-request actor "t2b"
                               {:op :coordinate-maintenance-order :route-id "route-1"
                                :patch {:vehicle-id "vehicle-1" :item "drivetrain overhaul" :estimated-cost 8200.0}}
                               ctx)]
      (is (some? result))
      (is (= 0 (count (store/coordination-log db)))
          "high-cost maintenance order must not auto-commit, must wait for approval")
      (resume-approval actor "t2b" :approved)
      (is (> (count (store/coordination-log db)) 0)
          "after approval, record must be committed"))))

(deftest low-cost-maintenance-order-auto-commits
  (testing "a low-cost :coordinate-maintenance-order auto-commits at phase 3 when clean"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-2c" :phase 3}
          result (exec-request actor "t2c"
                               {:op :coordinate-maintenance-order :route-id "route-1"
                                :patch {:vehicle-id "vehicle-1" :item "brake-pad replacement" :estimated-cost 350.0}}
                               ctx)]
      (is (some? result))
      (is (> (count (store/coordination-log db)) 0)
          "low-cost maintenance order must auto-commit when clean at phase 3"))))

(deftest dispatch-operation-with-verified-vehicle-and-operator-auto-commits
  (testing "a :schedule-dispatch-operation naming a verified vehicle+operator auto-commits at phase 3 when clean"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-2d" :phase 3}
          result (exec-request actor "t2d"
                               {:op :schedule-dispatch-operation :route-id "route-1"
                                :patch {:vehicle-id "vehicle-1" :operator-id "operator-1"
                                        :timetable-slot "weekday-morning" :date "2026-07-20"}}
                               ctx)]
      (is (some? result))
      (is (> (count (store/coordination-log db)) 0)
          "dispatch scheduling must auto-commit when clean at phase 3"))))

(deftest unregistered-route-hard-hold
  (testing "unregistered route -> permanent HARD hold, never escalates"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-3" :phase 3}]
      (exec-request actor "t3"
                     {:op :log-service-record :route-id "unknown-route"
                      :patch {:trip-count 0}}
                     ctx)
      (is (= 0 (count (store/coordination-log db)))
          "HARD hold must never commit"))))

(deftest unverified-route-hard-hold
  (testing "registered but unverified route -> permanent HARD hold"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-4" :phase 3}
          result (exec-request actor "t4"
                               {:op :log-service-record :route-id "route-3"
                                :patch {:trip-count 5}}
                               ctx)]
      (is (some? result))
      (is (= 0 (count (store/coordination-log db)))
          "unverified route must HARD hold"))))

(deftest unverified-vehicle-dispatch-hard-hold
  (testing "a dispatch operation naming an unverified vehicle -> permanent HARD hold"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-4b" :phase 3}
          result (exec-request actor "t4b"
                               {:op :schedule-dispatch-operation :route-id "route-1"
                                :patch {:vehicle-id "vehicle-2" :operator-id "operator-1"
                                        :timetable-slot "weekday-afternoon" :date "2026-07-21"}}
                               ctx)]
      (is (some? result))
      (is (= 0 (count (store/coordination-log db)))
          "unverified vehicle must HARD hold"))))

(deftest unverified-operator-dispatch-hard-hold
  (testing "a dispatch operation naming an unverified operator -> permanent HARD hold"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-4c" :phase 3}
          result (exec-request actor "t4c"
                               {:op :schedule-dispatch-operation :route-id "route-1"
                                :patch {:vehicle-id "vehicle-1" :operator-id "operator-2"
                                        :timetable-slot "weekday-evening" :date "2026-07-21"}}
                               ctx)]
      (is (some? result))
      (is (= 0 (count (store/coordination-log db)))
          "unverified operator must HARD hold"))))

(deftest effect-not-propose-hard-hold
  (testing "proposal with :effect :commit (not :propose) -> hard hold"
    (let [db (store/seed-db)
          bad-advisor (reify advisor/Advisor
                        (-advise [_ _ req]
                          (assoc (advisor/infer nil req) :effect :commit)))
          actor (op/build db {:advisor bad-advisor})
          ctx {:actor-id "test-5" :phase 3}
          result (exec-request actor "t5"
                               {:op :log-service-record :route-id "route-1"
                                :patch {:trip-count 18}}
                               ctx)]
      (is (some? result))
      (is (= 0 (count (store/coordination-log db)))
          "non-:propose effect must HARD hold"))))

(deftest scope-excluded-content-hard-hold
  (testing "proposal drifting into dispatch-safety-clearance-finalization scope -> permanent hard hold"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-6" :phase 3}
          result (exec-request actor "t6"
                               {:op :log-service-record :route-id "route-1"
                                :out-of-scope? true  ; triggers scope pollution in advisor
                                :patch {}}
                               ctx)]
      (is (some? result))
      (is (= 0 (count (store/coordination-log db)))
          "scope-excluded content must HARD hold"))))

(deftest phase-1-approval-gate
  (testing "phase 1 approved request -> commits after human approval"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-7" :phase 1}]
      (exec-request actor "t7"
                     {:op :log-service-record :route-id "route-1"
                      :patch {:trip-count 18}}
                     ctx)
      (is (= 0 (count (store/coordination-log db)))
          "phase 1 must not auto-commit, requires approval")
      (resume-approval actor "t7" :approved)
      (is (> (count (store/coordination-log db)) 0)
          "after approval, must commit")
      (is (some #(= :committed (:t %)) (store/ledger db))
          "committed fact must be logged after approval"))))

(deftest audit-trail-completeness
  (testing "every decision leaves immutable audit facts"
    (let [db (store/seed-db)
          actor (op/build db)
          ctx {:actor-id "test-8" :phase 3}]
      (exec-request actor "t8a"
                     {:op :log-service-record :route-id "route-1" :patch {:trip-count 18}}
                     ctx)
      (exec-request actor "t8b"
                     {:op :log-service-record :route-id "unknown" :patch {:trip-count 18}}
                     ctx)
      (let [ledger (store/ledger db)]
        (is (> (count ledger) 0))
        (is (some #(= :committed (:t %)) ledger)
            "successful commits must be logged")
        (is (some #(= :governor-hold (:t %)) ledger)
            "HARD holds must be logged")))))
