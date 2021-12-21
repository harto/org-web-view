(ns owv.events
  (:require [owv.todo-api :as todo-api]
            [re-frame.core :refer [reg-event-db reg-event-fx]]))

(reg-event-fx :init
  (fn [_ _]
    {:db {}
     ::todo-api/init :_}))

(reg-event-fx :load-todos
  (fn [{:keys [db]} [_ data-url]]
    {:db (assoc db
                :data-url data-url
                :loading-todos? true)
     ::todo-api/save-url data-url
     ::todo-api/fetch data-url}))

(reg-event-db :todos-loaded
  (fn [db [_ {:keys [todos updated-at]}]]
    (assoc db :todos todos :updated-at updated-at :loading-todos? false)))
