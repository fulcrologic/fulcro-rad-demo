(ns com.example.components.delete-middleware
  (:require
    [com.fulcrologic.rad.database-adapters.datomic-cloud :as datomic]))

(def middleware (datomic/wrap-datomic-delete))
