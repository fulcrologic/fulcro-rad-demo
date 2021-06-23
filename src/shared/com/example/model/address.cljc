(ns com.example.model.address
  (:require
    [com.fulcrologic.rad.attributes :as attr :refer [defattr]]
    [com.fulcrologic.rad.attributes-options :as ao]
    [com.fulcrologic.rad.authorization :as auth]))

(defattr id :address/id :uuid
  {ao/identity?                                     true
   :com.fulcrologic.rad.database-adapters.sql/table "address"
   ao/schema                                        :production})

(defattr street :address/street :string
  {ao/schema     :production
   ao/identities #{:address/id}})

(defattr city :address/city :string
  {ao/schema     :production
   ao/identities #{:address/id}})

(def states #:address.state {:AL "Alabama"
                             :AK "Alaska"
                             :AS "American Samoa"
                             :AZ "Arizona"
                             :AR "Arkansas"
                             :CA "California"
                             :CO "Colorado"
                             :CT "Connecticut"
                             :DE "Delaware"
                             :DC "District of Columbia"
                             :FL "Florida"
                             :GA "Georgia"
                             :GU "Guam"
                             :HI "Hawaii"
                             :ID "Idaho"
                             :IL "Illinois"
                             :IN "Indiana"
                             :IA "Iowa"
                             :KS "Kansas"
                             :KY "Kentucky"
                             :LA "Louisiana"
                             :ME "Maine"
                             :MD "Maryland"
                             :MA "Massachusetts"
                             :MI "Michigan"
                             :MN "Minnesota"
                             :MS "Mississippi"
                             :MO "Missouri"
                             :MT "Montana"
                             :NE "Nebraska"
                             :NV "Nevada"
                             :NH "New Hampshire"
                             :NJ "New Jersey"
                             :NM "New Mexico"
                             :NY "New York"
                             :NC " North Carolina"
                             :ND "North Dakota"
                             :MP "Northern Mariana Islands"
                             :OH "Ohio"
                             :OK "Oklahoma"
                             :OR "Oregon"
                             :PA "Pennsylvania"
                             :PR "Puerto Rico"
                             :RI "Rhode Island"
                             :SC "South Carolina"
                             :SD "South Dakota"
                             :TN "Tennessee"
                             :TX "Texas"
                             :UT "Utah"
                             :VT "Vermont"
                             :VA "Virginia"
                             :VI "Virgin Islands"
                             :WA "Washington"
                             :WV "West Virginia"
                             :WI "Wisconsin"
                             :WY "Wyoming"})

(defattr state :address/state :enum
  {ao/enumerated-values (set (keys states))
   ao/identities        #{:address/id}
   ao/schema            :production
   ao/enumerated-labels states})

(defattr zip :address/zip :string
  {ao/identities #{:address/id}
   ao/schema     :production})

(def attributes [id street city state zip])
