(defproject asana-paymo-sync "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1"]
                 [cheshire "5.4.0"]
                 [environ "1.0.0"]
                 [com.taoensso/faraday "1.5.0" :exclusions [org.clojure/clojure]]]
  :plugins [[lein-environ "1.0.0"]]
  :profiles {:dev {:dependencies [[clj-http-fake "1.0.1"]]}}
  :uberjar-name "asana-paymo-sync-standalone.jar"
  :min-lein-version "2.5.0"
  :main asana-paymo-sync.core
  :aot [asana-paymo-sync.core])
