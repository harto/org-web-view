(ns owv.local-storage
  (:require [cljs.reader :refer [read-string]]
            [re-frame.core :refer [reg-cofx reg-fx]]))

(defn read [k]
  (if-let [v (.getItem js/localStorage k)]
    (try
      (read-string v)
      (catch :default e
        (.error js/console e)))))

(defn write [k v]
  (.setItem js/localStorage k (pr-str v)))

(reg-cofx ::read
  (fn [cofx kvs]
    (into cofx (for [[k v] kvs]
                 [v (read k)]))))

(reg-fx ::write
  (fn [kvs]
    (doseq [[k v] kvs]
      (write k v))))
