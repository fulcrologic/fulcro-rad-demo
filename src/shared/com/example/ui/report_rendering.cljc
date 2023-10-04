(ns com.example.ui.report-rendering
  (:require
    #?@(:cljs [[com.fulcrologic.fulcro.dom :as dom :refer [div]]
               [com.fulcrologic.semantic-ui.modules.popup.ui-popup :refer [ui-popup]]
               [com.fulcrologic.semantic-ui.modules.popup.ui-popup-content :refer [ui-popup-content]]]
        :clj  [[com.fulcrologic.fulcro.dom-server :as dom :refer [div]]])
    [com.fulcrologic.fulcro-i18n.i18n :refer [trc]]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.rendering.semantic-ui.form :as sui-form]
    [com.fulcrologic.rad.rendering.semantic-ui.report :as sur]
    [com.fulcrologic.rad.report :as report]
    [com.fulcrologic.rad.report-options :as ro]
    [com.fulcrologic.rad.report-render :as rr]
    [com.fulcrologic.rad.semantic-ui-options :as suo]
    [com.fulcrologic.semantic-ui.addons.pagination.ui-pagination :as sui-pagination]
    [taoensso.timbre :as log]))

(defmethod rr/render-report [:default :multimethod] [this options]
  (dom/div :.ui.container nil
    (rr/render-controls this options)
    (rr/render-body this options)))

(defmethod rr/render-controls [:default :multimethod] [report-instance options]
  (let [controls (control/component-controls report-instance)
        {:keys [::report/paginate?]} options
        {::suo/keys [report-action-button-grouping]} (suo/get-rendering-options report-instance)
        {:keys [input-layout action-layout]} (control/standard-control-layout report-instance)
        {:com.fulcrologic.rad.container/keys [controlled?]} (comp/get-computed report-instance)]
    (comp/fragment
      (div {:className (or
                         (?! (suo/get-rendering-options report-instance suo/controls-class))
                         "ui basic segment")}
        (dom/h3 :.ui.header nil
          (or (some-> report-instance comp/component-options ::report/title (?! report-instance)) (trc "a table that shows a list of rows" "Report"))
          (div {:className (or (?! report-action-button-grouping report-instance)
                             "ui right floated buttons")}
            (keep (fn [k]
                    (let [control (get controls k)]
                      (when (and (or (not controlled?) (:local? control))
                              (-> (get control :visible? true)
                                (?! report-instance)))
                        (control/render-control report-instance k control))))
              action-layout)))
        (div :.ui.form nil
          (map-indexed
            (fn [idx row]
              (let [nfields (count (filter #(or (not controlled?) (:local? (get controls %))) row))]
                (div {:key       idx
                      :className (or
                                   (?! (suo/get-rendering-options report-instance suo/report-controls-row-class) report-instance idx)
                                   (sui-form/n-fields-string nfields))}
                  (keep #(let [control (get controls %)]
                           (when (or (not controlled?) (:local? control))
                             (control/render-control report-instance % control))) row))))
            input-layout))
        (when paginate?
          (let [page-count (report/page-count report-instance)]
            (when (> page-count 1)
              (div :.ui.two.column.centered.grid nil
                (div :.two.wide.column nil
                  (div {:style {:paddingTop "4px"}}
                    #?(:cljs
                       (sui-pagination/ui-pagination {:activePage   (report/current-page report-instance)
                                                      :onPageChange (fn [_ data]
                                                                      (report/goto-page! report-instance (comp/isoget data "activePage")))
                                                      :totalPages   page-count
                                                      :size         "tiny"}))))))))))))

(defmethod rr/render-body :default [this options]
  (let [rows         (report/current-rows this)
        selected-row (report/currently-selected-row this)]
    (dom/table :.ui.compact.table {}
      (rr/render-header this options)
      (dom/tbody nil
        (map-indexed
          (fn [idx row] (rr/render-row this options
                          (assoc row ::report/idx idx ::report/highlighted? (= idx selected-row))))
          rows))
      (rr/render-footer this options))))

(defmethod rr/render-header :default [this options]
  (let [column-headings  (report/column-heading-descriptors this options)
        props            (comp/props this)
        sort-params      (-> props :ui/parameters ::report/sort)
        {::report/keys [compare-rows]} options
        sortable?        (if-not (boolean compare-rows)
                           (constantly false)
                           (if-let [sortable-columns (some-> sort-params :sortable-columns set)]
                             (fn [{::attr/keys [qualified-key]}] (contains? sortable-columns qualified-key))
                             (constantly true)))
        sui-header-class (suo/get-rendering-options this suo/report-table-header-class)
        ascending?       (and sortable? (:ascending? sort-params))
        sorting-by       (and sortable? (:sort-by sort-params))]
    (dom/thead nil
      (dom/tr nil
        (map-indexed (fn [idx {:keys [label help column]}]
                       (let [alignment-class (sur/column-alignment-class this column)]
                         (dom/th {:key     idx
                                  :classes [alignment-class (?! sui-header-class this idx)]}
                           (if (sortable? column)
                             (dom/a {:onClick (fn [evt]
                                                (evt/stop-propagation! evt)
                                                (report/sort-rows! this column))}
                               label
                               (when (= sorting-by (::attr/qualified-key column))
                                 (if ascending?
                                   (dom/i :.angle.down.icon)
                                   (dom/i :.angle.up.icon))))
                             label)
                           #?(:cljs
                              (when help
                                (ui-popup {:trigger (dom/i :.ui.circle.info.icon)}
                                  (ui-popup-content {}
                                    help)))))))
          column-headings)))))

