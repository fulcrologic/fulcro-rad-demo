(ns com.example.ui.address-forms
  (:require
    [com.example.model.address :as address]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.form :as form]))

(form/defsc-form AddressForm [this props]
  {fo/id           address/id
   fo/attributes   [address/street address/city address/state address/zip]
   fo/cancel-route ["landing-page"]
   fo/route-prefix "address"
   fo/title        "Edit Address"
   fo/layout       [[:address/street]
                    [:address/city :address/state :address/zip]]})

