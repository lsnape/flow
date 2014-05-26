(defproject jarohen/flow "0.1.0"
  :description "Lightweight library to help you write dynamic CLJS webapps"

  :url "https://github.com/james-henderson/flow"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]]

  :plugins [[com.keminglabs/cljx "0.3.2"]]

  :source-paths ["src" "target/generated/clj"]
  
  :hooks [cljx.hooks]

  :filespecs [{:type :path
               :path "target/generated/cljs"}]

  :cljx {:builds [{:source-paths ["src"]
                   :output-path "target/generated/clj"
                   :rules :clj}

                  {:source-paths ["src"]
                   :output-path "target/generated/cljs"
                   :rules :cljs}]})
