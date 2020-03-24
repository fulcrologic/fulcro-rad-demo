(ns com.example.components.save-middleware
(:require
  [com.fulcrologic.rad.middleware.save-middleware :as r.s.middleware]
  [com.fulcrologic.rad.database-adapters.sql.middleware :as sql-middleware]
  [com.fulcrologic.rad.blob :as blob]
  [com.example.model :as model]))

(def middleware
  (->
    (sql-middleware/wrap-sql-save)
    (blob/wrap-persist-images model/all-attributes)
    (r.s.middleware/wrap-rewrite-values)))
