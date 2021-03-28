(ns com.example.components.delete-middleware
  (:require
   [roterski.fulcro.rad.database-adapters.crux :as crux]))

(def middleware (crux/wrap-crux-delete))
