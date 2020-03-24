(ns com.example.ui.address-forms
  (:require
    [com.example.model.address :as address]
    [com.fulcrologic.rad.form :as form]))

(form/defsc-form AddressForm [this props]
  {::form/id                address/id
   ::form/attributes        [address/street address/city address/state address/zip]
   ::form/enumeration-order {:address/state (sort-by #(get address/states %) (keys address/states))}
   ::form/cancel-route      ["landing-page"]
   ::form/route-prefix      "address"
   ::form/title             "Edit Address"
   ::form/layout            [[:address/street]
                             [:address/city :address/state :address/zip]]})

