(ns com.example.model.seed
  (:require
    [com.fulcrologic.rad.type-support.decimal :as math]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.rad.attributes :as attr]
    [cz.holyjak.rad.database-adapters.asami.write :as asami.write]))

(defn new-account
  "Seed helper. Uses name as db/id (tempid)."
  [id name email password & {:as extras}]
  (let [salt (attr/gen-salt)]
    (merge
      {:account/id id
       :account/email email
       :account/name name
       :password/hashed-value (attr/encrypt password salt 100)
       :password/salt salt
       :password/iterations 100
       :account/role :account.role/user
       :account/active? true}
      extras)))

(defn new-address
  "Seed helper. Uses street as db/id for tempid purposes."
  [id street & {:as extras}]
  (merge
    {:address/id id
     :address/street street
     :address/city "Sacramento"
     :address/state :address.state/CA
     :address/zip "99999"}
    extras))

(defn new-category
  "Seed helper. Uses label for tempid purposes."
  [id label & {:as extras}]
  (merge
    {:category/id id
     :category/label label}
    extras))

(defn new-item
  "Seed helper. Uses street at db/id for tempid purposes."
  [id name price & {:as extras}]
  (merge
    {:item/id id
     :item/name name
     :item/price (math/numeric price)}
    extras))

(defn new-line-item [item-id quantity price & {:as extras}]
  (let [id (get extras :line-item/id (new-uuid))]
    (merge
      {:line-item/id id
       :line-item/item [:id [:item/id item-id]]
       :line-item/quantity quantity
       :line-item/quoted-price (math/numeric price)
       :line-item/subtotal (math/* quantity price)}
      extras)))

(defn new-invoice [date customer-id line-items & {:as extras}]
  (let [id (new-uuid)]
   (merge
     {:invoice/id id
      :invoice/customer [:id [:account/id customer-id]]
      :invoice/line-items line-items
      :invoice/total (reduce
                       (fn [total {:line-item/keys [subtotal]}]
                         (math/+ total subtotal))
                       (math/zero)
                       line-items)
      :invoice/date date}
     extras)))


(comment
  ;; normal table
  [{:top-query [:c :b :a]}]
  ;;Result:
  {:top-query [{:a 1 :b 3 :c 2}
               {:a 1 :b 3 :c 2}
               {:a 1 :b 3 :c 2}
               {:a 1 :b 3 :c 2}]}

  ;; stats table, expects *one* result, where each entry represents a particular detail that has many groups
  ;; top query
  [({:invoice-statistics [:invoice-statistics/date-groups :invoice-statistics/gross-sales :invoice-statistics/item-count]} {:group-by :month
                                                                                                                            :start-date "1/1/2020"
                                                                                                                            :end-date "5/1/2020"})]
  ;; result
  ;; invoice-statistics, invoice-statistic
  {:invoice-statistics
   {:invoice-statistics/date-groups ["1/1/2020" "2/1/2020" "3/1/2020" "4/1/2020"]
    :invoice-statistics/gross-sales [323M 313M 124M 884M]
    :invoice-statistics/item-count [10 11 5 42]}})

