(ns com.example.ui.form-rendering
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom :refer [div]]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div]])
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.debugging :as debug]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.form-render :as fr]
    [com.fulcrologic.rad.form-render-options :as fro]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.rendering.semantic-ui.form :as rsf]
    [taoensso.encore :as enc]
    [taoensso.timbre :as log]))

(defn install!
  "Install multimethod rendering such that:

   * All keywords in the RAD model will derive from :default.
   * A form can use `fro/style :rad/multirender` to switch from the built-in plugin rendering to multimethod rendering.
  "
  [app attrs]
  (fr/allow-defaults! attrs)
  (fr/install-as! app :rad/multirender))

(defmethod fr/render-form [:default :rad/multirender] [{::form/keys [form-instance parent parent-relation master-form] :as renv} id-attr]
  (if (= form-instance master-form)
    (dom/div :.ui.container.form {:key (str (comp/get-ident form-instance))}
      (fr/render-header renv id-attr)
      (fr/render-fields renv id-attr)
      (fr/render-footer renv id-attr))
    (let [subform-options (log/spy :info (some-> parent (comp/component-options) fo/subforms parent-relation))
          can-delete?     (?! (fo/can-delete? subform-options) parent (comp/props form-instance))]
      (comp/fragment {:key (str (comp/get-ident form-instance))}
        (fr/render-header renv id-attr)
        (if can-delete?
          (dom/div :.ui.grid
            (dom/div :.fifteen.wide.cell
              (fr/render-fields renv id-attr))
            (dom/div :.one.wide.cell
              (dom/button :.ui.tiny.icon.button
                (dom/i :.times.icon))))
          (fr/render-fields renv id-attr))
        (fr/render-footer renv id-attr)))))

(defmethod fr/render-header :default [{::form/keys [master-form form-instance]} attr]
  (let [k    (ao/qualified-key attr)
        top? (= master-form form-instance)]
    (cond
      (and top? (ao/identity? attr))
      #_=> (let [props          (comp/props form-instance)
                 {::form/keys [title action-buttons]} (comp/component-options form-instance)
                 action-buttons (if action-buttons action-buttons form/standard-action-buttons)
                 title          (?! title form-instance props)]
             (div :.ui.basic.segment
               (div {:style {:display        "flex"
                             :justifyContent "space-between"
                             :flexWrap       "wrap"}}
                 (dom/h3 :.ui.header {:style {:wordWrap "break-word" :maxWidth "100%"}} title)
                 (div :.ui.buttons {:style {:textAlign "right" :display "inline" :flexGrow "1"}}
                   (keep #(control/render-control master-form %) action-buttons)))))
      (= :many (ao/cardinality attr))
      #_=> (let [{Form ::form/ui
                  :as  subform-options} (-> form-instance (comp/component-options) fo/subforms k)
                 options  (some-> Form (comp/component-options))
                 props    (get (comp/props form-instance) k)
                 can-add? (?! (fo/can-add? subform-options) form-instance attr)
                 title    (?! (fo/title options) form-instance props)]
             (dom/h3 (or title "") ent/nbsp ent/nbsp
               (when can-add?
                 (dom/button :.ui.tiny.icon.button
                   {:onClick (fn [] (form/add-child! form-instance k Form {:order :prepend}))}
                   (dom/i :.plus.icon))))))))

(defmethod fr/render-footer :default [renv attr])

