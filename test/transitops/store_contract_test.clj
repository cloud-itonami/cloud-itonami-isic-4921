(ns transitops.store-contract-test
  "Contract tests for `transitops.store/Store` protocol."
  (:require [clojure.test :refer [deftest is testing]]
            [transitops.store :as store]))

(deftest mem-store-route-lookup
  (testing "MemStore can store and retrieve routes by ID (string keys)"
    (let [routes {"r1" {:route-id "r1" :name "Route 1" :registered? true :verified? true}}
          s (store/mem-store routes)]
      (is (some? (store/route-record s "r1")))
      (is (nil? (store/route-record s "r99"))))))

(deftest mem-store-all-route-records
  (testing "MemStore returns all routes in sorted order"
    (let [routes {"r2" {:route-id "r2" :name "Route 2"}
                  "r1" {:route-id "r1" :name "Route 1"}
                  "r3" {:route-id "r3" :name "Route 3"}}
          s (store/mem-store routes)
          all-r (store/all-route-records s)]
      (is (= 3 (count all-r)))
      (is (= "r1" (:route-id (first all-r))))
      (is (= "r3" (:route-id (last all-r)))))))

(deftest mem-store-vehicle-lookup
  (testing "MemStore can store and retrieve vehicles by ID (string keys)"
    (let [vehicles {"v1" {:vehicle-id "v1" :name "Bus 1" :registered? true :verified? true}}
          s (store/mem-store {} vehicles)]
      (is (some? (store/vehicle-record s "v1")))
      (is (nil? (store/vehicle-record s "v99"))))))

(deftest mem-store-all-vehicle-records
  (testing "MemStore returns all vehicles in sorted order"
    (let [vehicles {"v2" {:vehicle-id "v2" :name "Bus 2"}
                    "v1" {:vehicle-id "v1" :name "Bus 1"}}
          s (store/mem-store {} vehicles)
          all-v (store/all-vehicle-records s)]
      (is (= 2 (count all-v)))
      (is (= "v1" (:vehicle-id (first all-v)))))))

(deftest mem-store-operator-lookup
  (testing "MemStore can store and retrieve operators by ID (string keys)"
    (let [operators {"o1" {:operator-id "o1" :name "Driver 1" :registered? true :verified? true}}
          s (store/mem-store {} {} operators)]
      (is (some? (store/operator-record s "o1")))
      (is (nil? (store/operator-record s "o99"))))))

(deftest mem-store-all-operator-records
  (testing "MemStore returns all operators in sorted order"
    (let [operators {"o2" {:operator-id "o2" :name "Driver 2"}
                     "o1" {:operator-id "o1" :name "Driver 1"}}
          s (store/mem-store {} {} operators)
          all-o (store/all-operator-records s)]
      (is (= 2 (count all-o)))
      (is (= "o1" (:operator-id (first all-o)))))))

(deftest mem-store-ledger-append
  (testing "MemStore append-ledger! adds facts to immutable log"
    (let [s (store/mem-store {})
          fact1 {:t :test :data "fact1"}
          fact2 {:t :test :data "fact2"}]
      (is (= 0 (count (store/ledger s))))
      (store/append-ledger! s fact1)
      (is (= 1 (count (store/ledger s))))
      (store/append-ledger! s fact2)
      (is (= 2 (count (store/ledger s)))))))

(deftest mem-store-coordination-log
  (testing "MemStore commit-record! appends to coordination-log"
    (let [s (store/mem-store {})
          record {:op :log-service-record :route-id "r1" :value {:trip-count 18}}]
      (is (= 0 (count (store/coordination-log s))))
      (store/commit-record! s record)
      (is (= 1 (count (store/coordination-log s))))
      (is (= record (first (store/coordination-log s)))))))

(deftest mem-store-with-route-records
  (testing "MemStore with-route-records replaces the route directory"
    (let [s (store/mem-store {})
          new-routes {"r1" {:route-id "r1" :name "Route 1"}}]
      (is (= 0 (count (store/all-route-records s))))
      (store/with-route-records s new-routes)
      (is (= 1 (count (store/all-route-records s)))))))

(deftest mem-store-with-vehicle-records
  (testing "MemStore with-vehicle-records replaces the vehicle directory"
    (let [s (store/mem-store {})
          new-vehicles {"v1" {:vehicle-id "v1" :name "Bus 1"}}]
      (is (= 0 (count (store/all-vehicle-records s))))
      (store/with-vehicle-records s new-vehicles)
      (is (= 1 (count (store/all-vehicle-records s)))))))

(deftest mem-store-with-operator-records
  (testing "MemStore with-operator-records replaces the operator directory"
    (let [s (store/mem-store {})
          new-operators {"o1" {:operator-id "o1" :name "Driver 1"}}]
      (is (= 0 (count (store/all-operator-records s))))
      (store/with-operator-records s new-operators)
      (is (= 1 (count (store/all-operator-records s)))))))

(deftest seed-db-has-demo-data
  (testing "seed-db creates a populated MemStore with demo routes, vehicles and operators"
    (let [s (store/seed-db)]
      (is (> (count (store/all-route-records s)) 0))
      (is (some? (store/route-record s "route-1")))
      (is (some? (store/route-record s "route-2")))
      (is (some? (store/route-record s "route-3")))
      (is (> (count (store/all-vehicle-records s)) 0))
      (is (some? (store/vehicle-record s "vehicle-1")))
      (is (some? (store/vehicle-record s "vehicle-2")))
      (is (> (count (store/all-operator-records s)) 0))
      (is (some? (store/operator-record s "operator-1")))
      (is (some? (store/operator-record s "operator-2"))))))

(deftest demo-data-string-key-consistency
  (testing "demo-data uses string keys, not keywords, for route-id/vehicle-id/operator-id"
    (let [demo (store/demo-data)
          routes (:routes demo)
          vehicles (:vehicles demo)
          operators (:operators demo)]
      (doseq [[k v] routes]
        (is (string? k) "route keys must be strings")
        (is (string? (:route-id v)) "route-id must be string")
        (is (= k (:route-id v)) "key must match route-id"))
      (doseq [[k v] vehicles]
        (is (string? k) "vehicle keys must be strings")
        (is (string? (:vehicle-id v)) "vehicle-id must be string")
        (is (= k (:vehicle-id v)) "key must match vehicle-id"))
      (doseq [[k v] operators]
        (is (string? k) "operator keys must be strings")
        (is (string? (:operator-id v)) "operator-id must be string")
        (is (= k (:operator-id v)) "key must match operator-id")))))

(deftest store-is-append-only
  (testing "appended facts are immutable and never removed"
    (let [s (store/seed-db)
          fact1 {:t :event1 :data "a"}
          fact2 {:t :event2 :data "b"}]
      (store/append-ledger! s fact1)
      (let [ledger-after-1 (store/ledger s)]
        (store/append-ledger! s fact2)
        (let [ledger-after-2 (store/ledger s)]
          (is (= (count ledger-after-1) (dec (count ledger-after-2))))
          (is (every? #(some (fn [x] (= x %)) ledger-after-2) ledger-after-1)
              "all prior facts must still be present"))))))
