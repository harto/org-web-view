(ns owv.events
  (:require [owv.todos :as todos]
            [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx :init
  (fn [_ _]
    {:db {}
     ::todos/init :_}))

(reg-event-fx :load-todos
  (fn [{:keys [db]} [_ data-url]]
    {:db (assoc db
                :data-url data-url
                :loading-todos? true)
     ::todos/save-url data-url
     ::todos/fetch data-url}))

(reg-event-db :todos-loaded
  (fn [db [_ {:keys [todos updated-at]}]]
    (assoc db :todos todos :updated-at updated-at :loading-todos? false)))
