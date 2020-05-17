(ns com.example.model.invoice
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.form :as form]
    [com.fulcrologic.rad.report-options :as ro]
    [com.wsscode.pathom.connect :as pc]
    [com.fulcrologic.rad.type-support.date-time :as datetime]
    [com.fulcrologic.rad.type-support.decimal :as math]
    #?(:clj [com.example.components.database-queries :as queries])
    [taoensso.timbre :as log]
    [com.fulcrologic.rad.type-support.date-time :as dt]
    [cljc.java-time.local-date-time :as ldt]
    [taoensso.encore :as enc]
    [cljc.java-time.local-date :as ld]
    [com.fulcrologic.rad.report :as report]))

(defattr id :invoice/id :uuid
  {ao/identity? true
   ;:com.fulcrologic.rad.database-adapters.datomic/native-id? true
   ao/schema    :production})

(defattr date :invoice/date :instant
  {::form/field-style           :date-at-noon
   ::datetime/default-time-zone "America/Los_Angeles"
   ao/identities                #{:invoice/id}
   ao/schema                    :production})

(defattr line-items :invoice/line-items :ref
  {ao/target                                                       :line-item/id
   :com.fulcrologic.rad.database-adapters.sql/delete-referent?     true
   :com.fulcrologic.rad.database-adapters.datomic/attribute-schema {:db/isComponent true}
   ao/cardinality                                                  :many
   ao/identities                                                   #{:invoice/id}
   ao/schema                                                       :production})

(defattr total :invoice/total :decimal
  {ao/identities      #{:invoice/id}
   ao/schema          :production
   ro/field-formatter (fn [report v] (math/numeric->currency-str v))
   ao/read-only?      true})

(defattr customer :invoice/customer :ref
  {ao/cardinality :one
   ao/target      :account/id
   ao/required?   true
   ao/identities  #{:invoice/id}
   ao/schema      :production})

;; Fold account details into the invoice details, if desired
#?(:clj
   (pc/defresolver customer-id [env {:invoice/keys [id]}]
     {::pc/input  #{:invoice/id}
      ::pc/output [:account/id]}
     {:account/id (queries/get-invoice-customer-id env id)}))

(defattr all-invoices :invoice/all-invoices :ref
  {ao/target     :invoice/id
   ao/pc-output  [{:invoice/all-invoices [:invoice/id]}]
   ao/pc-resolve (fn [{:keys [query-params] :as env} _]
                   #?(:clj
                      {:invoice/all-invoices (queries/get-all-invoices env query-params)}))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Statistics attributes.  Note that these are to-many, and are used by
;; reports that expect a given attribute to be grouped/filtered and possibly
;; aggregated values. Each of these statistics will output the same number of
;; items for given input groups, one for each group.
;;
;; These depend on the `groups` that are generated later in this file by the
;; `invoice-statistics` resolver.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defattr date-groups :invoice-statistics/date-groups :instant
  {ao/cardinality :many
   ao/style       :date
   ao/pc-input    #{:invoice-statistics/groups}
   ao/pc-output   [:invoice-statistics/date-groups]
   ao/pc-resolve  (fn [_ {:invoice-statistics/keys [groups]}]
                    {:invoice-statistics/date-groups (mapv :key groups)})})

(defattr gross-sales :invoice-statistics/gross-sales :decimal
  {ao/cardinality :many
   ao/style       :USD
   ao/pc-input    #{:invoice-statistics/groups}
   ao/pc-output   [:invoice-statistics/gross-sales]
   ao/pc-resolve  (fn [{:keys [query-params] :as env} {:invoice-statistics/keys [groups]}]
                    {:invoice-statistics/gross-sales (mapv (fn [{:keys [_ values]}]
                                                             (reduce
                                                               (fn [sales {:invoice/keys [total]}]
                                                                 (math/+ sales total))
                                                               (math/zero)
                                                               values)) groups)})})

