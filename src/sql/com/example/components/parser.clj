(ns com.example.components.parser
  (:require
    [com.example.components.auto-resolvers :refer [automatic-resolvers]]
    [com.example.components.config :refer [config]]
    [com.example.components.connection-pools :as pools]
    [com.fulcrologic.rad.database-adapters.sql :as sql]
    [com.fulcrologic.rad.pathom :as pathom]
    [mount.core :refer [defstate]]))

(defstate parser
  :start
  (pathom/new-parser config
    (fn [env]
      ;; Add SQL database(s) to env so resolvers can find them (keyed by schema)
      (assoc-in env
        [::sql/databases :production]
        (pools/get-jdbc-datasource)))
    [automatic-resolvers]))
