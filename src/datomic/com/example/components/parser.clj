(ns com.example.components.parser
  (:require
    [com.example.components.auto-resolvers :refer [automatic-resolvers]]
    [com.example.components.blob-store :as storage]
    [com.example.components.config :refer [config]]
    [com.example.components.datomic :refer [datomic-connections]]
    [com.example.components.save-middleware :refer [middleware]]
    [com.example.model.account :as account]
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.database-adapters.datomic :as datomic]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.pathom :as pathom]
    [mount.core :refer [defstate]]))

(defstate parser
  :start
  (pathom/new-parser config
    (fn [env]
      (-> env
        (assoc ::blob/temporary-storage storage/temporary-blob-store)
        (assoc ::blob/image-store storage/image-blob-store)
        (assoc ::form/save-middleware middleware)
        (datomic/add-datomic-env {:production (:main datomic-connections)})))
    [automatic-resolvers
     form/save-form
     form/delete-entity
     blob/resolvers
     account/login
     account/check-session]))
