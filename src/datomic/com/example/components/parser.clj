(ns com.example.components.parser
  (:require
    [com.example.components.auto-resolvers :refer [automatic-resolvers]]
    [com.example.components.config :refer [config]]
    [com.example.components.datomic :refer [datomic-connections]]
    [com.fulcrologic.rad.database-adapters.datomic :as datomic]
    [com.fulcrologic.rad.pathom :as pathom]
    [mount.core :refer [defstate]]
    [datomic.api :as d]
    [com.fulcrologic.rad.form :as form]))

(defstate parser
  :start
  (pathom/new-parser config
    (fn [env]
      (assoc env
        ;; Register Datomic's save function for forms
        ::form/save-handlers [datomic/save-form]
        ;; Setup required datomic env entries. This is how you would
        ;; select the correct connection when doing things like sharding.
        ::datomic/connections {:production (:main datomic-connections)}
        ::datomic/databases {:production (atom (d/db (:main datomic-connections)))}))
    [automatic-resolvers form/save-form]))
