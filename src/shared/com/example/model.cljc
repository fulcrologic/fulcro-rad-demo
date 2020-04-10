(ns com.example.model
  (:require
    [com.example.model.timezone :as timezone]
    [com.example.model.account :as account]
    [com.example.model.item :as item]
    [com.example.model.invoice :as invoice]
    [com.example.model.line-item :as line-item]
    [com.example.model.address :as address]
    [com.example.model.category :as category]
    [com.example.model.file :as m.file]
    [com.example.model.sales :as sales]
    [com.fulcrologic.rad.attributes :as attr]))

(def all-attributes (vec (concat
                           account/attributes
                           address/attributes
                           category/attributes
                           item/attributes
                           invoice/attributes
                           line-item/attributes
                           m.file/attributes
                           sales/attributes
                           timezone/attributes)))

(def all-attribute-validator (attr/make-attribute-validator all-attributes))
