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
    [com.example.rules :as rules]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.rad.type-support.date-time :as datetime]))

(defonce app (rad-app/fulcro-rad-app
               {:client-did-mount (fn [app]
                                    (log/merge-config! {:output-fn prefix-output-fn
                                                        :appenders {:console (console-appender)}})
                                    (rules/install-rules-engine! app)
                                    (auth/start! app [LoginForm])
                                    (dr/change-route app (dr/path-to ui/LandingPage)))}))

(defn refresh []
  (app/mount! app Root "app"))

(comment
  (dr/change-route app (dr/path-to ui/AccountForm {:action "new"
                                                   :id     (str (random-uuid))})))

(defn init []
  (log/info "Starting App")
  ;; a default tz until they log in
  (datetime/set-timezone! "America/Los_Angeles")
  (form/install-ui-controls! app sui/all-controls)
  (app/mount! app Root "app"))

