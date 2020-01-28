(ns com.example.components.config
  (:require
    [clojure.pprint :refer [pprint]]
    [com.fulcrologic.fulcro.server.config :as fserver]
    [com.example.lib.logging :as logging]
    [mount.core :refer [defstate args]]
    [taoensso.timbre :as log]
    [clojure.string :as str]
    [com.example.model :as model]
    [com.fulcrologic.rad.attributes :as attr]))

(defstate config
  "The overrides option in args is for overriding
   configuration in tests."
  :start (let [{:keys [config overrides]
                :or   {config "config/dev.edn"}} (args)
               loaded-config (merge (fserver/load-config {:config-path config}) overrides)]
           (log/warn "Loading config" config)
           (attr/register-attributes! model/all-attributes)
           (logging/configure-logging! loaded-config)
           loaded-config))
