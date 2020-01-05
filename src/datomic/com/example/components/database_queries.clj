(ns com.example.components.database-queries
  (:require
    [com.fulcrologic.rad.database-adapters.datomic :as datomic]
    [datomic.api :as d]
    [taoensso.timbre :as log]))

(defn get-all-accounts
  [env query-params]
  (if-let [db (some-> (get-in env [::datomic/databases :production]) deref)]
    (let [ids (if (:ui/show-inactive? query-params)
                (d/q [:find '[?uuid ...]
                      :where
                      ['?dbid :com.example.model.account/id '?uuid]] db)
                (d/q [:find '[?uuid ...]
                      :where
                      ['?dbid :com.example.model.account/active? true]
                      ['?dbid :com.example.model.account/id '?uuid]] db))]
      (mapv (fn [id] {:com.example.model.account/id id}) ids))
    (log/error "No database atom for production schema!")))
