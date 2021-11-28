(ns com.example.components.parser
  (:require
    [com.example.components.auto-resolvers :refer [automatic-resolvers]]
    [com.example.components.blob-store :as bs]
    [com.example.components.config :refer [config]]
    [com.example.components.delete-middleware :as delete]
    [com.example.components.save-middleware :as save]
    [com.example.components.xtdb :refer [xtdb-nodes]]
    [com.example.model :refer [all-attributes]]
    [com.example.model.account :as account]
    [com.example.model.invoice :as invoice]
    [com.example.model.item :as item]
    [com.example.model.sales :as sales]
    [com.example.model.timezone :as timezone]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.pathom3 :as pathom3]
    [mount.core :refer [defstate]]
    [roterski.fulcro.rad.database-adapters.xtdb :as xtdb]))

(defstate parser
  :start
  (let [env-middleware (-> (attr/wrap-env all-attributes)
                           (form/wrap-env save/middleware delete/middleware)
                           (xtdb/wrap-env (fn [_] {:production (:main xtdb-nodes)}))
                           (blob/wrap-env bs/temporary-blob-store {:files         bs/file-blob-store
                                                                   :avatar-images bs/image-blob-store}))]
    (pathom3/new-processor config env-middleware []
      [automatic-resolvers
       form/resolvers
       (blob/resolvers all-attributes)
       account/resolvers
       invoice/resolvers
       item/resolvers
       sales/resolvers
       timezone/resolvers])))
