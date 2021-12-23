(ns owv.local-storage
  (:refer-clojure :exclude [get set])
  (:require [cljs.reader :refer [read-string]]
            [re-frame.core :refer [reg-cofx reg-fx]]))

(defn get [k]
  (.getItem js/localStorage k))

(defn read [k]
  (if-let [v (get k)]
    (try
      (read-string v)
      (catch :default e
        (.error js/console e)))))

(defn set [k v]
  (.setItem js/localStorage k v))

(reg-cofx ::get
  (fn [cofx k]
    (assoc cofx k (get k))))

(reg-cofx ::read
  (fn [cofx k]
    (assoc cofx k (read k))))

(reg-fx ::set
  (fn [kvs]
    (doseq [[k v] kvs]
      (set k v))))
