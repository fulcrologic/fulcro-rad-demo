(ns com.example.components.connection-pools
  (:require
    [mount.core :refer [defstate]]
    [com.example.components.model :refer [all-attributes]]
    [com.example.components.config :refer [config]]
    [com.fulcrologic.rad.database-adapters.sql.plugin :as rad.sql.plugin])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defstate connection-pools
  :start
  (rad.sql.plugin/create-connection-pools! config all-attributes)
  :stop
  (rad.sql.plugin/stop-connection-pools! connection-pools))

(defn get-jdbc-datasource
  "Returns a clojure jdbc compatible data source config."
  []
  (let [ds ^HikariDataSource (some-> connection-pools :main)]
    {:datasource ds}))
