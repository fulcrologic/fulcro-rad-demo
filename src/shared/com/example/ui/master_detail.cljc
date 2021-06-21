(ns com.example.ui.master-detail
  (:require
    #?(:clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div label input]]
       :cljs [com.fulcrologic.fulcro.dom :as dom :refer [div label input]])
    [edn-query-language.core :as eql]
    [clojure.string :as str]
    [com.example.model.account :as account]
    [com.example.model :as model]
    [com.example.model.timezone :as timezone]
    [com.example.ui.address-forms :refer [AddressForm]]
    [com.example.ui.file-forms :refer [FileForm]]
    [com.fulcrologic.fulcro.algorithms.tempid :refer [tempid]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.form :as form :refer [defsc-form]]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.options-util :refer [child-classes]]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.routing :refer [route-to!]]
    [com.fulcrologic.rad.semantic-ui-options :as suo]
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.report-options :as ro]))

(form/defsc-form AccountForm [this props]
  {fo/id             account/id
   fo/attributes     [account/name
                      account/role
                      account/email
                      account/active?
                      account/addresses]
   fo/default-values {:account/active?         true
                      :account/primary-address {}
                      :account/addresses       [{}]}
   fo/validator      model/all-attribute-validator
   fo/title          "Edit Account"
   fo/cancel-route   :none
   fo/subforms       {:account/addresses {fo/ui            AddressForm
                                          fo/title         "Additional Addresses"
                                          fo/sort-children (fn [addresses] (sort-by :address/zip addresses))
                                          fo/can-delete?   (fn [parent _] (< 1 (count (:account/addresses (comp/props parent)))))
                                          fo/can-add?      (fn [parent _]
                                                             (and
                                                               (< (count (:account/addresses (comp/props parent))) 4)
                                                               :prepend))}}})

(defmutation close-detail [{:keys [asm-id detail]}]
  (action [{:keys [app]}]
    (uism/trigger! app asm-id :event/close-detail {:detail detail})))

(defstatemachine master-detail-report-machine
  (-> report/report-machine
    (assoc-in [::uism/aliases :active-details] [:actor/report :ui/active-details])
    (update-in [::uism/states :state/gathering-parameters ::uism/events]
      assoc
      :event/edit-detail {::uism/handler (fn [{::uism/keys [fulcro-app event-data] :as env}]
                                           (let [{:keys [id form join-key]} event-data
                                                 report-ident (uism/actor->ident env :actor/report)
                                                 join-path    (conj report-ident join-key)
                                                 form-ident   (assoc (comp/get-ident form {}) 1 id)]
                                             (when (and id form join-key)
                                               (form/start-form! fulcro-app id form
                                                 {:on-saved  [(close-detail {:asm-id report-ident :detail join-key})]
                                                  :on-cancel [(close-detail {:asm-id report-ident :detail join-key})]})
                                               (-> env
                                                 (uism/apply-action assoc-in join-path form-ident)
                                                 (uism/update-aliased :active-details (fnil conj #{}) join-key)))))}
      :event/create-detail {::uism/handler (fn [{::uism/keys [fulcro-app event-data] :as env}]
                                             (let [{:keys [form join-key]} event-data
                                                   id           (tempid)
                                                   report-ident (uism/actor->ident env :actor/report)
                                                   join-path    (conj report-ident join-key)
                                                   form-ident   (assoc (comp/get-ident form {}) 1 id)]
                                               (when (and form join-key)
                                                 (form/start-form! fulcro-app id form
                                                   {:on-saved  [(close-detail {:asm-id report-ident :detail join-key})]
                                                    :on-cancel [(close-detail {:asm-id report-ident :detail join-key})]})
                                                 (-> env
                                                   (uism/apply-action assoc-in join-path form-ident)
                                                   (uism/update-aliased :active-details (fnil conj #{}) join-key)))))}
      :event/close-detail {::uism/handler (fn [{{:keys [detail]} ::uism/event-data :as env}]
                                            (uism/update-aliased env :active-details disj detail))})))

(defn- form-at-key [this k]
  (let [{:keys [children]} (eql/query->ast (comp/get-query this))]
    (some (fn [{:keys [key component]}]
            (when (and component (= key k))
              component))
      children)))

(defn create! [this form-key]
  (let [Form (form-at-key this form-key)]
    (uism/trigger! this (comp/get-ident this) :event/create-detail {:form     Form
                                                                    :join-key form-key})))

(defn edit! [this form-key id]
  (let [Form (form-at-key this form-key)]
    (uism/trigger! this (comp/get-ident this) :event/edit-detail {:id       id
                                                                  :form     Form
                                                                  :join-key form-key})))

(report/defsc-report AccountList [this {:ui/keys [active-details account] :as props}]
  {ro/title               "All Accounts"
   ro/query-inclusions    [:ui/active-details {:ui/account (comp/get-query AccountForm)}]
   ro/machine             master-detail-report-machine
   suo/rendering-options  {suo/action-button-render      (fn [this {:keys [key onClick label]}]
                                                           (when (= key ::new-account)
                                                             (dom/button :.ui.tiny.basic.button {:onClick onClick}
                                                               (dom/i {:className "icon user"})
                                                               label)))
                           suo/report-table-class        "ui very compact celled selectable table"
                           suo/report-table-header-class (fn [this i] (case i
                                                                        0 ""
                                                                        1 "center aligned"
                                                                        "collapsing"))
                           suo/report-table-cell-class   (fn [this i] (case i
                                                                        0 ""
                                                                        1 "center aligned"
                                                                        "collapsing"))}
   ro/column-formatters   {:account/active? (fn [this v] (if v "Yes" "No"))
                           :account/name    (fn [this v {:account/keys [id]}]
                                              (dom/a {:onClick (fn [] (edit! this :ui/account id))} v))}
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
                                            :action (fn [this _] (create! this :ui/account))}
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

   ro/route               "account-master-detail"}
  (log/spy :info active-details)
  (if (seq active-details)
    (dom/div :.ui.grid
      (div :.row
        (div :.eight.wide.column
          (report/render-layout this))
        (div :.eight.wide.column
          (let [ui-account (comp/factory AccountForm)]
            (ui-account account)))))
    (report/render-layout this)))

