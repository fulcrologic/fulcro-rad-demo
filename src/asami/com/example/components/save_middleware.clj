(ns com.example.components.save-middleware
  (:require
    [com.fulcrologic.rad.middleware.save-middleware :as r.s.middleware]
    [cz.holyjak.rad.database-adapters.asami :as asami]
    [com.fulcrologic.rad.blob :as blob]
    [com.example.model :as model]))

(def middleware
  (->
    (asami/wrap-save)
    (blob/wrap-persist-images model/all-attributes)
    (r.s.middleware/wrap-rewrite-values)))
