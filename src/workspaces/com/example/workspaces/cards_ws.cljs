(ns com.example.workspaces.cards-ws
  (:require [com.fulcrologic.fulcro.components :as fp]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [com.fulcrologic.fulcro.mutations :as fm]
            ["react-tag-input" :rename {WithContext ReactTags}]
            [com.example.ui.location-field :as l.field]
            [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
            [com.fulcrologic.fulcro.dom :as dom]))



(fp/defsc FulcroDemo
  [this {:keys [counter]}]
  {:initial-state (fn [_] {:counter 0})
   :ident         (fn [] [::id "singleton"])
   :query         [:counter]}
  (dom/div
   (str "Fulcro counter demo [" counter "]")
   (dom/button {:onClick #(fm/set-value! this :counter (inc counter))} "+")))

(def react-tags (interop/react-factory ReactTags))

(let [keycode-delims [10 13 188]]
  (fp/defsc TaggedInput
            [this {:keys [tags suggestions]}]
            {:initial-state {:tags [] :suggestions []}
             :ident (fn [] [::id "tagged-input"])
             :query [:tags :suggestions]}
            (let [handle-delete #(fm/set-value!! this :tags (keep-indexed (fn [i v] (when-not (= % i) v)) tags))
                  handle-addition #(fm/set-value!! this :tags (conj tags %))
                  handle-drag #(fm/set-value!! this :tags (transduce
                                                           (map-indexed (fn [i v] [i v]))
                                                           (completing (fn [a [i v]]
                                                                         (cond
                                                                           (= i %3) (conj a %1 v)
                                                                           (= i %2) a
                                                                           :else (conj a v))))
                                                           [] tags))]
              (react-tags {:delimiters keycode-delims
                          :tags tags
                          :suggestions suggestions
                           :handleDelete handle-delete
                           :handleAddition handle-addition
                           :handleDrag handle-drag}))))

(ws/defcard fulcro-demo-card
  (ct.fulcro/fulcro-card
   {::ct.fulcro/root FulcroDemo
    ::ct.fulcro/wrap-root? true}))

(ws/defcard tagged-input-card
            (ct.fulcro/fulcro-card
             {::ct.fulcro/root TaggedInput
              ::ct.fulcro/wrap-root? true}))


(ws/defcard tagged-input-card2)