(defn- render-layout* [env options k->attribute layout]
  (when #?(:clj true :cljs goog.DEBUG)
    (when-not (and (vector? layout) (every? vector? layout))
      (log/error "::form/layout must be a vector of vectors!")))
  (try
    (into []
      (map-indexed
        (fn [idx row]
          (div {:key idx :className (rsf/n-fields-string (count row))}
            (mapv (fn [col]
                    (enc/if-let [_    k->attribute
                                 attr (k->attribute col)]
                      (fr/render-field env attr)
                      (if (some-> options ::control/controls (get col))
                        (control/render-control (::form/form-instance env) col)
                        (log/error "Missing attribute (or lookup) for" col))))
              row)))
        layout))
    (catch #?(:clj Exception :cljs :default) _)))

(defsc TabbedLayout [this env {::form/keys [attributes tabbed-layout] :as options}]
  {:initLocalState (fn [this]
                     (try
                       {:current-tab 0
                        :tab-details (memoize
                                       (fn [attributes tabbed-layout]
                                         (let [k->attr           (rsf/attribute-map attributes)
                                               tab-labels        (filterv string? tabbed-layout)
                                               tab-label->layout (into {}
                                                                   (map vec)
                                                                   (partition 2 (mapv first (partition-by string? tabbed-layout))))]
                                           {:k->attr           k->attr
                                            :tab-labels        tab-labels
                                            :tab-label->layout tab-label->layout})))}
                       (catch #?(:clj Exception :cljs :default) _
                         (log/error "Cannot build tabs for tabbed layout. Check your tabbed-layout options for" (comp/component-name this)))))}
  (let [{:keys [tab-details current-tab]} (comp/get-state this)
        {:keys [k->attr tab-labels tab-label->layout]} (tab-details attributes tabbed-layout)
        active-layout (some->> current-tab
                        (get tab-labels)
                        (get tab-label->layout))]
    (div {:key (str current-tab)}
      (div :.ui.pointing.menu {}
        (map-indexed
          (fn [idx title]
            (dom/a :.item
              {:key     (str idx)
               :onClick #(comp/set-state! this {:current-tab idx})
               :classes [(when (= current-tab idx) "active")]}
              title)) tab-labels))
      (div :.ui.segment
        (render-layout* env options k->attr active-layout)))))

(def ui-tabbed-layout (comp/computed-factory TabbedLayout))


(defn render-layout [{::form/keys [form-instance] :as env} {::form/keys [attributes layout] :as options}]

  (let [{::form/keys [attributes layout tabbed-layout debug?] :as options} (comp/component-options form-instance)
        k->attr (rsf/attribute-map attributes)
        layout  (cond
                  (vector? layout) (render-layout* env options k->attr layout)
                  (vector? tabbed-layout) (rsf/ui-tabbed-layout env options)
                  :else (mapv (fn [attr] (fr/render-field env attr)) attributes))]
    (if (and #?(:clj false :cljs goog.DEBUG) debug?)
      (debug/top-bottom-debugger form-instance (comp/props form-instance)
        (constantly layout))
      layout)))

(defmethod fr/render-fields :default [{::form/keys [form-instance] :as renv}]
  (div :.ui.basic.segment
    (render-layout renv (comp/component-options form-instance))))

(defmethod fr/render-field [:ref :table] [{::form/keys [form-instance] :as renv} field-attr]
  (let [relation-key (ao/qualified-key field-attr)
        item         (-> form-instance comp/props relation-key)
        ItemForm     (comp/component-options form-instance fo/subforms relation-key fo/ui)
        item-options (comp/component-options ItemForm)
        to-many?     (= :many (ao/cardinality field-attr))]
    (if ItemForm
      (dom/table :.ui.table {:key (str relation-key)}
        (fr/render-header renv field-attr)
        (dom/tbody nil
          (if to-many?
            (mapv (fn [i] (form/render-subform form-instance relation-key ItemForm i)) item)
            (form/render-subform form-instance relation-key ItemForm item)))
        (fr/render-footer renv field-attr))
      (log/error "There is no Subform UI declared for" relation-key "on" (comp/component-name form-instance)))))

(defmethod fr/render-field [:ref :default] [{::form/keys [form-instance] :as renv} field-attr]
  (let [relation-key (ao/qualified-key field-attr)
        item         (-> form-instance comp/props relation-key)
        {ItemForm ::form/ui
         :as      subform-options} (comp/component-options form-instance fo/subforms relation-key)
        to-many?     (= :many (ao/cardinality field-attr))]
    (if ItemForm
      (comp/fragment {:key (str relation-key)}
        (fr/render-header renv field-attr)
        (if to-many?
          (mapv (fn [i] (form/render-subform form-instance relation-key ItemForm i)) item)
          (form/render-subform form-instance relation-key ItemForm item))
        (fr/render-footer renv field-attr))
      (log/error "There is no Subform UI declared for" relation-key "on" (comp/component-name form-instance)))))

(defmethod fr/render-header [:default :table] [{::form/keys [form-instance] :as env} field-attr]
  (let [{ItemForm    ::form/ui
         ::form/keys [can-add?]} (comp/component-options form-instance fo/subforms (ao/qualified-key field-attr))
        attrs (comp/component-options ItemForm fo/attributes)]
    (dom/thead nil
      (dom/tr nil
        (mapv
          (fn [a] (dom/th {:key (str (ao/qualified-key a))} (form/field-label env a)))
          attrs)
        (when can-add?
          (dom/th nil ent/nbsp))))))

(defmethod fr/render-footer [:default :table] [{::form/keys [form-instance] :as env} {::attr/keys [qualified-key] :as field-attr}]
  (let [{ItemForm    ::form/ui
         ::form/keys [can-add?]} (comp/component-options form-instance fo/subforms qualified-key)
        can-add? (?! can-add? form-instance field-attr)]
    (when can-add?
      (dom/tfoot nil
        (dom/tr nil
          (dom/td nil
           (dom/button :.ui.icon.button
             {:onClick (fn []
                         (form/add-child! form-instance qualified-key ItemForm {:order :append}))}
             (dom/i :.plus.icon))))))))

(defmethod fr/render-form [:default :table] [{::form/keys [parent parent-relation form-instance] :as renv} _]
  (let [{::form/keys [attributes]} (comp/component-options form-instance)
        {::form/keys [can-delete?]} (comp/component-options parent fo/subforms parent-relation)
        can-delete? (?! can-delete? parent (comp/props form-instance))]
    (dom/tr {:key (str (comp/get-ident form-instance))}
      (mapv (fn [attr]
              (dom/td {:key (str (ao/qualified-key attr))}
                (fr/render-field renv (assoc attr fo/omit-label? true)))) attributes)
      (when can-delete?
        (dom/td {:style {:verticalAlign "middle"}}
          (dom/button :.ui.icon.button {:onClick (fn [] (form/delete-child! parent parent-relation (comp/get-ident form-instance)))}
            (dom/i :.times.icon)))))))
