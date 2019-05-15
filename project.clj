(defproject arcadia.nrepl "0.1.0-SNAPSHOT"
  :description "nREPL bridge for Arcadia Unity"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [nrepl "0.6.0"]
                 [org.clojure/tools.cli "0.4.2"]]
  :main ^:skip-aot arcadia.nrepl
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
