(ns com.example.components.delete-middleware
  (:require
   [roterski.fulcro.rad.database-adapters.xtdb :as xtdb]))

(def middleware (xtdb/wrap-xtdb-delete))
