(ns asana-paymo-sync.database
  (:require [taoensso.faraday :as far]
            [asana-paymo-sync.config :as config]))

(defn longify
  "Converts the number into a long, leaves nil"
  [value]
  (if (nil? value)
    nil
    (long value)))

(defn get-tuple
  "Get the correlating Paymo key"
  [asana-key]
  (-> (far/get-item config/dynamodb
                    (:table config/dynamodb)
                    {:asana asana-key})
      :paymo
      longify))

(defn set-tuple
  "Set the Asana and correlating Paymo key"
  [asana-key paymo-key]
  (far/put-item config/dynamodb
                (:table config/dynamodb)
                {:asana asana-key
                 :paymo paymo-key}))

