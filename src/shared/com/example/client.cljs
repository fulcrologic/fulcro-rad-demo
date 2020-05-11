(ns com.example.client
  (:require
    [com.example.ui :as ui :refer [Root]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.rad.application :as rad-app]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
    [taoensso.timbre :as log]
    [taoensso.tufte :as tufte :refer [profile]]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.rad.routing.html5-history :as hist5 :refer [html5-history]]
    [com.fulcrologic.rad.routing.history :as history]
    [com.fulcrologic.rad.routing :as routing]))

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

(defonce app (rad-app/fulcro-rad-app
               {:client-did-mount (fn [app]
                                    (auth/start! app [LoginForm] {:after-session-check `fix-route}))}))

(defn refresh []
  ;; hot code reload of installed controls
  (log/info "Reinstalling controls")
  (rad-app/install-ui-controls! app sui/all-controls)
  (app/mount! app Root "app"))

(defn init []
  (log/info "Starting App")
  (log/merge-config! {:output-fn prefix-output-fn
                      :appenders {:console (console-appender)}})
  ;; a default tz until they log in
  (datetime/set-timezone! "America/Los_Angeles")
  (history/install-route-history! app (html5-history))
  (rad-app/install-ui-controls! app sui/all-controls)
  (app/mount! app Root "app"))

(defonce performance-stats (tufte/add-accumulating-handler! {}))

(defn pperf
  "Dump the currently-collected performance stats"
  []
  (let [stats (not-empty @performance-stats)]
    (println (tufte/format-grouped-pstats stats))))
