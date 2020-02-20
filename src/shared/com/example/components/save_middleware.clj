(ns com.example.components.save-middleware
  (:require
    [clojure.pprint :refer [pprint]]
    [edn-query-language.core :as eql]
    [com.fulcrologic.rad.middleware.save-middleware :as r.s.middleware]
    [com.fulcrologic.rad.database-adapters.datomic :as datomic]
    [com.example.components.datomic :refer [datomic-connections]]
    [com.fulcrologic.rad.blob :as blob]
    [mount.core :refer [defstate]]
    [com.example.model :as model]))

(def middleware
  (->
    (datomic/wrap-datomic-save (fn [env] {:production (:main datomic-connections)}))
    (blob/wrap-persist-images model/all-attributes)
    (r.s.middleware/wrap-rewrite-values)))
