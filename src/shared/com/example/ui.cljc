(ns com.example.ui
  (:require
    [com.example.model.account :as acct]
    [com.example.model.address :as address]
    [com.example.ui.login-dialog :refer [LoginForm]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [com.fulcrologic.fulcro.routing.dynamic-routing :refer [defrouter]]
    [com.fulcrologic.rad :as rad]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.controller :as controller]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.ids :refer [new-uuid]]
    [com.fulcrologic.rad.rendering.semantic-ui.semantic-ui-controls]
    [com.fulcrologic.rad.report :as report]
    [taoensso.timbre :as log]))

;; NOTE: Limitation: Each "storage location" requires a form. The ident of the component matches the identity
;; of the item being edited. Thus, if you want to edit things that are related to a given entity, you must create
;; another form entity to stand in for it so that its ident is represented.  This allows us to use proper normalized
;; data in forms when "mixing" server side "entities/tables/documents".
(form/defsc-form AddressForm [this props]
  {::form/id           address/id
   ::form/attributes   [address/street address/city address/state address/zip]
   ::form/cancel-route ["landing-page"]
   ::form/route-prefix "address"
   ::form/title        "Edit Address"
   ::form/layout       [[::address/street]
                        [::address/city ::address/state ::address/zip]]})

(form/defsc-form AccountForm [this props]
  {::form/id           acct/id
   ::form/attributes   [acct/name acct/addresses acct/email acct/active?]
   ::form/read-only?   {::acct/email true}
   ::form/cancel-route ["landing-page"]
   ::form/route-prefix "account"
   ::form/title        "Edit Account"
   ;; NOTE: any form can be used as a subform, but when you do so you must add addl config here
   ;; so that computed props can be sent to the form to modify its layout. Subforms, for example,
   ;; don't get top-level controls like "Save" and "Cancel".
   ::form/subforms     {::acct/addresses {::form/ui              AddressForm
                                          ::form/can-delete-row? (fn [parent item] (< 1 (count (::acct/addresses parent))))
                                          ::form/can-add-row?    (fn [parent] true)
                                          ::form/add-row-title   "Add Address"
                                          ;; Use computed props to inform subform of its role.
                                          ::form/subform-style   :inline}}})

(defsc AccountListItem [this {::acct/keys [id name active? last-login] :as props}]
  {::report/columns         [::acct/name ::acct/active? ::acct/last-login]
   ::report/column-headings ["Name" "Active?" "Last Login"]
   ::report/edit-form       AccountForm
   :query                   [::acct/id ::acct/name ::acct/active? ::acct/last-login]
   :ident                   ::acct/id}
  #_(dom/div :.item
      (dom/i :.large.github.middle.aligned.icon)
      (div :.content
        (dom/a :.header {:onClick (fn [] (form/edit! this AccountForm id))} name)
        (dom/div :.description
          (str (if active? "Active" "Inactive") ". Last logged in " last-login)))))

(def ui-account-list-item (comp/factory AccountListItem {:keyfn ::acct/id}))

(report/defsc-report AccountList [this props]
  {::report/BodyItem         AccountListItem
   ::report/source-attribute ::acct/all-accounts
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
  {:router-targets [LandingPage AccountList AccountForm]})

(def ui-main-router (comp/factory MainRouter))

(auth/defauthenticator Authenticator {:local LoginForm})

(def ui-authenticator (comp/factory Authenticator))

(defsc Root [this {:keys [authenticator router]}]
  {:query         [{:authenticator (comp/get-query Authenticator)}
                   {:router (comp/get-query MainRouter)}]
   :initial-state {:router        {}
                   :authenticator {}}}
  (dom/div
    (div :.ui.top.menu
      (div :.ui.item "Demo Application")
      ;; TODO: Show how we can check authority to hide UI
      (dom/a :.ui.item {:onClick (fn [] (form/edit! this AccountForm (new-uuid 1)))} "My Account")
      #_(dom/a :.ui.item {:onClick (fn []
                                     (controller/route-to! this :main-controller
                                       ["account" "create" (str (new-uuid))]))} "New Account")
      (dom/a :.ui.item {:onClick (fn []
                                   (controller/route-to! this :main-controller ["accounts"]))} "List Accounts"))
    (div :.ui.container.segment
      (ui-authenticator authenticator)
      (ui-main-router router))))

(def ui-root (comp/factory Root))

