(defproject asana-paymo-sync "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1"]
                 [cheshire "5.4.0"]
                 [mysql/mysql-connector-java "5.1.32"]
                 [yesql "0.4.0"]
                 [environ "1.0.0"]]
  :plugins [[lein-environ "1.0.0"]]
  :uberjar-name "asana-paymo-sync-standalone.jar"
  :main asana-paymo-sync.core
  :aot :all)
