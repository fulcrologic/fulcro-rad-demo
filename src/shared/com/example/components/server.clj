(ns com.example.components.server
  (:require
    [immutant.web :as web]
    [mount.core :refer [defstate]]
    [taoensso.timbre :as log]
    [com.example.components.config :refer [config]]
    [com.example.components.ring-middleware :refer [middleware]]))

(defstate http-server
  :start
  (let [cfg            (get config :org.immutant.web/config)
        running-server (web/run middleware cfg)]
    (log/info "Starting webserver with config " cfg)
    {:server running-server})
  :stop
  (let [{:keys [server]} http-server]
    (web/stop server)))
