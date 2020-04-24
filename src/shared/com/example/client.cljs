(ns com.example.client
  (:require
    [com.example.ui :as ui :refer [Root]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.mutations :as m]
    [com.fulcrologic.rad.application :as rad-app]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [com.fulcrologic.fulcro.algorithms.timbre-support :refer [console-appender prefix-output-fn]]
    [taoensso.timbre :as log]
    [taoensso.tufte :as tufte :refer [profile]]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.rad.routing.html5-history :as hist5 :refer [html5-history]]
    [com.fulcrologic.rad.routing.history :as history]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.rad.routing :as routing]
    [com.fulcrologic.rad.type-support.cache-a-bools :as cb]))

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
                                    (log/merge-config! {:output-fn prefix-output-fn
                                                        :appenders {:console (console-appender)}})
                                    (auth/start! app [LoginForm] {:after-session-check `fix-route}))}))

(defn refresh []
  ;; hot code reload of installed controls
  (log/info "Reinstalling controls")
  (rad-app/install-ui-controls! app sui/all-controls)
  (app/mount! app Root "app"))

(defn can?
  "A sample implementation of a permission scheme for this app. A real app would have a considerably more complex
   system that might use a rules engine, multi-methods, component options, etc."
  [app action-map]
  (let [{::auth/keys [action subject context]} action-map
        {::routing/keys [target params]} context]
    (case action
      ;; A sample rule: Routing to an account is allowed if we can determine that it is active.
      :execute (if (and
                     (= subject `routing/route-to!)         ; the subject of execute is typically expressed as a symbol (mutation or function)
                     (= target :com.example.ui.account-forms/AccountForm) ; The target is usually represented as a component registry key
                     (contains? params ::report/row-props)) ; additional parameters may come from different subsystems when they do checks.
                 (let [{::report/keys [row-props]} params
                       active? (:account/active? row-props)]
                   ;; don't allow caching, since enabling an account will change this answer
                   (if active? cb/uncachably-true cb/uncachably-false))
                 ;; we don't have enough info, so we must assume it is OK and let the target deny access on load from server,
                 ;; if necessary. In this app, we're just trying to disable the link in the report, so someone pasting in
                 ;; a bookmarked URL is ok.
                 cb/uncachably-true)
      cb/cachably-true)))

(defn init []
  (log/info "Starting App")
  ;; a default tz until they log in
  (datetime/set-timezone! "America/Los_Angeles")
  (history/install-route-history! app (html5-history))
  (rad-app/install-ui-controls! app sui/all-controls)
  (auth/install-authorization! app can?)
  (app/mount! app Root "app"))

(defonce performance-stats (tufte/add-accumulating-handler! {}))

(defn pperf
  "Dump the currently-collected performance stats"
  []
  (let [stats (not-empty @performance-stats)]
    (println (tufte/format-grouped-pstats stats))))
