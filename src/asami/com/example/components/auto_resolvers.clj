(ns com.example.components.auto-resolvers
  (:require
    [com.example.model :refer [all-attributes]]
    [mount.core :refer [defstate]]
    [com.fulcrologic.rad.resolvers :as res]
    [cz.holyjak.rad.database-adapters.asami :as asami]
    [taoensso.timbre :as log]))

(defstate automatic-resolvers
  :start
  (vec
    (concat
      (res/generate-resolvers all-attributes)
      (asami/generate-resolvers all-attributes :production))))
