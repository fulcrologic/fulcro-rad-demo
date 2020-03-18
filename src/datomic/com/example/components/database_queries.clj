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
                      ['?dbid :account/id '?uuid]] db)
                (d/q [:find '[?uuid ...]
                      :where
                      ['?dbid :account/active? true]
                      ['?dbid :account/id '?uuid]] db))]
      (mapv (fn [id] {:account/id id}) ids))
    (log/error "No database atom for production schema!")))

(defn get-all-items
  [env {:category/keys [id]}]
  (if-let [db (some-> (get-in env [::datomic/databases :production]) deref)]
    (let [ids (if id
                (d/q '[:find [?uuid ...]
                       :in $ ?catid
                       :where
                       [?c :category/id ?catid]
                       [?i :item/category ?c]
                       [?i :item/id ?uuid]] db id)
                (d/q '[:find [?uuid ...]
                       :where
                       [_ :item/id ?uuid]] db))]
      (mapv (fn [id] {:item/id id}) ids))
    (log/error "No database atom for production schema!")))

(defn get-all-invoices
  [env query-params]
  (if-let [db (some-> (get-in env [::datomic/databases :production]) deref)]
    (let [ids (d/q [:find '[?uuid ...]
                    :where
                    ['?dbid :invoice/id '?uuid]] db)]
      (mapv (fn [id] {:invoice/id id}) ids))
    (log/error "No database atom for production schema!")))

(defn get-all-categories
  [env query-params]
  (if-let [db (some-> (get-in env [::datomic/databases :production]) deref)]
    (let [ids (d/q '[:find [?id ...]
                     :where
                     [?e :category/label]
                     [?e :category/id ?id]] db)]
      (mapv (fn [id] {:category/id id}) ids))
    (log/error "No database atom for production schema!")))
