(ns com.example.db.datomic-db-test
  (:require
    [com.fulcrologic.rad.database-adapters.datomic :as datomic]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [datomic.api :as d]
    [taoensso.timbre :as log]
    [clojure.test :refer :all]))

(log/set-level! :debug)


