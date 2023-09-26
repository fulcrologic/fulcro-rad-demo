(ns com.example.ui.form-rendering
  (:require
    #?(:cljs [com.fulcrologic.fulcro.dom :as dom :refer [div]]
       :clj  [com.fulcrologic.fulcro.dom-server :as dom :refer [div]])
    [com.fulcrologic.fulcro-i18n.i18n :refer [tr]]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.rad.attributes :as attr]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.control :as control]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.form-render :as fr]
    [com.fulcrologic.rad.options-util :refer [?!]]
    [com.fulcrologic.rad.rendering.semantic-ui.form :as rsf]
    [com.fulcrologic.rad.semantic-ui-options :as suo]
    [taoensso.timbre :as log]))

(defn install!
  "Install multimethod rendering such that:

   * All keywords in the RAD model will derive from :default.
   * A form can use `fro/style :multimethod` to switch from the built-in plugin rendering to multimethod rendering.
  "
  [app attrs]
  (fr/allow-defaults! attrs)
  (fr/install-as! app :multimethod))

;; Need this so that the plugin's way of rendering refs is used by default when rendering with multimethods
(defmethod fr/render-field [:ref :default] [{::form/keys [form-instance] :as renv} field-attr]
  (rsf/standard-ref-container renv field-attr (comp/component-options form-instance)))

;; This gives us the identical rendering of the plugin for rendering fields when no style matches
(defmethod fr/render-fields :default [{::form/keys [form-instance] :as renv} attr]
  (rsf/standard-form-layout-renderer renv))

;; Main fallthrough to render forms that choose fro/style :multimethod
(defmethod fr/render-form [:default :multimethod] [{::form/keys [form-instance parent parent-relation master-form] :as renv} id-attr]
  (dom/div :.ui.container.form {:key (str (comp/get-ident form-instance))}
    (fr/render-header renv id-attr)
    (fr/render-fields renv id-attr)
    (fr/render-footer renv id-attr)))

