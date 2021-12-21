(ns owv.subs
  (:require [owv.date :refer [days-since minutes-since]]
            [owv.todo :as todo]
            [re-frame.core :refer [reg-sub subscribe]]))

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

(reg-sub :tagged-todos
  :<- [:grouped-todos]
  (fn [grouped-todos [_ tag]]
    (get grouped-todos tag)))

(defn reg-tagged-todo-filtering-sub [name pred]
  (reg-sub name
    (fn [[_ tag]]
      (subscribe [:tagged-todos tag]))
    (fn [todos _]
      (filter pred todos))))

(reg-tagged-todo-filtering-sub :tagged-ready-todos todo/ready?)
(reg-tagged-todo-filtering-sub :tagged-todos-needing-attention todo/needs-attention?)
(reg-tagged-todo-filtering-sub :tagged-overdue-todos todo/overdue?)

(reg-sub :last-updated-at #(:updated-at %))

(reg-sub :last-updated-minutes-ago
  :<- [:last-updated-at]
  (fn [d]
    (minutes-since d)))