(defattr items-sold :invoice-statistics/items-sold :int
  {ao/cardinality :many
   ao/pc-input    #{:invoice-statistics/groups}
   ao/pc-output   [:invoice-statistics/items-sold]
   ao/pc-resolve  (fn [{:keys [query-params] :as env} {:invoice-statistics/keys [groups]}]
                    {:invoice-statistics/items-sold (mapv (fn [{:keys [_ values]}]
                                                            (reduce
                                                              (fn [total {:invoice/keys [line-items]}]
                                                                (+ total (reduce
                                                                           (fn [m {:line-item/keys [quantity]}]
                                                                             (+ m quantity)) 0 line-items)))
                                                              0
                                                              values)) groups)})})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; This is the workhorse of statistics reporting at the Pathom layer. The following
;; resolver does the database query for all of the items needed to complete any
;; resolvers that are dependent upon the groups.  The groups are generated by
;; this resolver in a parameterized manner so that filtering, sorting, and grouping by
;; user parameters are accomplished here. Then this resolver makes those groups
;; available on output to be consumed by the resolvers (above) that then generate
;; derived statistics from them.  This prevents the resolvers from having to redo
;; the grouping calculations at the expense of having to make sure they are
;; calculated here.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
#?(:clj
   (pc/defresolver invoice-statistics [{:keys [parser query-params] :as env} _]
     {::pc/output [{:invoice-statistics [{:invoice-statistics/groups [:key :values]}]}]
      ::pc/doc    "Pull and group the invoices and line items based on query-params. This then flows to other resolvers as input."}
     ;; NOTE: you'd normally need to pass in tz as a param, or use it from session to localize the groupings.
     (let [{:keys    [start-date end-date]
            grouping :group-by} query-params
           ;; TODO: Actual start/end filter
           all-invoices     (get
                              (parser env [{:invoice/all-invoices [:invoice/id
                                                                   :invoice/date
                                                                   :invoice/total
                                                                   {:invoice/line-items [:line-item/quantity
                                                                                         :line-item/subtotal]}]}])
                              :invoice/all-invoices)
           ;; TODO: support date range on all-invoices
           invoices         (filterv (fn [{:invoice/keys [date]}]
                                       (and
                                         (or (nil? start-date) (<= (compare start-date date) 0))
                                         (or (nil? end-date) (<= (compare date end-date) 0))))
                              all-invoices)
           grouped-invoices (enc/map-keys (fn [ld] (if (keyword? ld)
                                                     ld
                                                     (dt/local-datetime->inst (ld/at-time ld 12 0))))
                              (group-by
                                (fn [{:invoice/keys [date]}]
                                  (case grouping
                                    :year (let [d (dt/inst->local-datetime date)]
                                            (-> (ldt/to-local-date d)
                                              (ld/with-day-of-month 1)
                                              (ld/with-month 1)))
                                    :month (let [d (dt/inst->local-datetime date)]
                                             (ld/with-day-of-month (ldt/to-local-date d) 1))
                                    :day (let [d (dt/inst->local-datetime date)]
                                           (ldt/to-local-date d))
                                    ;;default is summary
                                    :summary))
                                invoices))
           result           (reduce
                              (fn [result k]
                                (conj result {:key k :values (get grouped-invoices k)}))
                              []
                              (sort (keys grouped-invoices)))]
       {:invoice-statistics {:invoice-statistics/groups result}})))

(def attributes [id date line-items customer all-invoices total date-groups gross-sales items-sold])
#?(:clj
   (def resolvers [customer-id invoice-statistics]))

(comment
  (report/rotate-result
    {:invoice-statistics/date-groups ["1/1/2020" "2/1/2020" "3/1/2020" "4/1/2020"]
     :invoice-statistics/gross-sales [323M 313M 124M 884M]
     :invoice-statistics/items-sold  [10 11 5 42]}
    [date-groups gross-sales items-sold]))
