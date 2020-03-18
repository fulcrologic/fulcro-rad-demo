(ns com.example.model.seed
  (:require
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.rad.attributes :as attr]))

(defn new-account
  "Seed helper. Uses name as db/id (tempid)."
  [id name email password & {:as extras}]
  (let [salt (attr/gen-salt)]
    (merge
      {:db/id                 name
       :account/id            id
       :account/email         email
       :account/name          name
       :password/hashed-value (attr/encrypt password salt 100)
       :password/salt         salt
       :password/iterations   100
       :account/role          :account.role/user
       :account/active?       true}
      extras)))

(defn new-address
  "Seed helper. Uses street at db/id for tempid purposes."
  [id street & {:as extras}]
  (merge
    {:db/id          street
     :address/id     id
     :address/street street
     :address/city   "Sacramento"
     :address/state  :address.state/CA
     :address/zip    "99999"}
    extras))

(defn new-category
  "Seed helper. Uses label for tempid purposes."
  [id label & {:as extras}]
  (merge
    {:db/id          label
     :category/id id
     :category/label label}
    extras))

(defn new-item
  "Seed helper. Uses street at db/id for tempid purposes."
  [id name price & {:as extras}]
  (merge
    {:db/id      name
     :item/id    id
     :item/name  name
     :item/price (math/numeric price)}
    extras))

