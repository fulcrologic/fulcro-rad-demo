(ns com.example.client
  (:require
    [com.example.ui :as ui :refer [Root]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.rad.application :as rad-app]
    [com.fulcrologic.rad.locale :as r.locale]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
    [taoensso.timbre :as log]
    [taoensso.tufte :as tufte :refer [profile]]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.fulcro.algorithms.tx-processing.synchronous-tx-processing :as stx]
    [com.fulcrologic.rad.routing.html5-history :as hist5 :refer [html5-history]]
    [com.fulcrologic.rad.routing.history :as history]
    [com.fulcrologic.rad.routing :as routing]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [clojure.set :as set]))

(defonce stats-accumulator
  (tufte/add-accumulating-handler! {:ns-pattern "*"}))

(m/defmutation fix-route
  "Mutation. Called after auth startup. Looks at the session. If the user is not logged in, it triggers authentication"
  [_]
  (action [{:keys [app]}]
    (let [logged-in (auth/verified-authorities app)]
      (if (empty? logged-in)
        (routing/route-to! app ui/LandingPage {})
        (hist5/restore-route! app ui/LandingPage {})))))

(defn setup-RAD [app]
  (rad-app/install-ui-controls! app sui/all-controls)
  (report/install-formatter! app :boolean :affirmation (fn [_ value] (if value "yes" "no"))))

(defonce app (rad-app/fulcro-rad-app {}))

(defn refresh []
  ;; hot code reload of installed controls
  (log/info "Reinstalling controls")
  (setup-RAD app)
  (comp/refresh-dynamic-queries! app)
  (app/mount! app Root "app"))

(defn has-params-for-visible-reports? [params] ; BEWARE: Add require for `[clojure.set :as set]`
  (let [visible-report-ids
        (->> (comp/get-indexes app)
             :ident->components
             keys
             (keep (fn [[type report-id]] (when (= type :com.fulcrologic.rad.report/id) report-id)))
             set)

        report-ids-with-params
        (->> params keys set)]
    (seq (set/intersection visible-report-ids report-ids-with-params))))

(defn init []
  (log/merge-config! {:output-fn prefix-output-fn
                      :appenders {:console (console-appender)}})
  (log/info "Starting App")
  ;; default time zone (should be changed at login for given user)
  (datetime/set-timezone! "America/Los_Angeles")
  ;; Avoid startup async timing issues by pre-initializing things before mount
  (app/set-root! app Root {:initialize-state? true})
  (dr/initialize! app)
  (setup-RAD app)
  (dr/change-route! app ["landing-page"])
  (history/install-route-history! app (html5-history))
  (history/add-route-listener! app ::init
                               (fn [route params]
                                 (if (and (= route (dr/current-route app)) has-params-for-visible-reports?)
                                   ;; Find a way to check the current params for differences against the new ones???
                                   ;; No route change would happen for same `route` (despite `params` being different) so let's force it:
                                   (dr/change-route! app route (assoc params ::dr/force? true))
                                   (println "LISTENER: No ignored report-relevant route/param change => doing nothing"))

                                 ))
  (auth/start! app [LoginForm] {:after-session-check `fix-route})
  (app/mount! app Root "app" {:initialize-state? false}))

(comment

  )

(defonce performance-stats (tufte/add-accumulating-handler! {}))

(defn pperf
  "Dump the currently-collected performance stats"
  []
  (let [stats (not-empty @performance-stats)]
    (println (tufte/format-grouped-pstats stats))))
