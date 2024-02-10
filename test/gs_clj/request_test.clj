(ns gs-clj.request-test
  (:require [clojure.test :refer [deftest testing is]]
            [gs-clj.request :refer [from-str]]
            [gs-clj.gemini :refer [clrf max-request-bytes]]))

(def invalid-uris
  (map
   #(str % clrf)
   ["gemini://user@localhost/"
    "gemini://user:pass@localhost/"
    ; request too large
    (str "gemini://localhost/" (apply str (take max-request-bytes (repeat "a"))))
    ; wrong protocol
    "http://bennyboy"
    ; fragments not allowed
    "gemini://ohyeah5.com/some/path?some=query#whee-lets-go"
    ; not allowed to use an IP for authority
    "gemini://127.0.0.1"]))

(def valid-uris
  (map
   #(str % clrf)
   ["gemini://localhost/"
    "gemini://ohyeah1.org"
    "gemini://ohyeah2.com"
    "gemini://ohyeah3.com/some/path"
    "gemini://ohyeah4.com/some/path?some=query"
    "gemini://ohyeah6"]))

(deftest from-str-non-gemini
  (testing "invalid uris return nil"
    (is (every? nil? (map from-str invalid-uris)))))

(deftest from-str-gemini
  (testing "valid uris return non-nil"
    (is (every? (comp not nil?) (map from-str valid-uris)))))
