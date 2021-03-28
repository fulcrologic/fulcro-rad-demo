(ns com.example.components.database-queries
  (:require
   [roterski.fulcro.rad.database-adapters.crux-options :as co]
   [crux.api :as crux]
   [taoensso.timbre :as log]
   [taoensso.encore :as enc]))

(defn get-all-accounts
  [env query-params]
  (if-let [db (some-> (get-in env [co/databases :production]) deref)]
    (->> (if (:show-inactive? query-params)
           '{:find [?uuid]
             :where [[?uuid :account/id]]}
           '{:find [?uuid]
             :where [[?uuid :account/id]
                     [?uuid :account/active? true]]})
         (crux/q db)
         (mapv (fn [[id]] {:account/id id})))
    (log/error "No database atom for production schema!")))

(defn get-all-items
  [env {:category/keys [id]}]
  (if-let [db (some-> (get-in env [co/databases :production]) deref)]
    (->> (if id
           (crux/q db '{:find [?uuid]
                        :in [?catid]
                        :where [[?c :category/id ?catid]
                                [?i :item/category ?c]
                                [?i :item/id ?uuid]]}
                   id)
           (crux/q db '{:find [?uuid]
                        :where [[_ :item/id ?uuid]]}))
         (mapv (fn [[id]] {:item/id id})))
    (log/error "No database atom for production schema!")))

(defn get-customer-invoices [env {:account/keys [id]}]
  (if-let [db (some-> (get-in env [co/databases :production]) deref)]
    (->> (crux/q db '{:find [?uuid]
                      :in [?cid]
                      :where [[?dbid :invoice/id ?uuid]
                              [?dbid :invoice/customer ?c]
                              [?c :account/id ?cid]]}
                 id)
         (mapv (fn [[id]] {:invoice/id id})))
    (log/error "No database atom for production schema!")))

(defn get-all-invoices
  [env query-params]
  (if-let [db (some-> (get-in env [co/databases :production]) deref)]
    (->> '{:find [?uuid]
           :where [[?dbid :invoice/id ?uuid]]}
         (crux/q db)
         (mapv (fn [[id]] {:invoice/id id})))
    (log/error "No database atom for production schema!")))

(defn get-invoice-customer-id
  [env invoice-id]
  (if-let [db (some-> (get-in env [co/databases :production]) deref)]
    (-> db
        (crux/q '{:find [?account-uuid]
                  :in [?invoice-uuid]
                  :where [[?i :invoice/id ?invoice-uuid]
                          [?i :invoice/customer ?c]
                          [?c :account/id ?account-uuid]]}
                invoice-id)
        ffirst)
    (log/error "No database atom for production schema!")))

(defn get-all-categories
  [env query-params]
  (if-let [db (some-> (get-in env [co/databases :production]) deref)]
    (->> '{:find [?id]
           :where [[?e :category/label]
                   [?e :category/id ?id]]}
         (crux/q db)
         (mapv (fn [[id]] {:category/id id})))
    (log/error "No database atom for production schema!")))

(defn get-line-item-category [env line-item-id]
  (if-let [db (some-> (get-in env [co/databases :production]) deref)]
    (-> db
        (crux/q '{:find [?cid]
                  :in [?line-item-id]
                  :where [[?e :line-item/id ?line-item-id]
                          [?e :line-item/item ?item]
                          [?item :item/category ?c]
                          [?c :category/id ?cid]]}
                line-item-id)
        ffirst)
    (log/error "No database atom for production schema!")))

(defn get-login-info
  "Get the account name, time zone, and password info via a username (email)."
  [env username]
  (enc/if-let [db (some-> (get-in env [co/databases :production]) deref)]
    (-> db
        (crux/q '{:find [(eql/project ?account [:account/name
                                                {:time-zone/zone-id [:db/ident]}
                                                :password/hashed-value
                                                :password/salt
                                                :password/iterations])]
                  :in [?email]
                  :where [[?account :account/email ?email]]}
                username)
        ffirst)))