;; This gives us the standard headings for forms and to-many . Factored out of SUI plugin
(defmethod fr/render-header :default [{::form/keys [master-form form-instance] :as env} attr]
  (let [nested?         (not= master-form form-instance)
        props           (comp/props form-instance)
        computed-props  (comp/get-computed props)
        {::form/keys [title action-buttons controls show-header?]} (comp/component-options form-instance)
        title           (?! title form-instance props)
        action-buttons  (if action-buttons action-buttons form/standard-action-buttons)
        show-header?    (cond
                          (some? show-header?) (?! show-header? master-form)
                          (some? (fo/show-header? computed-props)) (?! (fo/show-header? computed-props) master-form)
                          :else true)
        {::form/keys [can-delete?]} computed-props
        read-only-form? (or
                          (?! (comp/component-options form-instance ::form/read-only?) form-instance)
                          (?! (comp/component-options master-form ::form/read-only?) master-form))

        {:ui/keys    [new?]
         ::form/keys [errors]} props
        invalid?        (if read-only-form? false (form/invalid? env))
        errors?         (or invalid? (seq errors))]
    (if nested?
      (div {:className (or (?! (comp/component-options form-instance ::ref-element-class) env) "ui segment")}
        (div :.ui.form {:classes [(when errors? "error")]
                        :key     (str (comp/get-ident form-instance))}
          (when can-delete?
            (dom/button :.ui.icon.primary.right.floated.button {:disabled (not can-delete?)
                                                                :onClick  (fn [] (form/delete-child! env))}
              (dom/i :.times.icon)))))
      (div {:key       (str (comp/get-ident form-instance))
            :className (or
                         (?! (suo/get-rendering-options form-instance suo/layout-class) env)
                         (?! (comp/component-options form-instance suo/layout-class) env)
                         (?! (comp/component-options form-instance ::top-level-class) env)
                         "ui container")}
        (when show-header?
          (div {:className (or
                             (?! (suo/get-rendering-options form-instance suo/controls-class) env)
                             (?! (comp/component-options form-instance ::controls-class) env)
                             "ui top attached segment")}
            (div {:style {:display        "flex"
                          :justifyContent "space-between"
                          :flexWrap       "wrap"}}
              (dom/h3 :.ui.header {:style {:wordWrap "break-word" :maxWidth "100%"}}
                title)
              (div :.ui.buttons {:style {:textAlign "right" :display "inline" :flexGrow "1"}}
                (keep #(control/render-control master-form %) action-buttons)))))
        (div {:classes [(or (?! (comp/component-options form-instance ::form-class) env) "ui attached form")
                        (when errors? "error")]}
          (when invalid?
            (div :.ui.red.message (tr "The form has errors and cannot be saved.")))
          (when (seq errors)
            (div :.ui.red.message
              (div :.content
                (dom/div :.ui.list
                  (map-indexed
                    (fn [idx {:keys [message]}]
                      (dom/div :.item {:key (str idx)}
                        (dom/i :.triangle.exclamation.icon)
                        (div :.content (str message))))
                    errors))
                (when-not new?
                  (dom/a {:onClick (fn []
                                     (form/undo-via-load! env))} (tr "Reload from server")))))))))))

(defmethod fr/render-footer :default [renv attr])

;; The rest of the methods are for rendering to-many subforms as a table.
;; Rendering the to-many field must wrap things in the table, and wrap the subforms (which will render as rows) in a tbody.
(defmethod fr/render-field [:ref :table] [{::form/keys [form-instance] :as renv} field-attr]
  (let [relation-key (ao/qualified-key field-attr)
        item         (-> form-instance comp/props relation-key)
        ItemForm     (form/subform-ui (comp/component-options form-instance) field-attr)
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

;; Rendering the header/footer for a ref attribute (we call that in the prior method) will happen in the context of the
;; owning form and the ref attribute.
;; We aren't supporting using this on top-level forms, so we output a warning div if it gets used that way
(defmethod fr/render-header [:default :table] [{::form/keys [form-instance] :as env} field-attr]
  (if (ao/identity? field-attr)
    (dom/div "Table header only supports rendering as a subform")
    (let [{ItemForm    ::form/ui
           ::form/keys [can-add?]} (fo/subform-options (comp/component-options form-instance) field-attr)
          attrs (comp/component-options ItemForm fo/attributes)]
      (dom/thead nil
        (dom/tr nil
          (mapv
            (fn [a] (dom/th {:key (str (ao/qualified-key a))} (form/field-label env a)))
            attrs)
          (when can-add?
            (dom/th nil ent/nbsp)))))))

(defmethod fr/render-footer [:default :table] [{::form/keys [form-instance] :as env} {::attr/keys [qualified-key] :as field-attr}]
  (if (ao/identity? field-attr)
    (dom/div "Table footer only supports rendering as a subform")
    (let [{ItemForm    ::form/ui
           ::form/keys [can-add?]} (fo/subform-options (comp/component-options form-instance) qualified-key)
          can-add? (?! can-add? form-instance field-attr)]
      (when can-add?
        (dom/tfoot nil
          (dom/tr nil
            (dom/td nil
              (dom/button :.ui.icon.button
                {:onClick (fn []
                            (form/add-child! form-instance qualified-key ItemForm {:order :append}))}
                (dom/i :.plus.icon)))))))))

;; Rendering the an actual form as a table row is supported. If table is used on a top-level form we just emit a warning div.
(defmethod fr/render-form [:default :table] [{::form/keys [parent parent-relation form-instance] :as renv} idattr]
  (if parent
    (let [{::form/keys [attributes]} (comp/component-options form-instance)
          {::form/keys [can-delete?]} (fo/subform-options (comp/component-options parent) parent-relation)
          can-delete? (?! can-delete? parent (comp/props form-instance))]
      (dom/tr {:key (str (comp/get-ident form-instance))}
        (mapv (fn [attr]
                (dom/td {:key (str (ao/qualified-key attr))}
                  (form/render-input renv attr))) attributes)
        (when can-delete?
          (dom/td {:style {:verticalAlign "middle"}}
            (dom/button :.ui.icon.button {:onClick (fn [] (form/delete-child! parent parent-relation (comp/get-ident form-instance)))}
              (dom/i :.times.icon))))))
    (dom/div nil "Rendering top-level forms as a table is not supported.")))
