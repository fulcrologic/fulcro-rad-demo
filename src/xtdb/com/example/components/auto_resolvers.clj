(ns com.example.components.auto-resolvers
  (:require
   [com.example.model :refer [all-attributes]]
   [mount.core :refer [defstate]]
   [com.fulcrologic.rad.resolvers :as res]
   [roterski.fulcro.rad.database-adapters.xtdb :as xtdb]
   [taoensso.timbre :as log]))

(defstate automatic-resolvers
  :start
  (vec
   (concat
    (res/generate-resolvers all-attributes)
    (xtdb/generate-resolvers all-attributes :production))))
