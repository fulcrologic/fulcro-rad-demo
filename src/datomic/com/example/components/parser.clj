(ns com.example.components.parser
  (:require
    [com.example.components.auto-resolvers :refer [automatic-resolvers]]
    [com.example.components.config :refer [config]]
    [com.example.components.datomic :refer [datomic-connections]]
    [com.example.components.save-middleware :as save]
    [com.example.components.delete-middleware :as delete]
    [com.example.model.account :as account]
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.blob-storage :as storage]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.pathom :as pathom]
    [mount.core :refer [defstate]]
    [com.fulcrologic.rad.database-adapters.datomic :as datomic]))

(defstate parser
  :start
  (pathom/new-parser config
    [(form/pathom-plugin save/middleware delete/middleware)
     (datomic/pathom-plugin (fn [env] {:production (:main datomic-connections)}))
     ;; TASK: make defstate for these that check dev sys property and use leaky store during dev only
     (blob/pathom-plugin (storage/leaky-blob-store) {:avatar-images (storage/leaky-blob-store)})]
    [automatic-resolvers
     form/resolvers
     blob/resolvers
     account/login
     account/check-session]))
