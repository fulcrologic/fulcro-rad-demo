(ns com.example.ui.item-forms
  (:require
    [com.example.model.item :as item]
    [com.fulcrologic.rad.form :as form]))

(form/defsc-form ItemForm [this props]
  {::form/id           item/id
   ::form/attributes   [item/item-name item/item-description item/item-in-stock item/item-price]
   ::form/cancel-route ["landing-page"]
   ::form/route-prefix "item"
   ::form/title        "Edit Item"})
