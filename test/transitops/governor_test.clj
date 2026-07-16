(ns transitops.governor-test
  "Pure unit tests of `transitops.governor/check` against hand-built
  proposals -- the fast, focused complement to `governor-contract-
  test`'s full-graph integration coverage."
  (:require [clojure.test :refer [deftest is testing]]
            [transitops.advisor :as adv]
            [transitops.governor :as gov]
            [transitops.store :as store]))

(def route-1 {:route-id "route-1" :name "Route 12 - Downtown Loop" :registered? true :verified? true})
(def route-3 {:route-id "route-3" :name "Pilot BRT Corridor" :registered? true :verified? false})
(def vehicle-1 {:vehicle-id "vehicle-1" :name "Bus Unit 204" :registered? true :verified? true})
(def vehicle-2 {:vehicle-id "vehicle-2" :name "Tram Unit 12 (inspection expired)" :registered? true :verified? false})
(def operator-1 {:operator-id "operator-1" :name "Driver A. Tanaka" :registered? true :verified? true})
(def operator-2 {:operator-id "operator-2" :name "Driver B. Ishikawa (license pending)" :registered? true :verified? false})

(defn- clean-proposal [op route-id]
  {:op op :route-id route-id :summary "s" :rationale "routine transit dispatch coordination"
   :cites [route-id] :effect :propose :value {} :confidence 0.85})

(defn- clean-dispatch [route-id vehicle-id operator-id]
  (assoc (clean-proposal :schedule-dispatch-operation route-id)
         :value {:route-id route-id :vehicle-id vehicle-id :operator-id operator-id}))

(defn- clean-maintenance-order [route-id vehicle-id cost]
  (assoc (clean-proposal :coordinate-maintenance-order route-id)
         :value {:route-id route-id :vehicle-id vehicle-id :estimated-cost cost}))

(deftest route-unregistered-is-hard
  (testing "no route record at all -> HARD hold"
    (let [s (store/mem-store {"route-1" route-1})
          verdict (gov/check {} nil (clean-proposal :log-service-record "unknown-route") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:route-unverified} (map :rule (:violations verdict)))))))

(deftest route-unverified-is-hard
  (testing "route registered but not yet verified -> HARD hold"
    (let [s (store/mem-store {"route-3" route-3})
          verdict (gov/check {} nil (clean-proposal :log-service-record "route-3") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:route-unverified} (map :rule (:violations verdict)))))))

(deftest vehicle-missing-on-dispatch-is-hard
  (testing "dispatch proposal with no :vehicle-id at all -> HARD hold"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1} {"operator-1" operator-1})
          verdict (gov/check {} nil (clean-dispatch "route-1" nil "operator-1") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:vehicle-unverified} (map :rule (:violations verdict)))))))

(deftest vehicle-unregistered-on-dispatch-is-hard
  (testing "dispatch proposal naming an unknown vehicle -> HARD hold"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1} {"operator-1" operator-1})
          verdict (gov/check {} nil (clean-dispatch "route-1" "unknown-vehicle" "operator-1") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:vehicle-unverified} (map :rule (:violations verdict)))))))

(deftest vehicle-unverified-on-dispatch-is-hard
  (testing "dispatch proposal naming a registered-but-unverified vehicle -> HARD hold"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1 "vehicle-2" vehicle-2} {"operator-1" operator-1})
          verdict (gov/check {} nil (clean-dispatch "route-1" "vehicle-2" "operator-1") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:vehicle-unverified} (map :rule (:violations verdict)))))))

(deftest vehicle-verified-on-dispatch-is-not-hard-on-vehicle-check
  (testing "dispatch proposal naming a verified vehicle never trips :vehicle-unverified"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1} {"operator-1" operator-1})
          verdict (gov/check {} nil (clean-dispatch "route-1" "vehicle-1" "operator-1") s)]
      (is (empty? (filter #(= :vehicle-unverified (:rule %)) (:violations verdict)))))))

(deftest operator-missing-on-dispatch-is-hard
  (testing "dispatch proposal with no :operator-id at all -> HARD hold"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1} {"operator-1" operator-1})
          verdict (gov/check {} nil (clean-dispatch "route-1" "vehicle-1" nil) s)]
      (is (true? (:hard? verdict)))
      (is (some #{:operator-unverified} (map :rule (:violations verdict)))))))

(deftest operator-unregistered-on-dispatch-is-hard
  (testing "dispatch proposal naming an unknown operator -> HARD hold"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1} {"operator-1" operator-1})
          verdict (gov/check {} nil (clean-dispatch "route-1" "vehicle-1" "unknown-operator") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:operator-unverified} (map :rule (:violations verdict)))))))

