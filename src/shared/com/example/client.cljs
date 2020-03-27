(ns com.example.client
  (:require
    [com.example.ui :as ui :refer [Root]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.rad.application :as rad-app]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
    [taoensso.timbre :as log]
    [taoensso.tufte :as tufte :refer [profile]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.rad.type-support.date-time :as datetime]))

(defonce stats-accumulator
  (tufte/add-accumulating-handler! {:ns-pattern "*"}))

(defonce app (rad-app/fulcro-rad-app
               {:client-did-mount (fn [app]
                                    (log/merge-config! {:output-fn prefix-output-fn
                                                        :appenders {:console (console-appender)}})
                                    (auth/start! app [LoginForm])
                                    (dr/change-route app (dr/path-to ui/LandingPage)))}))

(defn refresh []
  (app/mount! app Root "app"))

(defn init []
  (log/info "Starting App")
  ;; a default tz until they log in
  (datetime/set-timezone! "America/Los_Angeles")
  (rad-app/install-ui-controls! app sui/all-controls)
  (app/mount! app Root "app"))

(defonce performance-stats (tufte/add-accumulating-handler! {}))

(defn pperf
  "Dump the currently-collected performance stats"
  []
  (let [stats (not-empty @performance-stats)]
    (println (tufte/format-grouped-pstats stats))))
