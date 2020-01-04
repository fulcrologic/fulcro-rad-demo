(ns com.example.client
  (:require
    [com.example.ui :as ui :refer [Root]]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.rad.controller :as controller]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.rad.attributes :as attr]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls :as sui]
    [com.example.model.account :as account]
    [com.example.model.tag :as tag]
    [com.example.model.address :as address]
    [com.fulcrologic.rad.form :as form]))

;; TODO: Constructor function. Allow option to completely autogenerate forms if desired.

(defonce app (app/fulcro-app {:remotes              {:remote (http/fulcro-http-remote {})}
                              :global-eql-transform (fn [ast]
                                                      (let [kw-namespace (fn [k] (and (keyword? k) (namespace k)))]
                                                        (df/elide-ast-nodes ast (fn [k]
                                                                                  (let [ns (some-> k kw-namespace)]
                                                                                    (or
                                                                                      (= k '[:com.fulcrologic.fulcro.ui-state-machines/asm-id _])
                                                                                      (= k df/marker-table)
                                                                                      (= k ::fs/config)
                                                                                      (and
                                                                                        (string? ns)
                                                                                        (= "ui" ns))))))))
                              :client-did-mount     (fn [app]
                                                      (auth/start! app {:local (uism/with-actor-class (comp/get-ident LoginForm {}) LoginForm)})
                                                      (controller/start! app
                                                        {::attr/all-attributes  [account/attributes tag/attributes address/attributes]
                                                         ::controller/home-page ["landing-page"]
                                                         ::controller/router    ui/MainRouter
                                                         ::controller/id        :main-controller}))}))


(defn start []
  (form/install-ui-controls! app sui/all-controls)
  (app/mount! app Root "app"))
