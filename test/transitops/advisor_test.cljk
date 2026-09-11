(ns transitops.advisor-test
  "Unit tests of `transitops.advisor` proposal generation."
  (:require [clojure.test :refer [deftest is testing]]
            [transitops.advisor :as adv]
            [transitops.store :as store]))

(def db (store/seed-db))

(deftest propose-service-record-shape
  (testing "service-record proposal has correct shape and fields"
    (let [p (adv/infer db {:op :log-service-record
                           :route-id "route-1"
                           :patch {:trip-count 18 :ridership-count 412}})]
      (is (= :log-service-record (:op p)))
      (is (= "route-1" (:route-id p)))
      (is (= :propose (:effect p)))
      (is (<= 0 (:confidence p) 1))
      (is (map? (:value p)))
      (is (contains? (:value p) :route-id)))))

(deftest propose-dispatch-operation-shape
  (testing "dispatch-operation proposal has correct shape"
    (let [p (adv/infer db {:op :schedule-dispatch-operation
                           :route-id "route-2"
                           :patch {:vehicle-id "vehicle-1" :operator-id "operator-1"
                                   :timetable-slot "weekday-morning"}})]
      (is (= :schedule-dispatch-operation (:op p)))
      (is (= "route-2" (:route-id p)))
      (is (= :propose (:effect p))))))

(deftest propose-maintenance-order-shape
  (testing "maintenance-order proposal has correct shape"
    (let [p (adv/infer db {:op :coordinate-maintenance-order
                           :route-id "route-1"
                           :patch {:vehicle-id "vehicle-1" :item "brake-pad replacement" :estimated-cost 350.0}})]
      (is (= :coordinate-maintenance-order (:op p)))
      (is (= :propose (:effect p)))
      (is (string? (:summary p)))
      (is (= "vehicle-1" (get-in p [:value :vehicle-id]))))))

(deftest propose-safety-concern-shape
  (testing "safety-concern proposal always escalates"
    (let [p (adv/infer db {:op :flag-safety-concern
                           :route-id "route-1"
                           :patch {:concern "unusual brake noise reported"}})]
      (is (= :flag-safety-concern (:op p)))
      (is (= :propose (:effect p)))
      (is (string? (:summary p))))))

(deftest all-proposals-effect-is-always-propose
  (testing "every proposal type has :effect :propose, never direct actuation"
    (doseq [op [:log-service-record :schedule-dispatch-operation :coordinate-maintenance-order
                :flag-safety-concern]]
      (let [p (adv/infer db {:op op :route-id "route-1" :patch {}})]
        (is (= :propose (:effect p))
            (str "op " op " must have :effect :propose"))))))

(deftest rationale-string-is-present
  (testing "every proposal has a rationale explaining the advisor's thinking"
    (doseq [op [:log-service-record :schedule-dispatch-operation :coordinate-maintenance-order
                :flag-safety-concern]]
      (let [p (adv/infer db {:op op :route-id "route-1" :patch {}})]
        (is (string? (:rationale p))
            (str "op " op " must have a :rationale string"))))))
