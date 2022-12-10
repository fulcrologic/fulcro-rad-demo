(ns com.example.model
  (:require
    [com.example.model.account :as account]
    [com.example.model.address :as address]
    [com.example.model.category :as category]
    [com.example.model.company :as company]
    [com.example.model.entity :as entity]
    [com.example.model.file :as m.file]
    [com.example.model.invoice :as invoice]
    [com.example.model.item :as item]
    [com.example.model.line-item :as line-item]
    [com.example.model.note :as note]
    [com.example.model.person :as person]
    [com.example.model.sales :as sales]
    [com.example.model.timezone :as timezone]
    [com.fulcrologic.rad.attributes :as attr]))

(def all-attributes (vec (concat
                           account/attributes
                           address/attributes
                           category/attributes
                           company/attributes
                           entity/attributes
                           item/attributes
                           invoice/attributes
                           line-item/attributes
                           note/attributes
                           m.file/attributes
                           person/attributes
                           sales/attributes
                           timezone/attributes)))

(def key->attribute (attr/attribute-map all-attributes))
(def idk->attributes (attr/entity-map all-attributes))
(def all-attribute-validator (attr/make-attribute-validator all-attributes))
