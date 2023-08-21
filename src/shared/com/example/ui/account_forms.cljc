(ns com.example.ui.account-forms
  (:require
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [clojure.string :as str]
    [com.example.model :as model]
    [com.example.model.account :as account]
    [com.example.model.timezone :as timezone]
    [com.example.ui.address-forms :refer [AddressForm]]
    [com.example.ui.file-forms :refer [FileForm]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.raw.components :as rc]
    [com.fulcrologic.rad.blob :as blob]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.picker-options :as po]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.rad.semantic-ui-options :as suo]))

#_(def account-validator
    "Here's how to make a validator, possibly to override a validation defined on the attributes.
     Usually `ao/valid?` with an `attr/make-attribute-validator` is sufficient. "
    (fs/make-validator (fn [form field]
                         (case field
                           ;; Override what the attribute asks for. Comment this out and see that it uses
                           ;; the attribute's validation, which is lower case instead (also comment out the custom
                           ;; validation message on the form).
                           :account/email (let [prefix (or
                                                         (some-> form
                                                           :account/name
                                                           (str/split #"\s")
                                                           (first)
                                                           (str/upper-case))
                                                         "")]
                                            (str/starts-with? (get form field "") prefix))
                           (= :valid (model/all-attribute-validator form field))))))

;; NOTE: Limitation: Each "storage location" requires a form. The ident of the component matches the identity
;; of the item being edited. Thus, if you want to edit things that are related to a given entity, you must create
;; another form entity to stand in for it so that its ident is represented.  This allows us to use proper normalized
;; data in forms when "mixing" server side "entities/tables/documents".

(form/defsc-form TagForm [this props]
  {fo/id         account/tag-id
   fo/attributes [account/tag-label]})

(form/defsc-form AccountForm [this props]
  {fo/id              account/id
   ; fo/debug? true
   ;   ::form/read-only?          true
   fo/query-inclusion [(blob/status-key :account/avatar) ; IMPORTANT: For image upload to "look right" you need to include these in your query
                       (blob/url-key :account/avatar)
                       (blob/progress-key :account/avatar)]
   fo/attributes      [account/avatar
                       account/name
                       account/primary-address
                       account/role timezone/zone-id account/email
                       account/active? account/addresses
                       account/files
                       account/tags]
   fo/default-values  {:account/active?         true
                       :account/primary-address {}
                       :account/addresses       [{}]}
   ;fo/validator           account-validator
   ;fo/validation-messages {:account/email "You must use your UPPER case first name as your email address name."}
   fo/route-prefix    "account"
   fo/title           "Edit Account"
   ;; NOTE: any form can be used as a subform, but when you do so you must add addl config here
   ;; so that computed props can be sent to the form to modify its layout. Subforms, for example,
   ;; don't get top-level controls like "Save" and "Cancel".
   fo/field-styles    {:account/tags :pick-many}
   fo/field-options   {:account/tags {:style             :dropdown
                                      fo/title           "Add tag"
                                      po/form            TagForm
                                      po/quick-create    (fn [v] {:tag/id    (tempid/tempid)
                                                                  :tag/label v})
                                      po/query-key       :all-tags
                                      po/query-component (rc/nc [:tag/id :tag/label])
                                      po/options-xform   (fn [_ options] (mapv
                                                                           (fn [{:tag/keys [id label]}]
                                                                             {:text label :value [:tag/id id]})
                                                                           (sort-by :tag/label options)))
                                      po/cache-time-ms   30000}}
   fo/subforms        {:account/primary-address {fo/ui                      AddressForm
                                                 fo/title                   "Primary Address"
                                                 ::form/autocreate-on-load? true}
                       :account/files           {fo/ui                    FileForm
                                                 fo/title                 "Files"
                                                 fo/can-delete?           (fn [_ _] true)
                                                 fo/layout-styles         {:ref-container :file}
                                                 ::form/added-via-upload? true}
                       :account/addresses       {fo/ui            AddressForm
                                                 fo/title         "Additional Addresses"
                                                 fo/sort-children (fn [addresses] (sort-by :address/zip addresses))
                                                 fo/can-delete?   (fn [parent _] (< 1 (count (:account/addresses (comp/props parent)))))
                                                 fo/can-add?      (fn [parent _]
                                                                    (and
                                                                      (< (count (:account/addresses (comp/props parent))) 4)
                                                                      :prepend))}}})

(form/defsc-form BriefAccountForm [this props]
  {fo/id             account/id
   fo/controls       {}
   fo/attributes     [account/name
                      account/role
                      timezone/zone-id account/email
                      account/active?]
   fo/default-values {:account/active? true}})

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
  {ro/title               "All Accounts"
   ;; NOTE: You can uncomment these 3 lines to see how to switch over to using hand-written row rendering, with a list style
   ;::report/layout-style             :list
   ;::report/row-style                :list
   ;::report/BodyItem                 AccountListItem

   ;; The rendering options can also be set globally. Putting them on the component override globals.
   suo/rendering-options  {suo/action-button-render      (fn [this {:keys [key onClick label]}]
                                                           (when (= key ::new-account)
                                                             (dom/button :.ui.tiny.basic.button {:onClick onClick}
                                                               (dom/i {:className "icon user"})
                                                               label)))
                           suo/body-class                ""
                           suo/controls-class            ""
                           suo/layout-class              ""
                           suo/report-table-class        "ui very compact celled selectable table"
                           suo/report-table-header-class (fn [this i] (case i
                                                                        0 ""
                                                                        1 "center aligned"
                                                                        "collapsing"))
                           suo/report-table-cell-class   (fn [this i] (case i
                                                                        0 ""
                                                                        1 "center aligned"
                                                                        "collapsing"))}
   ro/form-links          {account/name AccountForm}
   ro/column-formatters   {:account/active? (fn [this v] (if v "Yes" "No"))}
   ro/column-headings     {:account/name "Account Name"}
   ro/columns             [account/name account/active?]
   ro/row-pk              account/id
   ro/source-attribute    :account/all-accounts
   ro/row-visible?        (fn [{::keys [filter-name]} {:account/keys [name]}]
                            (let [nm     (some-> name (str/lower-case))
                                  target (some-> filter-name (str/trim) (str/lower-case))]
                              (or
                                (nil? target)
                                (empty? target)
                                (and nm (str/includes? nm target)))))
   ro/run-on-mount?       true

   ro/initial-sort-params {:sort-by          :account/name
                           :ascending?       false
                           :sortable-columns #{:account/name}}

   ro/controls            {::new-account   {:type   :button
                                            :local? true
                                            :label  "New Account"
                                            :action (fn [this _] (form/create! this AccountForm))}
                           ::search!       {:type   :button
                                            :local? true
                                            :label  "Filter"
                                            :class  "ui basic compact mini red button"
                                            :action (fn [this _] (report/filter-rows! this))}
                           ::filter-name   {:type        :string
                                            :local?      true
                                            :placeholder "Type a partial name and press enter."
                                            :onChange    (fn [this _] (report/filter-rows! this))}
                           :show-inactive? {:type          :boolean
                                            :local?        true
                                            :style         :toggle
                                            :default-value false
                                            :onChange      (fn [this _] (control/run! this))
                                            :label         "Show Inactive Accounts?"}}

   ro/control-layout      {:action-buttons [::new-account]
                           :inputs         [[::filter-name ::search! :_]
                                            [:show-inactive?]]}

   ro/row-actions         [{:label     "Enable"
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

   ro/route               "accounts"})
