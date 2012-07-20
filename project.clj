(defproject portage "0.1.0-SNAPSHOT"
  :description "You chart the course, we'll handle the async rapids."
  :url "https://github.com/gregspurrier/portage"
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]]
                   :plugins [[lein-midje "2.0.0-SNAPSHOT"]]}})
