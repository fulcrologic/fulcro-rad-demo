(ns com.example.components.xtdb
  (:require
   [roterski.fulcro.rad.database-adapters.xtdb :as xtdb]
   [mount.core :refer [defstate]]
   [com.example.components.config :refer [config]]))

(defstate ^{:on-reload :noop} xtdb-nodes
  :start
  (xtdb/start-databases (xtdb/symbolize-xtdb-modules config))
  :stop
  (doseq [[_ node] xtdb-nodes]
    (.close node)))
