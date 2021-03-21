(ns com.example.components.crux
  (:require
   [roterski.fulcro.rad.database-adapters.crux :as crux]
   [mount.core :refer [defstate]]
   [com.example.components.config :refer [config]]))

(defstate ^{:on-reload :noop} crux-nodes
  :start
  (crux/start-databases (crux/symbolize-crux-modules config))
  :stop
  (for [node crux-nodes]
    (.close node)))
