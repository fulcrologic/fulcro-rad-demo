(ns com.example.client
  (:require
    [com.example.model :as model]
    [com.example.model.account :as account]
    [com.example.model.address :as address]
    [com.example.ui :as ui :refer [Root]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.fulcro.networking.http-remote :as net]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.controller :as controller]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [edn-query-language.core :as eql]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

;; TODO: Constructor function. Allow option to completely autogenerate forms if desired.
(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (if (undefined? js/fulcro_network_csrf_token)
                           "TOKEN-NOT-IN-HTML!"
                           (log/spy :info js/fulcro_network_csrf_token)))
    (net/wrap-fulcro-request)))


(defonce app (app/fulcro-app {:remotes              {:remote (http/fulcro-http-remote {:url                "/api"
                                                                                       :request-middleware secured-request-middleware})}
                              :global-eql-transform (fn [ast]
                                                      (let [kw-namespace (fn [k] (and (keyword? k) (namespace k)))
                                                            mutation?    (symbol? (:dispatch-key ast))]
                                                        (cond-> (df/elide-ast-nodes ast
                                                                  (fn [k]
                                                                    (let [ns (some-> k kw-namespace)]
                                                                      (or
                                                                        (= k '[:com.fulcrologic.fulcro.ui-state-machines/asm-id _])
                                                                        (= k df/marker-table)
                                                                        (= k ::fs/config)
                                                                        (and
                                                                          (string? ns)
                                                                          (= "ui" ns))))))
                                                          mutation? (update :children conj (eql/expr->ast :tempids)))))
                              :client-did-mount     (fn [app]
                                                      (auth/start! app [LoginForm])
                                                      (dr/change-route app (dr/path-to ui/LandingPage))
                                                      #_(controller/start! app
                                                          {::controller/home-page ["landing-page"]
                                                           ::controller/router    ui/MainRouter
                                                           ::controller/id        :main-controller}))}))

(defn refresh []
  (app/mount! app Root "app"))

(defn init []
  (log/info "Starting App")
  (form/install-ui-controls! app sui/all-controls)
  (attr/register-attributes! model/all-attributes)
  (app/mount! app Root "app"))