(defmethod rr/render-footer :default [_ _]
  (dom/tfoot nil
    (dom/tr nil
      (dom/td {:colSpan 3
               :style   {:color    "lightgrey"
                         :fontSize "8pt"}}
        "Table by multimethods"))))

(comp/defsc TableRowLayout [_ {:keys [report-instance props] :as rp}]
  {}
  (let [{::report/keys [columns link links on-select-row]} (comp/component-options report-instance)
        links          (or links link)
        action-buttons (sur/row-action-buttons report-instance props)
        {::report/keys [idx highlighted?]} props
        sui-cell-class (suo/get-rendering-options report-instance suo/report-table-cell-class)]
    (dom/tr {:classes [(when highlighted? "active")]
             :onClick (fn [evt]
                        (evt/stop-propagation! evt)
                        (when-not (false? (suo/get-rendering-options report-instance suo/selectable-table-rows?))
                          (?! on-select-row report-instance props)
                          (report/select-row! report-instance idx)))}
      (map-indexed
        (fn [idx {::attr/keys [qualified-key] :as column}]
          (let [alignment-class (sur/column-alignment-class report-instance column)
                column-classes  (str alignment-class " " (report/column-classes report-instance column))]
            (dom/td {:key     (str "col-" qualified-key)
                     :classes [(?! sui-cell-class report-instance idx) column-classes]}
              (let [{:keys [edit-form entity-id]} (report/form-link report-instance props qualified-key)
                    link-fn (get links qualified-key)
                    label   (report/formatted-column-value report-instance props column)]
                (cond
                  edit-form (dom/a {:onClick (fn [evt]
                                               (evt/stop-propagation! evt)
                                               (form/edit! report-instance edit-form entity-id))} label)
                  (fn? link-fn) (dom/a {:onClick (fn [evt]
                                                   (evt/stop-propagation! evt)
                                                   (link-fn report-instance props))} label)
                  :else label)))))
        columns)
      (when action-buttons
        (dom/td {:key       "actions"
                 :className (or
                              (?! sui-cell-class report-instance (count columns))
                              "collapsing")}
          action-buttons)))))

(let [ui-table-row-layout (comp/factory TableRowLayout)]
  (defn render-table-row [report-instance row-class row-props]
    (ui-table-row-layout {:report-instance report-instance
                          :row-class       row-class
                          :props           row-props})))


(defmethod rr/render-row [:default :multimethod] [this options row]
  (render-table-row this (ro/BodyItem options) row))
