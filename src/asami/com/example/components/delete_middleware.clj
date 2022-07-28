(ns com.example.components.delete-middleware
  (:require
    [cz.holyjak.rad.database-adapters.asami :as asami]))

(def middleware (asami/wrap-delete))
