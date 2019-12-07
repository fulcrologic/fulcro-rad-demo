(ns com.example.components.database-queries
  (:require
    [datomic.api :as d]))

(defn get-all-accounts
  [db query-params]
  #?(:clj
     (let [ids (if (:ui/show-inactive? query-params)
                 (d/q [:find '[?uuid ...]
                       :where
                       ['?dbid ::id '?uuid]] db)
                 (d/q [:find '[?uuid ...]
                       :where
                       ['?dbid ::active? true]
                       ['?dbid ::id '?uuid]] db))]
       (mapv (fn [id] {::id id}) ids))))
