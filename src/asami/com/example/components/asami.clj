(ns com.example.components.asami
  (:require
    [asami.core :as d]
    [cz.holyjak.rad.database-adapters.asami :as asami]
    [mount.core :refer [defstate]]
    [com.example.model :refer [all-attributes]]
    [com.example.components.config :refer [config]]))

(defstate ^{:on-reload :noop} asami-connections
  :start
  (asami/start-connections config)
  :stop
  (d/shutdown))
