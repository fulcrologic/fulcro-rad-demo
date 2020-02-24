(ns com.example.ui.account-forms
  (:require
    [clojure.string :as str]
    [com.example.model :as model]
    [com.example.model.account :as acct]
    [com.example.ui.address-forms :refer [AddressForm]]
    [com.example.ui.file-forms :refer [FileForm]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.blob :as blob]))

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
   ::form/attributes          [acct/avatar
                               acct/name
                               acct/primary-address
                               ;; TODO: Fix performance of large dropdowns (time zone)
                               acct/role #_acct/time-zone acct/email
                               acct/active? acct/addresses
                               acct/files]
   ::form/default             {:account/active?         true
                               :account/primary-address {}
                               :account/addresses       [{}]}
   ::form/validator           account-validator
   ::form/validation-messages {:account/email (fn [_] "Must start with your lower-case first name")}
   ::form/cancel-route        ["landing-page"]
   ::form/route-prefix        "account"
   ::form/title               "Edit Account"
   ;; NOTE: any form can be used as a subform, but when you do so you must add addl config here
   ;; so that computed props can be sent to the form to modify its layout. Subforms, for example,
   ;; don't get top-level controls like "Save" and "Cancel".
   ::form/subforms            {:account/primary-address {::form/ui                  AddressForm
                                                         ::form/title               "Primary Address"
                                                         ::form/autocreate-on-load? true}
                               :account/files           {::form/ui                FileForm
                                                         ::form/title             "Files"
                                                         ::form/can-delete?       (fn [_ _] true)
                                                         ::form/layout-styles     {:ref-container :file}
                                                         ::form/added-via-upload? true}
                               :account/addresses       {::form/ui          AddressForm
                                                         ::form/title       "Additional Addresses"
                                                         ::form/can-delete? (fn [parent item] (< 1 (count (:account/addresses parent))))
                                                         ::form/can-add?    (fn [parent] (< (count (:account/addresses parent)) 2))}}})

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
  {::report/title                    "All Accounts"
   ::report/source-attribute         :account/all-accounts
   ::report/BodyItem                 AccountListItem
   ::report/run-on-mount?            true
   ::report/run-on-parameter-change? true
   ::report/parameters               {:ui/show-inactive? {:type  :boolean
                                                          :label "Show Inactive Accounts?"}}
   ::report/initial-parameters       {:ui/show-inactive? false}
   ::report/route                    "accounts"})