(deftest operator-unverified-on-dispatch-is-hard
  (testing "dispatch proposal naming a registered-but-unverified operator -> HARD hold"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1} {"operator-1" operator-1 "operator-2" operator-2})
          verdict (gov/check {} nil (clean-dispatch "route-1" "vehicle-1" "operator-2") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:operator-unverified} (map :rule (:violations verdict)))))))

(deftest operator-verified-on-dispatch-is-not-hard-on-operator-check
  (testing "dispatch proposal naming a verified operator never trips :operator-unverified"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1} {"operator-1" operator-1})
          verdict (gov/check {} nil (clean-dispatch "route-1" "vehicle-1" "operator-1") s)]
      (is (empty? (filter #(= :operator-unverified (:rule %)) (:violations verdict)))))))

(deftest vehicle-and-operator-checks-are-scoped-to-dispatch-only
  (testing "non-dispatch ops never trip :vehicle-unverified/:operator-unverified, even with no vehicles/operators registered at all"
    (let [s (store/mem-store {"route-1" route-1})]
      (doseq [op [:log-service-record :coordinate-maintenance-order :flag-safety-concern]]
        (let [verdict (gov/check {} nil (clean-proposal op "route-1") s)]
          (is (empty? (filter #(#{:vehicle-unverified :operator-unverified} (:rule %)) (:violations verdict)))
              (str "op " op " must never trip :vehicle-unverified/:operator-unverified")))))))

(deftest effect-not-propose-is-hard
  (testing "any :effect other than :propose is a HARD, un-overridable block"
    (let [s (store/mem-store {"route-1" route-1})
          verdict (gov/check {} nil (assoc (clean-proposal :log-service-record "route-1") :effect :commit) s)]
      (is (true? (:hard? verdict)))
      (is (some #{:effect-not-propose} (map :rule (:violations verdict)))))))

(deftest op-outside-allowlist-is-hard
  (testing "an op outside the closed four-op allowlist is a scope violation"
    (let [s (store/mem-store {"route-1" route-1})
          verdict (gov/check {} nil (clean-proposal :finalize-dispatch-safety-clearance "route-1") s)]
      (is (true? (:hard? verdict)))
      (is (some #{:op-not-allowed} (map :rule (:violations verdict)))))))

(deftest dispatch-safety-clearance-finalization-content-is-hard-and-permanent
  (testing "a proposal whose rationale touches directly finalizing a dispatch-safety clearance is HARD-blocked regardless of op/confidence"
    (let [s (store/mem-store {"route-1" route-1})
          poisoned (assoc (clean-proposal :log-service-record "route-1")
                          :rationale "finalized the dispatch-safety clearance for the morning run"
                          :confidence 0.99)
          verdict (gov/check {} nil poisoned s)]
      (is (true? (:hard? verdict)))
      (is (some #{:scope-excluded} (map :rule (:violations verdict)))))))

(deftest driver-fitness-determination-content-is-hard
  (testing "a proposal touching determining driver fitness to drive is HARD-blocked, same as dispatch-safety-clearance finalization"
    (let [s (store/mem-store {"route-1" route-1})
          poisoned (assoc (clean-proposal :log-service-record "route-1")
                          :rationale "determined driver fitness to drive before the shift began"
                          :confidence 0.90)
          verdict (gov/check {} nil poisoned s)]
      (is (true? (:hard? verdict)))
      (is (some #{:scope-excluded} (map :rule (:violations verdict)))))))

(deftest vehicle-departure-clearance-content-is-hard
  (testing "a proposal touching clearing a vehicle for departure is HARD-blocked"
    (let [s (store/mem-store {"route-1" route-1})
          poisoned (assoc (clean-proposal :schedule-dispatch-operation "route-1")
                          :summary "dispatcher cleared the vehicle for departure despite the reported fault")
          verdict (gov/check {} nil poisoned s)]
      (is (true? (:hard? verdict)))
      (is (some #{:scope-excluded} (map :rule (:violations verdict)))))))

(deftest roadworthiness-override-content-is-hard
  (testing "a proposal touching overriding the roadworthiness inspection is HARD-blocked"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1} {"operator-1" operator-1})
          poisoned (assoc (clean-dispatch "route-1" "vehicle-1" "operator-1")
                          :summary "overrode the roadworthiness inspection to keep the schedule")
          verdict (gov/check {} nil poisoned s)]
      (is (true? (:hard? verdict)))
      (is (some #{:scope-excluded} (map :rule (:violations verdict)))))))

(deftest legitimate-safety-concern-is-not-scope-excluded
  (testing "flagging observed vehicle-defect/driver-fitness/route-hazard concerns as a SAFETY CONCERN (not a clearance/determination finalization) never trips scope-exclusion -- this actor's core valid use case must not be self-blocked"
    (let [s (store/mem-store {"route-1" route-1})
          concern (assoc (clean-proposal :flag-safety-concern "route-1")
                         :value {:concern "unusual brake noise reported by driver on downtown loop; driver reports feeling fatigued"})
          verdict (gov/check {} nil concern s)]
      (is (empty? (filter #(= :scope-excluded (:rule %)) (:violations verdict)))
          "raw observation content (vehicle defect/driver fitness/route hazard) is exactly what this op exists to surface"))))

(deftest safety-concern-always-escalates-clean
  (testing ":flag-safety-concern is always high-stakes/escalate, even when otherwise clean and high confidence"
    (let [s (store/mem-store {"route-1" route-1})
          verdict (gov/check {} nil (assoc (clean-proposal :flag-safety-concern "route-1") :confidence 0.99) s)]
      (is (false? (:hard? verdict)))
      (is (true? (:high-stakes? verdict)))
      (is (true? (:escalate? verdict))))))

(deftest high-cost-maintenance-order-always-escalates
  (testing "a :coordinate-maintenance-order above the cost threshold is high-stakes/escalate, even when otherwise clean and high confidence"
    (let [s (store/mem-store {"route-1" route-1})
          expensive (assoc (clean-maintenance-order "route-1" "vehicle-1" 5000.0) :confidence 0.97)
          verdict (gov/check {} nil expensive s)]
      (is (false? (:hard? verdict)))
      (is (true? (:high-stakes? verdict)))
      (is (true? (:escalate? verdict))))))

(deftest low-cost-maintenance-order-does-not-force-escalate
  (testing "a :coordinate-maintenance-order at or below the cost threshold does not trip the high-cost escalate gate"
    (let [s (store/mem-store {"route-1" route-1})
          cheap (assoc (clean-maintenance-order "route-1" "vehicle-1" 350.0) :confidence 0.9)
          verdict (gov/check {} nil cheap s)]
      (is (false? (:hard? verdict)))
      (is (false? (:high-stakes? verdict)))
      (is (false? (:escalate? verdict))))))

;; ----------------------------- self-trip regression -----------------------------
;;
;; A known bug class in this actor fleet: the governor's own
;; scope-exclusion term list is sometimes phrased as a bare noun (e.g.
;; "safety" or "dispatch"), which then accidentally matches inside the
;; mock advisor's own DEFAULT rationale/disclaimer text for a
;; legitimate, allowed proposal -- causing the actor to self-block its
;; own happy path. This is a dedicated regression test: every op the
;; default mock advisor can generate, with default (non-`out-of-scope?`)
;; request patches, must NEVER trip `:scope-excluded` or
;; `:op-not-allowed`.
(deftest default-mock-advisor-proposals-never-self-trip-scope-exclusion
  (testing "the default mock advisor's own proposals for every allowed op never trip the governor's scope-exclusion check"
    (let [s (store/mem-store {"route-1" route-1} {"vehicle-1" vehicle-1} {"operator-1" operator-1})]
      (doseq [op [:log-service-record :schedule-dispatch-operation :coordinate-maintenance-order
                  :flag-safety-concern]]
        (let [patch (case op
                      :schedule-dispatch-operation {:vehicle-id "vehicle-1" :operator-id "operator-1"
                                                     :timetable-slot "weekday-morning"}
                      :coordinate-maintenance-order {:vehicle-id "vehicle-1" :item "brake-pad replacement"
                                                      :estimated-cost 350.0}
                      {})
              proposal (adv/infer nil {:op op :route-id "route-1" :patch patch})
              verdict (gov/check {:route-id "route-1"} nil proposal s)]
          (is (empty? (filter #(= :scope-excluded (:rule %)) (:violations verdict)))
              (str "default advisor proposal for " op " must never self-trip :scope-excluded -- rationale/summary: "
                   (pr-str (select-keys proposal [:summary :rationale]))))
          (is (empty? (filter #(= :op-not-allowed (:rule %)) (:violations verdict)))
              (str "default advisor proposal for " op " must always be inside the closed op allowlist")))))))
