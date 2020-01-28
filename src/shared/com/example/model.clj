(ns com.example.model
  (:require
    [com.example.model.account :as account]
    [com.example.model.address :as address]))

(def all-attributes (vec (concat
                           account/attributes
                           address/attributes)))
