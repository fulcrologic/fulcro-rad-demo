(ns com.example.components.connection-pools
  (:require
    [mount.core :refer [defstate]]
    [com.example.model :refer [all-attributes]]
    [com.example.components.config :refer [config]]
    [com.fulcrologic.rad.database-adapters.sql :as sql])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defstate connection-pools
  :start
  (sql/create-connection-pools! config all-attributes)
  :stop
  (sql/stop-connection-pools! connection-pools))

(defn get-jdbc-datasource
  "Returns a clojure jdbc compatible data source config."
  []
  (let [ds ^HikariDataSource (some-> connection-pools :main)]
    {:datasource ds}))
