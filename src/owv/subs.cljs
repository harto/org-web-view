(ns owv.subs
  (:require [owv.date :refer [days-since minutes-since]]
            [owv.todo :as todo]
            [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub :loading-todos? #(:loading-todos? %))

(reg-sub :show-settings-pane? #(:settings-panel %))
(reg-sub :visible-settings-panel #(:settings-panel %))

(reg-sub :todos-url #(:todos-url %))

(reg-sub :all-todos #(:todos %))
(reg-sub :new-todos #(:new-todos %))
(reg-sub :completed-todos #(:completed-todos %))

(reg-sub :active-todos
  :<- [:all-todos]
  :<- [:completed-todos]
  (fn [[all-todos completed-todos]]
    (remove completed-todos all-todos)))

(reg-sub :grouped-todos
  :<- [:active-todos]
  (fn [todos]
    (group-by #(first (:tags %)) todos)))

(reg-sub :tags
  :<- [:grouped-todos]
  (fn [grouped-todos]
    (sort (keys grouped-todos))))

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
