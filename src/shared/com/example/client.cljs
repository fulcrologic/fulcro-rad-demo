(ns com.example.client
  (:require
    [cljs.loader :as loader]
    [com.example.model :as r.model]
    [com.example.ui :as ui :refer [Root]]
    [com.example.ui.form-rendering]                         ; side effect to get defmethods
    [com.example.ui.report-rendering]                       ; side effect to get defmethods
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
    [com.fulcrologic.fulcro.algorithms.tx-processing.batched-processing :as btxn]
    [com.fulcrologic.fulcro.algorithms.tx-processing.synchronous-tx-processing :as sync]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.fulcro.react.version18 :refer [with-react18]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.rad.application :as rad-app]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form-render :as fr]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.routing :as routing]
    [com.fulcrologic.rad.routing.history :as history]
    [com.fulcrologic.rad.routing.html5-history :as hist5 :refer [new-html5-history]]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [taoensso.timbre :as log]
    [taoensso.tufte :as tufte :refer [profile]]))

(defonce stats-accumulator
  (tufte/add-accumulating-handler! {:ns-pattern "*"}))

(defn module-for-url
  "Figure out if the target URL requires a module of code to load."
  [app]
  (let [hist                  (history/active-history app)
        {:keys [route]} (history/-current-route hist)
        route-element->module {"invoice"          :invoicing
                               "account-invoices" :invoicing
                               "invoices"         :invoicing}]
    (first (keep route-element->module route))))

(m/defmutation fix-route
  "Mutation. If the user is logged in, try to restore the route, making sure any code modules are loaded before doing so."
  [_]
  (action [{:keys [app]}]
    (let [logged-in (auth/verified-authorities app)]
      (if (empty? logged-in)
        (routing/route-to! app ui/LandingPage {})
        (let [module  (module-for-url app)
              loaded? (if (nil? module)
                        true
                        (some-> module (loader/loaded?)))]
          (if loaded?
            (hist5/restore-route! app ui/LandingPage {})
            (loader/load module #(js/setTimeout (fn [] (hist5/restore-route! app ui/LandingPage {}))
                                   500))))))))

(defn setup-RAD [app]
  (rad-app/install-ui-controls! app sui/all-controls)
  (report/install-formatter! app :boolean :affirmation (fn [_ value] (if value "yes" "no")))
  (log/info "ALLOWING DEFAULTS")
  (fr/allow-defaults! r.model/all-attributes))

(defonce app (-> (rad-app/fulcro-rad-app {})
               (with-react18)
               (btxn/with-batched-reads)
               #_(sync/with-synchronous-transactions #{:remote})))

(defn refresh []
  ;; hot code reload of installed controls
  (log/info "Reinstalling controls")
  (setup-RAD app)
  (comp/refresh-dynamic-queries! app)
  (app/force-root-render! app))

(defn init []
  (log/merge-config! {:output-fn   prefix-output-fn
                      :output-opts {:stacktrace-fonts {}}
                      :ns-filter   {:allow #{"*"}
                                    :deny  #{"com.fulcrologic.fulcro.ui-state-machines"}}
                      :appenders   {:console (console-appender)}})
  (when goog.DEBUG
    (log/set-min-level! :trace))
  (log/info "Starting App")
  ;; default time zone (should be changed at login for given user)
  (datetime/set-timezone! "America/Los_Angeles")
  ;; Avoid startup async timing issues by pre-initializing things before mount
  (app/set-root! app Root {:initialize-state? true})
  (dr/initialize! app)
  (setup-RAD app)
  (dr/change-route! app ["landing-page"])
  (history/install-route-history! app (new-html5-history {:app           app
                                                          :default-route {:route ["landing-page"]}}))
  (auth/start! app [LoginForm] {:after-session-check `fix-route})
  (app/mount! app Root "app" {:initialize-state? false}))

(defonce performance-stats (tufte/add-accumulating-handler! {}))

(defn pperf
  "Dump the currently-collected performance stats"
  []
  (let [stats (not-empty @performance-stats)]
    (println (tufte/format-grouped-pstats stats))))
