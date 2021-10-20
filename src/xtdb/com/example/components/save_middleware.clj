(ns com.example.components.save-middleware
  (:require
   [com.fulcrologic.rad.middleware.save-middleware :as r.s.middleware]
   [roterski.fulcro.rad.database-adapters.xtdb :as xtdb]
   [com.fulcrologic.rad.blob :as blob]
   [com.example.model :as model]))

(def middleware
  (->
   (xtdb/wrap-xtdb-save)
   (blob/wrap-persist-images model/all-attributes)
   (r.s.middleware/wrap-rewrite-values)))
