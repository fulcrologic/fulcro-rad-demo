(ns com.example.ui.location-field
  (:require
   #?@(:cljs
       [["react" :as react]
        ["react-tag-input" :rename {WithContext ReactTags}]
        [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
        [goog.object :as gobj]])
   [clojure.string :as str]
   [com.fulcrologic.fulcro.dom.inputs :as d.inputs]
   [com.fulcrologic.rad.rendering.semantic-ui.field :refer [render-field-factory]]
   [com.fulcrologic.fulcro.components :as comp]))

(defn db->val
  "Convert db representation to react tag input repr"
  [db-val]
  (if (seq db-val)
    (mapv (fn [v] {:id v :text v}) db-val)
    []))

(defn val->db
  "Convert tagged input val to db representation"
  [input-val]
  (if (seq input-val) (vec (keep (fn [v] (:text v)) input-val)) []))


(defn propagate-changes [this ntv]
  (let [{:keys [value onChange]} (comp/props this)
        nv (val->db ntv)]
    (comp/set-state! this {:tags  ntv
                           :oldPropValue value
                           :value        nv})
    (when (and onChange (not= value nv))
      (onChange nv))))


#?(:cljs
   (def react-tags (interop/react-factory ReactTags)))

(let [keycode-delims [10 13 188]]

  (def TaggedInput
    "Wrapper for tagged input React Libraries. 
   Compatible with Fulcro RAD Forms Input"
    (let [cls (fn [props]
                #?(:cljs
                   (cljs.core/this-as this
                                      (let [props         (gobj/get props "fulcro$value")
                                            {:keys [value suggestions]} props

                                            initial-state {:oldPropValue value
                                                           :handle-delete (fn [deleted-val]
                                                                            (let [{:keys [tags]} (comp/get-state this)
                                                                                  ntv (keep-indexed
                                                                                       (fn [i v] (when-not (= deleted-val i) v))
                                                                                       tags)]
                                                                              (propagate-changes this ntv)))
                                                           :handle-addition (fn [added-val]
                                                                              (let [{:keys [tags]} (comp/get-state this)
                                                                                    ntv (conj tags (js->clj added-val :keywordize-keys true))]
                                                                                (propagate-changes this ntv)))
                                                           :handle-drag (fn [new-val curr-pos new-pos]
                                                                          (let [{:keys [tags]} (comp/get-state this)
                                                                                ntv (transduce
                                                                                     (map-indexed (fn [i v] [i v]))
                                                                                     (completing (fn [a [i v]]
                                                                                                   (cond
                                                                                                     (= i new-pos) (conj a (js->clj
                                                                                                                            new-val
                                                                                                                            :keywordize-keys true) v)
                                                                                                     (= i curr-pos) a
                                                                                                     :else (conj a v))))
                                                                                     [] tags)]
                                                                            (propagate-changes this ntv)))
                                                           :tags  (db->val value)
                                                           :suggestions (db->val suggestions)}]
                                        (set! (.-state this) (cljs.core/js-obj "fulcro$state" initial-state)))
                                      nil)))]
      (comp/configure-component! cls ::TaggedInput
                                 {:getDerivedStateFromProps
                                  (fn [latest-props state]
                                    (let [{:keys [value suggestions]} latest-props
                                          {:keys [oldPropValue]} state
                                          changedPropValue?  (or (not= oldPropValue value) (not= value (:value state)))
                                          changedSuggestions?  (not= (or suggestions []) (:suggestions state))
                                          new-derived-state      (cond-> (assoc state :oldPropValue value)
                                                                   changedPropValue? (assoc :tags
                                                                                            (db->val value))
                                                                   changedSuggestions? (assoc :suggestions (db->val suggestions)))]
                                      #js {"fulcro$state" new-derived-state}))
                                  :render
                                  (fn [this]
                                    #?(:cljs
                                       (let [{:keys [value onBlur] :as props} (comp/props this)
                                             {:keys [tags suggestions handle-delete handle-addition handle-drag]} (comp/get-state this)]
                                         (react-tags
                                          (merge props
                                                 (cond->
                                                  {:tags tags
                                                   :delimiters keycode-delims
                                                   :suggestions suggestions
                                                   :handleDelete handle-delete
                                                   :handleAddition handle-addition
                                                   :handleDrag handle-drag}
                                                   onBlur (assoc :onBlur (fn [_]
                                                                           (js/console.log "blur called")
                                                                           (onBlur value)))))))))})
      (comp/register-component! ::TaggedInput cls)
      cls)))

(def ui-tag-input
  "A vector input using the React Tag Library"
  (comp/factory TaggedInput))

(def ui-vector-input
  "A vector input. Used just like a DOM input, but requires you supply nil or a vector of strings for `:value`, and
   will send a vector to `onChange` and `onBlur`. Any other attributes in props are passed directly to the
   underlying `dom/input`."
  (comp/factory (d.inputs/StringBufferedInput ::VectorInput {:model->string #(str/join ", " %)
                                                             :string->model #(str/split % #", ")})))


(def tag-render-field (render-field-factory ui-tag-input))
(def vector-render-field (render-field-factory ui-vector-input))