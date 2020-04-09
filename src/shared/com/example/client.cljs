(ns com.example.client
  (:require
    ;; needed for date-time support, but not included in default date time ns for build size optimization
    [com.fulcrologic.rad.type-support.date-time]
    ["js-joda-timezone/dist/js-joda-timezone-10-year-range.min.js"]

    [com.example.ui :as ui :refer [Root]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.rad.application :as rad-app]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.example.ui.account-forms :as account-forms]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
    [taoensso.timbre :as log]
    [taoensso.tufte :as tufte :refer [profile]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.rad.routing.html5-history :as hist5 :refer [html5-history]]
    [com.fulcrologic.rad.routing :as rad-routing]
    [com.fulcrologic.rad.routing.history :as history]))

(defonce stats-accumulator
  (tufte/add-accumulating-handler! {:ns-pattern "*"}))

(defonce app (rad-app/fulcro-rad-app
               {:client-did-mount (fn [app]
                                    (log/merge-config! {:output-fn prefix-output-fn
                                                        :appenders {:console (console-appender)}})
                                    (auth/start! app [LoginForm])
                                    (hist5/restore-route! app ui/LandingPage {}))}))

(defn refresh []
  ;; hot code reload of installed controls
  (log/info "Reinstalling controls")
  (rad-app/install-ui-controls! app sui/all-controls)
  (app/mount! app Root "app"))

(defn init []
  (log/info "Starting App")
  ;; a default tz until they log in
  (datetime/set-timezone! "America/Los_Angeles")
  (history/install-route-history! app (html5-history))
  (history/add-route-listener! app ::rad-route-control
    (fn [route params]
      (if (not (dr/can-change-route? app))
        (do
          (log/warn "Browser routing was denied")
          (history/undo! app route params))
        (dr/change-route! app route params))))
  (rad-app/install-ui-controls! app sui/all-controls)
  (app/mount! app Root "app"))

(defonce performance-stats (tufte/add-accumulating-handler! {}))

(defn pperf
  "Dump the currently-collected performance stats"
  []
  (let [stats (not-empty @performance-stats)]
    (println (tufte/format-grouped-pstats stats))))
