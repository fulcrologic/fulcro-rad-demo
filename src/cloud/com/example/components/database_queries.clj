(ns com.example.components.database-queries
  (:require
    [com.fulcrologic.rad.database-adapters.datomic-cloud :as datomic]
    [datomic.api :as d]
    [taoensso.timbre :as log]
    [taoensso.encore :as enc]))

(defn get-all-accounts
  [env query-params]
  (if-let [db (some-> (get-in env [::datomic/databases :production]) deref)]
    (let [ids (if (:show-inactive? query-params)
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

(defn get-customer-invoices [env {:account/keys [id]}]
  (if-let [db (some-> (get-in env [::datomic/databases :production]) deref)]
    (let [ids (d/q '[:find [?uuid ...]
                     :in $ ?cid
                     :where
                     [?dbid :invoice/id ?uuid]
                     [?dbid :invoice/customer ?c]
                     [?c :account/id ?cid]] db id)]
      (mapv (fn [id] {:invoice/id id}) ids))
    (log/error "No database atom for production schema!")))

(defn get-all-invoices
  [env query-params]
  (if-let [db (some-> (get-in env [::datomic/databases :production]) deref)]
    (let [ids (d/q [:find '[?uuid ...]
                    :where
                    ['?dbid :invoice/id '?uuid]] db)]
      (mapv (fn [id] {:invoice/id id}) ids))
    (log/error "No database atom for production schema!")))

(defn get-invoice-customer-id
  [env invoice-id]
  (if-let [db (some-> (get-in env [::datomic/databases :production]) deref)]
    (d/q '[:find ?account-uuid .
           :in $ ?invoice-uuid
           :where
           [?i :invoice/id ?invoice-uuid]
           [?i :invoice/customer ?c]
           [?c :account/id ?account-uuid]] db invoice-id)
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

(defn get-line-item-category [env line-item-id]
  (if-let [db (some-> (get-in env [::datomic/databases :production]) deref)]
    (let [id (d/q '[:find ?cid .
                    :in $ ?line-item-id
                    :where
                    [?e :line-item/id ?line-item-id]
                    [?e :line-item/item ?item]
                    [?item :item/category ?c]
                    [?c :category/id ?cid]] db line-item-id)]
      id)
    (log/error "No database atom for production schema!")))

(defn get-login-info
  "Get the account name, time zone, and password info via a username (email)."
  [{::datomic/keys [databases] :as env} username]
  (enc/if-let [db @(:production databases)]
    (d/pull db [:account/name
                {:time-zone/zone-id [:db/ident]}
                :password/hashed-value
                :password/salt
                :password/iterations]
      [:account/email username])))
