(ns gs-clj.utils
  (:require [clojure.string :as string]
            [clojure.java.io :as io]))

(defn emphasize
  [s & {:keys [len c] :or {len 20 c '*}}]
  (let [s-len (count s)
        star-count (/ (- len s-len) 2)]
    (if (> s-len len)
      s
      (let [stars (apply str (take star-count (repeat c)))]
        (str stars (string/upper-case s) stars)))))

(defn- byte-len [s]
  (alength (.getBytes s "UTF-8")))

(defn byte-len-within [s len]
  (< (byte-len s) len))

(defn pick
  "return a sequence of the vals at the keys of the provided map"
  [m & ks]
  (vals (select-keys m ks)))

(defn slurp-bytes
  [^String path]
  (when (.exists (io/file path))
    (with-open [in (io/input-stream path)
                out (java.io.ByteArrayOutputStream.)]
      (io/copy in out)
      (.toByteArray out))))

(slurp-bytes "resources/public/pictures/joey-cig.png")
