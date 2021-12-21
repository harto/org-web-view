(ns owv.todo
  (:require [owv.date :refer [days-since days-until]]))

(defn ready? [todo]
  (or (= 0 (days-until (:scheduled todo)))
      (<= 3 (days-until (:deadline todo)) 7)))

(defn needs-attention? [todo]
  (or (> (days-since (:scheduled todo)) 0)
      (<= 0 (days-until (:deadline todo)) 2)))

(defn overdue? [todo]
  (> (days-since (:deadline todo)) 0))

(defn stale? [todo]
  (and (not (:scheduled todo)) (> (days-since (:created todo)) 30)))
