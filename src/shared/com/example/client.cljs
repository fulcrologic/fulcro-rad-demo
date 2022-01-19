(ns com.example.client
  (:require
   [com.example.app :refer [SPA]]
   [com.example.ui :as ui :refer [Root]]
   [com.example.ui.login-dialog :refer [LoginForm]]
   [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.mutations :as m]
   [com.fulcrologic.fulcro.networking.http-remote :as net]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.rad.application :as rad-app]
   [com.fulcrologic.rad.authorization :as auth]
   [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
   [com.fulcrologic.rad.report :as report]
   [com.fulcrologic.rad.routing :as routing]
   [com.fulcrologic.rad.routing.history :as history]
   [com.fulcrologic.rad.routing.html5-history :as hist5 :refer [html5-history]]
   [com.fulcrologic.rad.type-support.date-time :as datetime]
   [goog.functions :refer [debounce]]
   [taoensso.timbre :as log]
   [taoensso.tufte :as tufte :refer [profile]]
   [com.example.ui.util.toast :as toast]))

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

(defn wrap-error-reporting []
  (let [debounced-toast! (debounce toast/toast! 1000)]
    (fn error-reporting [{:keys [body status-code error outgoing-request] :as response}]
      (when (not= 200 status-code)
        (debounced-toast! "There was a network error. Please try again."))
      response)))

(def response-middleware (-> (wrap-error-reporting) (net/wrap-fulcro-response)))

(defonce app (rad-app/fulcro-rad-app
              (let [token (when-not (undefined? js/fulcro_network_csrf_token)
                            js/fulcro_network_csrf_token)]
                {:remotes
                 {:remote
                  (net/fulcro-http-remote {:url                 "/api"
                                            ;; Add response middleware and use `toast!` to inform the
                                            ;; user about things like session timeouts and network errors
                                           :response-middleware response-middleware
                                           :request-middleware  (rad-app/secured-request-middleware {:csrf-token token})})}})))

(defn refresh []
  ;; hot code reload of installed controls
  (log/info "Reinstalling controls")
  (setup-RAD app)
  (comp/refresh-dynamic-queries! app)
  (app/mount! app Root "app"))

(defn init []
  (reset! SPA app)
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
  (auth/start! app [LoginForm] {:after-session-check `fix-route})
  (app/mount! app Root "app" {:initialize-state? false}))

(comment)

(defonce performance-stats (tufte/add-accumulating-handler! {}))

(defn pperf
  "Dump the currently-collected performance stats"
  []
  (let [stats (not-empty @performance-stats)]
    (println (tufte/format-grouped-pstats stats))))
