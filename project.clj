(defproject gs-clj "0.1.0-SNAPSHOT"
  :description "A gemini server written in Clojure"
  :url "https://github.com/aburd/gemini-server-clj"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [aleph "0.7.1"]
                 [org.clj-commons/gloss "0.3.6"]]
  :repl-options {:init-ns gs-clj.core}
  :main gs-clj.core)

