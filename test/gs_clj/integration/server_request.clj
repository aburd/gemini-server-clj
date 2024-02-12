(ns gs-clj.integration.server-request
  (:require [clojure.java.shell :refer [sh]]
            [clojure.test :refer [deftest testing is]]
            [clojure.java.io :as io]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [gs-clj.server :as server]
            [taoensso.timbre :as log]
            [gs-clj.request :as request]))

(defn get-checksum
  [path]
  (->> path
       (sh "md5sum" "-b")
       :out
       (take-while #(not= % \space))
       (apply str)))

(deftest checksum-works
  (testing "can call md5checksum on values"
    (is (=
         (sh "echo" "foo" "|" "md5checksum")
         (sh "echo" "foo" "|" "md5checksum")))))

(def in-path "resources/public/pictures/joey.jpg")
(def out-path "/tmp/joey.jpg")

; (def stream (s/stream))
; (def b (request/get-file-body in-path))
; (def out (io/output-stream out-path))
; @(s/put! stream b)
; (.write out (byte-array @(s/take! stream)))

(deftest can-write-same-file
  (testing "can call md5checksum on values"
    (with-open [temp (io/output-stream out-path)]
      (let [bytes (request/get-file-body in-path)]
        (.write temp (byte-array bytes))
        (is (=
             (get-checksum in-path)
             (get-checksum out-path)))))))

(deftest test-closed-and-drained
  (testing "foo"
    (let [s (s/stream)]
      (s/put! s 1)
      (is (= false (s/closed? s)))

      (s/close! s)

      (is (= false @(s/put! s 2)))
      (is (= true (s/closed? s)))
      (is (= false (s/drained? s)))
      (is (= 1 @(s/take! s)))
      (is (= nil @(s/take! s)))
      (is (= true (s/drained? s))))))
