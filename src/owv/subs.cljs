(ns owv.subs
  (:require [owv.date :refer [days-since minutes-since]]
            [re-frame.core :refer [reg-sub]]))

(reg-sub :loading-todos? #(:loading-todos? %))

(reg-sub :config-panel-open? #(:config-panel-open? %))

(reg-sub :data-url #(:data-url %))

(reg-sub :todos #(:todos %))

(reg-sub :grouped-todos
  :<- [:todos]
  (fn [todos]
    (group-by #(first (:tags %)) todos)))

(reg-sub :tags
  :<- [:grouped-todos]
  (fn [grouped-todos]
    (keys grouped-todos)))

(reg-sub :get-todos
  :<- [:grouped-todos]
  (fn [grouped-todos [_ tag]]
    (get grouped-todos tag)))

(reg-sub :last-updated-at #(:updated-at %))

(reg-sub :last-updated-minutes-ago
  :<- [:last-updated-at]
  (fn [d]
    (minutes-since d)))
