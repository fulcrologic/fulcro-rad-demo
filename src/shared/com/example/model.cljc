(ns com.example.model
  (:require
    [com.example.model.account :as account]
    [com.example.model.item :as item]
    [com.example.model.invoice :as invoice]
    [com.example.model.line-item :as invoice-row]
    [com.example.model.address :as address]))

(def all-attributes (vec (concat
                           account/attributes
                           address/attributes
                           item/attributes
                           invoice/attributes
                           invoice-row/attributes)))
