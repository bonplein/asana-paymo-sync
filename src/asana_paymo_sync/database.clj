(ns asana-paymo-sync.database
  (:require [clojure.java.jdbc :as jdbc]
            [yesql.core :refer :all]))

;; insert mapping (:asana :paymo)
(defquery insert-mapping! "asana_paymo_sync/sql/insert_mapping.sql")

;; get :paymo by :asana
(defquery paymo-by-asana "asana_paymo_sync/sql/paymo_by_asana.sql")
