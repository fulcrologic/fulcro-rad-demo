(ns com.example.components.save-middleware
  (:require
   [com.fulcrologic.rad.middleware.save-middleware :as r.s.middleware]
   [roterski.fulcro.rad.database-adapters.crux :as crux]
   [com.fulcrologic.rad.blob :as blob]
   [com.example.model :as model]))

(def middleware
  (->
   (crux/wrap-crux-save)
   (blob/wrap-persist-images model/all-attributes)
   (r.s.middleware/wrap-rewrite-values)))
