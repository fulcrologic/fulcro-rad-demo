(ns com.example.model.seed
  (:require
   [com.fulcrologic.rad.type-support.decimal :as math]
   [com.fulcrologic.rad.report :as report]
   [com.fulcrologic.rad.ids :refer [new-uuid]]
   [com.fulcrologic.rad.attributes :as attr]))

(defn new-account
  "Seed helper. Uses name as db/id (tempid)."
  [id name email password & {:as extras}]
  (let [salt (attr/gen-salt)]
    (merge
     {:xt/id            id
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
  "Seed helper. Uses street as db/id for tempid purposes."
  [id street & {:as extras}]
  (merge
   {:xt/id     id
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
   {:xt/id     id
    :category/id    id
    :category/label label}
   extras))

(defn new-item
  "Seed helper. Uses street at db/id for tempid purposes."
  [id name price & {:as extras}]
  (merge
   {:xt/id id
    :item/id    id
    :item/name  name
    :item/price (math/numeric price)}
   extras))

(defn new-line-item [id item quantity price & {:as extras}]
  (merge
   {:xt/id             id
    :line-item/id           id
    :line-item/item         item
    :line-item/quantity     quantity
    :line-item/quoted-price (math/numeric price)
    :line-item/subtotal     (math/* quantity price)}
   extras))

(defn new-invoice [id date customer line-items & {:as extras}]
  (merge
   {:xt/id         id
    :invoice/id         id
    :invoice/customer   customer
    :invoice/line-items (mapv :xt/id line-items)
    :invoice/total      (reduce
                         (fn [total {:line-item/keys [subtotal]}]
                           (math/+ total subtotal))
                         (math/zero)
                         line-items)
    :invoice/date       date}
   extras))
