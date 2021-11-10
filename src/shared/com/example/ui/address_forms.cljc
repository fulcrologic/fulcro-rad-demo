(ns com.example.ui.address-forms
  (:require
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [com.example.model.address :as address]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.form :as form]
    [taoensso.timbre :as log]))

(form/defsc-form AddressForm [this props]
  {fo/id           address/id
   fo/attributes   [address/street address/city address/state address/zip]
   fo/cancel-route ["landing-page"]
   fo/route-prefix "address"
   fo/title        "Edit Address"
   fo/layout       [[:address/street]
                    [:address/city :address/state :address/zip]]}
  ;; When this is rendered as primary address, unwrap it and render
  ;; JUST the form fields, which will make it look more like the
  ;; subform is part of the primary.
  (if (= :account/primary-address (form/parent-relation this))
    (form/render-form-fields this props)
    (form/render-layout this props)))

