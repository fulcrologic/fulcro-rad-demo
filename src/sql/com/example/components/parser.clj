(ns com.example.components.parser
  (:require
    [com.example.components.auto-resolvers :refer [automatic-resolvers]]
    [com.example.components.config :refer [config]]
    [com.example.components.connection-pools :as pools]
    [com.fulcrologic.rad.database-adapters.sql :as sql]
    [com.fulcrologic.rad.pathom :as pathom]
    [com.fulcrologic.rad.blob :as blob]
    [com.example.components.blob-store :as storage]
    [com.fulcrologic.rad.middleware.save-middleware :as save-middleware]
    [mount.core :refer [defstate]]))

(defstate parser
  :start
  (pathom/new-parser config
    (fn [env]
      ;; Add SQL database(s) to env so resolvers can find them (keyed by schema)
      (-> env
        (assoc ::blob/temporary-storage storage/temporary-blob-store)
        (assoc ::form/save-middleware (save-middleware/wrap-rewrite-values))
        (assoc-in [::sql/databases :production] (pools/get-jdbc-datasource))))
    [automatic-resolvers]))
