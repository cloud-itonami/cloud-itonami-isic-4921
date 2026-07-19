(ns transitops.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300,
  Wave5 rollout, generator template ledger seq 6): this repo previously
  had NO demo page and no generator at all. This namespace drives the
  REAL actor stack (`transitops.operation` -> `transitops.governor` ->
  `transitops.store`) through a scenario adapted from this repo's own
  `transitops.sim` demo driver (`clojure -M:run`, confirmed to run
  correctly against the real seeded route/vehicle/operator directory
  before this file was written), trimmed to a representative subset at
  phase 3 (supervised-auto) and rendered deterministically -- no
  invented numbers, no timestamps in the page content, byte-identical
  across reruns against the same seed (verified by diffing two
  consecutive runs).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [transitops.store :as store]
            [transitops.operation :as op]
            [langgraph.graph :as g]))

(def ^:private coordinator
  {:actor-id "coord-1" :actor-role :transit-dispatch-coordinator :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context coordinator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "coord-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store through a scenario mixing every disposition
  this actor can reach: route-1 clears a service-record log (auto-
  commit, clean), a verified vehicle+operator dispatch scheduling
  (auto-commit, clean), and a low-cost maintenance-order coordination
  (auto-commit, clean); route-1's high-cost maintenance order and its
  safety-concern flag both ALWAYS escalate to a human -- approved in
  both cases, so route-1's own last recorded status is a clean
  approval, not a hold. route-99 (does not exist) HARD-holds on
  `:route-unverified`; route-3 (registered but its inspection/
  certification is not yet verified) HARD-holds on the same rule,
  showing the distinct 'unregistered' vs 'registered-but-unverified'
  ground states; a dispatch-scheduling against route-2 naming
  vehicle-2 (registered but its roadworthiness inspection has lapsed)
  HARD-holds on `:vehicle-unverified`; a second dispatch-scheduling
  against route-2 naming operator-2 (registered but license renewal
  pending) HARD-holds on `:operator-unverified`. Every HARD hold never
  reaches a human. Returns the resulting store -- every field read by
  `render` below is real governor/store output, not a hand-typed copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    (exec! actor "t1" {:op :log-service-record :route-id "route-1"
                       :patch {:trip-count 12 :ridership-count 301}})

    (exec! actor "t2" {:op :schedule-dispatch-operation :route-id "route-1"
                       :patch {:vehicle-id "vehicle-1" :operator-id "operator-1"
                               :timetable-slot "weekday-morning" :date "2026-07-20"}})

    (exec! actor "t3" {:op :coordinate-maintenance-order :route-id "route-1"
                       :patch {:vehicle-id "vehicle-1" :item "brake-pad replacement"
                               :estimated-cost 350.0}})

    (exec! actor "t4" {:op :coordinate-maintenance-order :route-id "route-1"
                       :patch {:vehicle-id "vehicle-1" :item "drivetrain overhaul"
                               :estimated-cost 8200.0}})
    (approve! actor "t4")

    (exec! actor "t5" {:op :flag-safety-concern :route-id "route-1"
                       :patch {:concern "vehicle-1 reported unusual brake noise on downtown loop; driver requested inspection"
                               :confidence 0.92}})
    (approve! actor "t5")

    (exec! actor "t6" {:op :log-service-record :route-id "route-99"
                       :patch {:trip-count 0}})

    (exec! actor "t7" {:op :log-service-record :route-id "route-3"
                       :patch {:trip-count 5}})

    (exec! actor "t8" {:op :schedule-dispatch-operation :route-id "route-2"
                       :patch {:vehicle-id "vehicle-2" :operator-id "operator-1"
                               :timetable-slot "weekday-afternoon" :date "2026-07-21"}})

    (exec! actor "t9" {:op :schedule-dispatch-operation :route-id "route-2"
                       :patch {:vehicle-id "vehicle-1" :operator-id "operator-2"
                               :timetable-slot "weekday-evening" :date "2026-07-21"}})
    db))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- last-fact-for [ledger route-id]
  (last (filter #(= (:route-id %) route-id) ledger)))

(defn- status-cell [ledger route-id]
  (let [f (last-fact-for ledger route-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      (= :approval-granted (:t f)) "<span class=\"ok\">approved &amp; committed</span>"
      (= :governor-hold (:t f))
      (let [rule (-> f :violations first :rule)]
        (str "<span class=\"critical\">HARD hold &middot; " (esc (name (or rule :unknown))) "</span>"))
      (= :approval-requested (:t f)) "<span class=\"warn\">awaiting approval</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- bool-cell [v] (if v "<span class=\"ok\">yes</span>" "<span class=\"err\">no</span>"))

(defn- route-row [ledger {:keys [route-id name registered? verified?]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc route-id) (esc name) (bool-cell registered?) (bool-cell verified?)
          (status-cell ledger route-id)))

(defn- vehicle-row [{:keys [vehicle-id name registered? verified?]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc vehicle-id) (esc name) (bool-cell registered?) (bool-cell verified?)))

(defn- operator-row [{:keys [operator-id name registered? verified?]}]
  (format "        <tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc operator-id) (esc name) (bool-cell registered?) (bool-cell verified?)))

(defn- ledger-row [{:keys [t op route-id basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (name t)) (esc (name (or op :n-a))) (esc (or route-id ""))
          (esc (or (some->> basis (map name) (str/join ", ")) ""))))

(def ^:private action-gate-rows
  ;; Static description of this actor's own op contract (README `Ops`
  ;; table, `transitops.governor`/`transitops.phase`) -- documentation
  ;; of fixed behavior, not runtime telemetry, so it is legitimately
  ;; hand-described rather than derived from a live run.
  ["        <tr><td><code>:log-service-record</code></td><td><span class=\"ok\">auto-commit when clean, phase 3</span></td></tr>"
   "        <tr><td><code>:schedule-dispatch-operation</code></td><td><span class=\"ok\">auto-commit when clean &middot; vehicle + operator independently verified</span></td></tr>"
   "        <tr><td><code>:coordinate-maintenance-order</code></td><td><span class=\"warn\">auto-commit below cost threshold &middot; ALWAYS human approval above it</span></td></tr>"
   "        <tr><td><code>:flag-safety-concern</code></td><td><span class=\"warn\">ALWAYS human approval, any phase</span></td></tr>"])

(defn render
  "Renders the full operator-console.html document from a store `db`
  that has already run `run-demo!` (or any other real scenario)."
  [db]
  (let [ledger (vec (store/ledger db))
        routes (store/all-route-records db)
        vehicles (store/all-vehicle-records db)
        operators (store/all-operator-records db)
        route-rows (str/join "\n" (map (partial route-row ledger) routes))
        vehicle-rows (str/join "\n" (map vehicle-row vehicles))
        operator-rows (str/join "\n" (map operator-row operators))
        ledger-rows (str/join "\n" (map ledger-row ledger))]
    (str
     "<html><head><meta charset=\"utf-8\"><title>cloud-itonami-isic-4921 &middot; urban/suburban transit dispatch</title><style>\n"
     "table { width: 100%; border-collapse: collapse; font-size: 14px; }\n"
     ".ok { color: #137a3f; }\n"
     "body { font-family: system-ui,-apple-system,sans-serif; margin: 0; color: #1a1a1a; background: #fafafa; }\n"
     "header.bar { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e5e5e5; }\n"
     "th, td { text-align: left; padding: 8px 10px; border-bottom: 1px solid #f0f0f0; }\n"
     "h2 { margin-top: 0; font-size: 15px; }\n"
     ".warn { color: #b25c00; background: #fff8e1; padding: 2px 6px; border-radius: 4px; }\n"
     "main { max-width: 980px; margin: 24px auto; padding: 0 20px; }\n"
     "header.bar h1 { font-size: 18px; margin: 0; font-weight: 600; }\n"
     ".muted { color: #888; font-size: 13px; }\n"
     ".critical { color: #fff; background: #b3261e; padding: 2px 6px; border-radius: 4px; font-weight: 600; }\n"
     ".card { background: #fff; border: 1px solid #e5e5e5; border-radius: 8px; padding: 16px; margin-bottom: 16px; }\n"
     ".err { color: #b3261e; background: #fbe9e7; padding: 2px 6px; border-radius: 4px; }\n"
     "th { font-weight: 600; color: #555; font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; }\n"
     "header.bar .badge { margin-left: auto; font-size: 12px; color: #666; }\n"
     "code { font-size: 12px; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; }\n"
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Urban/suburban transit dispatch (ISIC 4921) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · dispatch-safety-clearance finalization always excluded</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"card\">\n"
     "    <h2>Routes</h2>\n"
     "    <p class=\"muted\">Demo snapshot — build-time-generated from <code>transitops.store</code> via <code>transitops.render-html</code> (<code>clojure -M:dev:render-html</code>), regenerated nightly.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Route</th><th>Name</th><th>Registered</th><th>Verified</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     route-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Vehicles</h2>\n"
     "    <table>\n"
     "      <thead><tr><th>Vehicle</th><th>Name</th><th>Registered</th><th>Verified (roadworthiness)</th></tr></thead>\n"
     "      <tbody>\n"
     vehicle-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Operators (drivers)</h2>\n"
     "    <table>\n"
     "      <thead><tr><th>Operator</th><th>Name</th><th>Registered</th><th>Verified (license)</th></tr></thead>\n"
     "      <tbody>\n"
     operator-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Action gate (Urban Transit Dispatch Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden. Route/vehicle/operator status is independently re-derived from the store, never trusted from a proposal. Directly finalizing a dispatch-safety-clearance decision or determining driver fitness-to-drive is permanently out of scope.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every proposal, hold and commit this scenario produced.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Route</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     ledger-rows "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        db (run-demo!)
        html (render db)]
    (spit out html)
    (println "wrote" out "(" (count (store/ledger db)) "ledger facts )")))
