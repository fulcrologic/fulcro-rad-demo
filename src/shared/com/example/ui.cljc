(ns com.example.ui
  (:require
    [com.example.model.account :as acct]
    [com.example.model.address :as address]
    [com.example.model.item :as item]
    [com.example.model.line-item :as line-item]
    [com.example.model.invoice :as invoice]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.rad.rendering.semantic-ui.components :refer [ui-wrapped-dropdown]]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.rad.type-support.decimal :as math]))

(form/defsc-form ItemForm [this props]
  {::form/id           item/id
   ::form/attributes   [item/item-name item/item-description item/item-in-stock item/item-price]
   ::form/cancel-route ["landing-page"]
   ::form/route-prefix "item"
   ::form/title        "Edit Item"})

(form/defsc-form AddressForm [this props]
  {::form/id                address/id
   ::form/attributes        [address/street address/city address/state address/zip]
   ::form/enumeration-order {:address/state (sort-by #(get address/states %) (keys address/states))}
   ::form/cancel-route      ["landing-page"]
   ::form/route-prefix      "address"
   ::form/title             "Edit Address"
   ::form/layout            [[:address/street]
                             [:address/city :address/state :address/zip]]})

;; NOTE: Limitation: Each "storage location" requires a form. The ident of the component matches the identity
;; of the item being edited. Thus, if you want to edit things that are related to a given entity, you must create
;; another form entity to stand in for it so that its ident is represented.  This allows us to use proper normalized
;; data in forms when "mixing" server side "entities/tables/documents".
(form/defsc-form AccountForm [this props]
  {::form/id           acct/id
   ::form/attributes   [acct/name acct/email acct/active? acct/addresses]
   ::form/default      {:account/active?   true
                        :account/addresses [{}]}
   ::form/read-only?   {:account/email true}
   ::form/cancel-route ["landing-page"]
   ::form/route-prefix "account"
   ::form/title        "Edit Account"
   ;; NOTE: any form can be used as a subform, but when you do so you must add addl config here
   ;; so that computed props can be sent to the form to modify its layout. Subforms, for example,
   ;; don't get top-level controls like "Save" and "Cancel".
   ::form/subforms     {:account/addresses {::form/ui              AddressForm
                                            ::form/can-delete-row? (fn [parent item] (< 1 (count (:account/addresses parent))))
                                            ::form/can-add-row?    (fn [parent] true)
                                            ::form/add-row-title   "Add Address"
                                            ;; Use computed props to inform subform of its role.
                                            ::form/subform-style   :inline}}})



(form/defsc-form LineItemForm [this props]
  {::form/id           line-item/id
   ::form/attributes   [line-item/item line-item/quantity]
   ::form/cancel-route ["landing-page"]
   ::form/route-prefix "line-item"
   ::form/title        "Line Items"
   ::form/layout       [[:line-item/item :line-item/quantity]]
   ::form/subforms     {:line-item/item {::form/ui            form/ToOneEntityPicker
                                         ::form/pick-one      {:options/query-key :item/all-items
                                                               :options/subquery  [:item/id :item/name :item/price]
                                                               :options/transform (fn [{:item/keys [id name price]}]
                                                                                    {:text (str name " - " (math/numeric->currency-str price)) :value [:item/id id]})}
                                         ::form/label         "Item"
                                         ;; Use computed props to inform subform of its role.
                                         ::form/subform-style :inline}}})

(form/defsc-form InvoiceForm [this props]
  {::form/id           invoice/id
   ::form/attributes   [invoice/customer invoice/line-items]
   ::form/subforms     {:invoice/customer   {::form/ui            form/ToOneEntityPicker
                                             ::form/pick-one      {:options/query-key :account/all-accounts
                                                                   :options/subquery  [:account/id :account/name :account/email]
                                                                   :options/transform (fn [{:account/keys [id name email]}]
                                                                                        {:text (str name ", " email) :value [:account/id id]})}
                                             ::form/label         "Customer"
                                             ;; Use computed props to inform subform of its role.
                                             ::form/subform-style :inline}
                        :invoice/line-items {::form/ui              LineItemForm
                                             ::form/can-delete-row? (fn [parent item] true)
                                             ::form/can-add-row?    (fn [parent] true)
                                             ::form/add-row-title   "Add Item"
                                             ;; Use computed props to inform subform of its role.
                                             ::form/subform-style   :inline}}
   ::form/cancel-route ["landing-page"]
   ::form/route-prefix "invoice"
   ::form/title        "Edit Invoice"})

(defsc AccountListItem [this {:account/keys [id name active? last-login] :as props}]
  {::report/columns         [:account/name :account/active? :account/last-login]
   ::report/column-headings ["Name" "Active?" "Last Login"]
   ::report/row-actions     {:delete (fn [this id] (form/delete! this :account/id id))}
   ::report/edit-form       AccountForm
   :query                   [:account/id :account/name :account/active? :account/last-login]
   :ident                   :account/id}
  #_(dom/div :.item
      (dom/i :.large.github.middle.aligned.icon)
      (div :.content
        (dom/a :.header {:onClick (fn [] (form/edit! this AccountForm id))} name)
        (dom/div :.description
          (str (if active? "Active" "Inactive") ". Last logged in " last-login)))))

(def ui-account-list-item (comp/factory AccountListItem {:keyfn :account/id}))

(report/defsc-report AccountList [this props]
  {::report/BodyItem         AccountListItem
   ::report/source-attribute :account/all-accounts
   ::report/parameters       {:ui/show-inactive? :boolean}
   ::report/route            "accounts"})

(defsc InvoiceLineItem [this {:keys [:invoice/id :invoice/customer :invoice/line-items] :as props}]
  {:query [:invoice/id :invoice/customer :invoice/line-items]
   :ident :invoice/id})

(def ui-invoice-line-item (comp/factory InvoiceLineItem {:keyfn :invoice/id}))

#_(report/defsc-report InvoiceList [this props]
    {::report/BodyItem         InvoiceLineItem
     ::report/source-attribute :account/all-accounts
     ::report/parameters       {:ui/show-inactive? :boolean}
     ::report/route            "accounts"})

