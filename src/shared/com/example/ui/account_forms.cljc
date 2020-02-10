(ns com.example.ui.account-forms
  (:require
    [clojure.string :as str]
    [com.example.model :as model]
    [com.example.model.account :as acct]
    [com.example.ui.address-forms :refer [AddressForm]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.report :as report]))

(def account-validator (fs/make-validator (fn [form field]
                                            (case field
                                              :account/email (let [prefix (or
                                                                            (some-> form
                                                                              (get :account/name)
                                                                              (str/split #"\s")
                                                                              (first)
                                                                              (str/lower-case))
                                                                            "")]
                                                               (str/starts-with? (get form field) prefix))
                                              (= :valid (model/all-attribute-validator form field))))))

;; NOTE: Limitation: Each "storage location" requires a form. The ident of the component matches the identity
;; of the item being edited. Thus, if you want to edit things that are related to a given entity, you must create
;; another form entity to stand in for it so that its ident is represented.  This allows us to use proper normalized
;; data in forms when "mixing" server side "entities/tables/documents".
(form/defsc-form AccountForm [this props]
  {::form/id                  acct/id
   ::form/attributes          [acct/name
                               ;; Not working completely yet...
                               ;;acct/primary-address
                               acct/role acct/time-zone acct/email acct/active? acct/addresses]
   ::form/default             {:account/active?   true
                               ;;:account/primary-address {}
                               :account/addresses [{}]}
   ::form/validator           account-validator
   ::form/validation-messages {:account/email (fn [_] "Must start with your lower-case first name")}
   ::form/cancel-route        ["landing-page"]
   ::form/route-prefix        "account"
   ::form/title               "Edit Account"
   ;; NOTE: any form can be used as a subform, but when you do so you must add addl config here
   ;; so that computed props can be sent to the form to modify its layout. Subforms, for example,
   ;; don't get top-level controls like "Save" and "Cancel".
   ::form/subforms            {#_#_:account/primary-address {::form/ui              AddressForm
                                                             ::form/can-delete-row? (fn [parent item] false)
                                                             ::form/can-add-row?    (fn [parent] false)}
                               :account/addresses {::form/ui              AddressForm
                                                   ::form/can-delete-row? (fn [parent item] (< 1 (count (:account/addresses parent))))
                                                   ::form/can-add-row?    (fn [parent] (< (count (:account/addresses parent)) 2))
                                                   ::form/add-row-title   "Add Address"
                                                   ;; Use computed props to inform subform of its role.
                                                   ::form/subform-style   :inline}}})

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

