(ns com.example.model.seed
  (:require
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.rad.attributes :as attr]))

(defn new-account
  "Seed helper."
  [id email password & {:as extras}]
  (let [salt (attr/gen-salt)]
    (merge
      {:account/id            id
       :account/email         email
       :password/hashed-value (attr/encrypt password salt 100)
       :password/salt         salt
       :password/iterations   100
       :account/role          :account.role/user
       :account/active?       true}
      extras)))

(def development-seed-txn
  [(new-account (new-uuid 1)
     "tony@fulcrologic.com"
     "letmein"
     :account/name "Tony")])
