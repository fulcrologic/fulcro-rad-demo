(ns com.example.client
  (:require
    [com.example.model :as model]
    [com.example.ui :as ui :refer [Root]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.rendering.keyframe-render2 :as kr2]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.application :as rad-app]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [edn-query-language.core :as eql]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.fulcro.networking.file-upload :as file-upload]
    [clojure.walk :as walk]))

(defonce app (rad-app/fulcro-rad-app
               {:client-did-mount (fn [app]
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

