(defproject gs-clj "0.1.0-SNAPSHOT"
  :description "A gemini server written in Clojure. This implementation is based on the [gemini spec](https://geminiprotocol.net/docs/specification.gmi) as of v0.16.1, January 30th 2022"
  :url "https://github.com/aburd/gemini-server-clj"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [aleph "0.7.1"]
                 [org.clj-commons/gloss "0.3.6"]
                 [manifold "0.4.2"]
                 [org.clj-commons/pretty "2.2.1"]
                 [com.taoensso/timbre "6.3.1"]
                 [org.clojure/tools.cli "1.0.219"]]
  :repl-options {:init-ns gs-clj.core}
  :main gs-clj.core)

