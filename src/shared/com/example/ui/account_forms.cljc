(ns com.example.ui.account-forms
  (:require
    [clojure.string :as str]
    [com.example.model :as model]
    [com.example.model.account :as account]
    [com.example.model.timezone :as timezone]
    [com.example.ui.address-forms :refer [AddressForm]]
    [com.example.ui.file-forms :refer [FileForm]]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :refer [defmutation]]
    [com.fulcrologic.rad.authorization :as auth]
    [com.fulcrologic.rad.type-support.cache-a-bools :as cb]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.options-util :as opts]))

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
  {fo/id                  account/id
   ;   ::form/read-only?          true
   fo/attributes          [;account/avatar
                           account/name
                           account/primary-address
                           account/role timezone/zone-id account/email
                           account/active? account/addresses
                           account/files]
   fo/default-values      {:account/active?         true
                           :account/primary-address {}
                           :account/addresses       [{}]}
   fo/validator           account-validator
   fo/validation-messages {:account/email (fn [_] "Must start with your lower-case first name")}
   fo/cancel-route        ["landing-page"]
   fo/route-prefix        "account"
   fo/title               "Edit Account"
   ;; NOTE: any form can be used as a subform, but when you do so you must add addl config here
   ;; so that computed props can be sent to the form to modify its layout. Subforms, for example,
   ;; don't get top-level controls like "Save" and "Cancel".
   fo/subforms            {:account/primary-address {::form/ui                  AddressForm
                                                     ::form/title               "Primary Address"
                                                     ::form/autocreate-on-load? true}
                           :account/files           {::form/ui                FileForm
                                                     ::form/title             "Files"
                                                     ::form/can-delete?       (fn [_ _] true)
                                                     ::form/layout-styles     {:ref-container :file}
                                                     ::form/added-via-upload? true}
                           :account/addresses       {::form/ui            AddressForm
                                                     ::form/title         "Additional Addresses"
                                                     ::form/sort-children (fn [addresses] (sort-by :address/zip addresses))
                                                     ::form/can-delete?   (fn [parent item] (< 1 (count (:account/addresses parent))))
                                                     ::form/can-add?      (fn [parent]
                                                                            (and
                                                                              (< (count (:account/addresses parent)) 4)
                                                                              :prepend))}}})

(defsc AccountListItem [this
                        {:account/keys [id name active?] :as props}
                        {:keys [report-instance row-class ::report/idx]}]
  {:query [:account/id :account/name :account/active?]
   :ident :account/id}
  (let [{:keys [edit-form entity-id]} (report/form-link report-instance props :account/name)]
    (dom/div :.item
      (dom/i :.large.github.middle.aligned.icon)
      (div :.content
        (if edit-form
          (dom/a :.header {:onClick (fn [] (form/edit! this edit-form entity-id))} name)
          (dom/div :.header name))
        (dom/div :.description
          (str (if active? "Active" "Inactive"))))))
  #_(dom/tr
      (dom/td :.right.aligned name)
      (dom/td (str active?))))

(report/defsc-report AccountList [this props]
  {ro/title                 "All Accounts"
   ;::report/layout-style             :list
   ;::report/row-style                :list
   ;::report/BodyItem                 AccountListItem
   ro/form-links            {account/name AccountForm}
   ro/field-formatters      {:account/name (fn [this v] v)}
   ::report/column-headings {:account/name "Account Name"}
   ro/columns               [account/name account/active?]
   ro/row-pk                account/id
   ro/source-attribute      :account/all-accounts
   ro/run-on-mount?         true

   ro/controls              {::new-account   {:type   :button
                                              :label  "New Account"
                                              :action (fn [this _] (form/create! this AccountForm))}
                             :show-inactive? {:type          :boolean
                                              :style         :toggle
                                              :default-value false
                                              :onChange      (fn [this _] (report/reload! this))
                                              :label         "Show Inactive Accounts?"}}

   ro/control-layout        {:action-buttons [::new-account]
                             :inputs         [[:show-inactive?]]}

   ro/row-actions           [{:label     "Enable"
                              :action    (fn [report-instance {:account/keys [id]}]
                                           #?(:cljs
                                              (comp/transact! report-instance [(account/set-account-active {:account/id      id
                                                                                                            :account/active? true})])))
                              ;:visible?  (fn [_ row-props] (not (:account/active? row-props)))
                              :disabled? (fn [_ row-props] (:account/active? row-props))}
                             {:label     "Disable"
                              :action    (fn [report-instance {:account/keys [id]}]
                                           #?(:cljs
                                              (comp/transact! report-instance [(account/set-account-active {:account/id      id
                                                                                                            :account/active? false})])))
                              ;:visible?  (fn [_ row-props] (:account/active? row-props))
                              :disabled? (fn [_ row-props] (not (:account/active? row-props)))}]

   ro/route                 "accounts"})

(comment

  (comp/get-query AccountList-Row))
