(ns gs-clj.gemini
  (:require [gloss.core :as gloss]))

(def default-port 1965)

(def max-request-bytes 1024)
(def max-response-meta-bytes 1024)
(def clrf "\r\n")

(def request-protocol
  (gloss/compile-frame
   (gloss/string :utf-8 :length max-request-bytes :suffix clrf)))

; TODO
(def response-protocol
  (gloss/compile-frame
   (gloss/string :utf8 :suffix clrf)))
