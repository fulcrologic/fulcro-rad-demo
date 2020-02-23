(ns com.example.model
  (:require
    [com.example.model.account :as account]
    [com.example.model.item :as item]
    [com.example.model.invoice :as invoice]
    [com.example.model.line-item :as line-item]
    [com.example.model.address :as address]
    [com.example.model.file :as m.file]
    [com.fulcrologic.rad.attributes :as attr]))

(def all-attributes (vec (concat
                           account/attributes
                           address/attributes
                           item/attributes
                           invoice/attributes
                           line-item/attributes
                           m.file/attributes)))

(def all-attribute-validator (attr/make-attribute-validator all-attributes))