(defsc LandingPage [this props]
  {:query         ['*]
   :ident         (fn [] [:component/id ::LandingPage])
   :initial-state {}
   :route-segment ["landing-page"]}
  (dom/div "Hello World"))

;; This will just be a normal router...but there can be many of them.
(defrouter MainRouter [this props]
  {:router-targets [LandingPage ItemForm InvoiceForm AccountList AccountForm]})

(def ui-main-router (comp/factory MainRouter))

(auth/defauthenticator Authenticator {:local LoginForm})

(def ui-authenticator (comp/factory Authenticator))

(defsc Root [this {::auth/keys [authorization]
                   :keys       [authenticator router]}]
  {:query         [{:authenticator (comp/get-query Authenticator)}
                   {:router (comp/get-query MainRouter)}
                   ::auth/authorization]
   :initial-state {:router        {}
                   :authenticator {}}}
  (let [logged-in? (= :success (some-> authorization :local ::auth/status))
        username   (some-> authorization :local :account/name)]
    (dom/div
      (div :.ui.top.menu
        (div :.ui.item "Demo")
        (when true #_logged-in?
          (comp/fragment
            (dom/a :.ui.item {:onClick (fn [] (form/edit! this AccountForm (new-uuid 101)))} "My Account")
            (dom/a :.ui.item {:onClick (fn [] (form/edit! this ItemForm (new-uuid 200)))} "Some Item")
            (dom/a :.ui.item {:onClick (fn [] (form/create! this AccountForm))} "New Account")
            (dom/a :.ui.item {:onClick (fn [] (form/create! this InvoiceForm))} "New Invoice")
            (dom/a :.ui.item {:onClick (fn [] (form/delete! this :com.example.model.account/id (new-uuid 102)))}
              "Delete account 2")
            (dom/a :.ui.item {:onClick (fn []
                                         (dr/change-route this (dr/path-to AccountList)))} "List Accounts")))
        (div :.right.menu
          (if logged-in?
            (comp/fragment
              (div :.ui.item
                (str "Logged in as " username))
              (div :.ui.item
                (dom/button :.ui.button {:onClick #(auth/logout! this :local)}
                  "Logout")))
            (div :.ui.item
              (dom/button :.ui.primary.button {:onClick #(auth/authenticate! this :local nil)}
                "Login")))))
      (div :.ui.container.segment
        (ui-authenticator authenticator)
        (ui-main-router router)))))

(def ui-root (comp/factory Root))
