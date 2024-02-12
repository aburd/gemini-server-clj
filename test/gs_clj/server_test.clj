(ns gs-clj.server-test
  (:require [manifold.stream :as s]
            [gs-clj.server :as server]
            [gs-clj.request :as request]
            [gs-clj.utils :refer [slurp-bytes]]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [taoensso.timbre :as log]))

(log/set-min-level! :info)

(defn get-checksum
  [path]
  (->> path
       (sh "md5sum" "-b")
       :out
       (take-while #(not= % \space))
       (apply str)))

(def in-file "resources/test-public/pictures/joey-cig.png")
(def out-file "/tmp/joey.png")

(defn clean-up-tmp [f]
  (f)
  (try
    (io/delete-file out-file)
    (catch Exception e)))

(use-fixtures :each clean-up-tmp)

(deftest test-send-image
  (with-open [temp (io/output-stream out-file)]
    (let [img-bytes (slurp-bytes in-file)
          s (s/stream)
          wrapped s]
      (s/put! wrapped img-bytes)
      (s/close! wrapped)
      (.write temp (byte-array @(s/take! wrapped)))
      (is (=
           (get-checksum in-file)
           (get-checksum out-file))))))

(deftest test-get-gemini
  (testing "can get index.gmi with no path"
    (let [s (s/stream)
          f (server/wrap-tcp-stream request/handle! {:public-path "resources/test-public"})]
      (s/put! s "gemini://localhost\r\n")
      (s/close! s)
      (is (nil? @(f s s  {}))))))

(deftest test-get-png
  (testing "can send png"
    (let [s (s/stream)
          f (server/wrap-tcp-stream request/handle! {:public-path "resources/test-public"})]
      (s/put! s "gemini://localhost/test-public/pictures/small.png\r\n")
      (s/close! s)
      (is (nil? @(f s s  {}))))))
