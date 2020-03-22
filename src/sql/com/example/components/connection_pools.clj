(ns com.example.components.connection-pools
  (:require
    [mount.core :refer [defstate]]
    [com.example.model :refer [all-attributes]]
    [com.example.components.config :refer [config]]
    [com.fulcrologic.rad.database-adapters.sql.connection :as pools])
  (:import (com.zaxxer.hikari HikariDataSource)))

(defstate connection-pools
  :start
  (pools/create-connection-pools! config all-attributes)
  :stop
  (pools/stop-connection-pools! connection-pools))

