(ns com.example.components.server
  (:require
    [org.httpkit.server :refer [run-server]]
    [mount.core :as mount :refer [defstate]]
    [taoensso.timbre :as log]
    [com.example.components.config :refer [config]]
    [com.example.components.ring-middleware :refer [middleware]]))

(defstate http-server
  :start
  (let [cfg     (get config :org.httpkit.server/config)
        stop-fn (run-server middleware cfg)]
    (log/info "Starting webserver with config " cfg)
    {:stop stop-fn})
  :stop
  (let [{:keys [stop]} http-server]
    (when stop
      (stop))))

;; This is a separate file for the uberjar only. We control the server in dev mode from src/dev/user.clj
(defn -main [& args]
  (mount/start-with-args {:config "config/prod.edn"}))
