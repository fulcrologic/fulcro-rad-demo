(ns com.example.components.auto-resolvers
  (:require
   [com.example.model :refer [all-attributes]]
   [mount.core :refer [defstate]]
   [com.fulcrologic.rad.resolvers :as res]
   [roterski.fulcro.rad.database-adapters.crux :as crux]
   [taoensso.timbre :as log]))

(defstate automatic-resolvers
  :start
  (vec
   (concat
    (res/generate-resolvers all-attributes)
    (crux/generate-resolvers all-attributes :production))))
