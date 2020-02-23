(ns com.example.components.parser
  (:require
    [com.example.components.auto-resolvers :refer [automatic-resolvers]]
    [com.example.components.config :refer [config]]
    [com.example.components.datomic :refer [datomic-connections]]
    [com.example.components.save-middleware :as save]
    [com.example.components.delete-middleware :as delete]
    [com.example.model.account :as account]
    [com.example.model :refer [all-attributes]]
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.form :as form]
    [com.example.components.blob-store :as bs]
    [com.fulcrologic.rad.pathom :as pathom]
    [mount.core :refer [defstate]]
    [com.fulcrologic.rad.database-adapters.datomic :as datomic]))

(defstate parser
  :start
  (pathom/new-parser config
    [(form/pathom-plugin save/middleware delete/middleware)
     (datomic/pathom-plugin (fn [env] {:production (:main datomic-connections)}))
     (blob/pathom-plugin bs/temporary-blob-store {:files         bs/file-blob-store
                                                  :avatar-images bs/image-blob-store})]
    [automatic-resolvers
     form/resolvers
     (blob/resolvers all-attributes)
     account/login
     account/check-session]))
