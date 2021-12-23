(ns owv.events
  (:require [owv.local-storage :as local-storage]
            [owv.todo-api :as todo-api]
            [re-frame.core :refer [after inject-cofx reg-event-db reg-event-fx]]))

(reg-event-fx :init
  [(inject-cofx ::local-storage/get "owv.todos-url")
   (inject-cofx ::local-storage/read "owv.new-todos")]
  (fn [{url "owv.todos-url"
        new-todos "owv.new-todos"} _]
    {:db {:todos-url url
          :new-todos new-todos}
     :fx [[:dispatch (if url
                       [:load-todos url]
                       [:show-config-panel])]]}))

(reg-event-fx :set-todos-url
  (fn [{:keys [db]} [_ url]]
    {:db (assoc db :todos-url url)
     ::local-storage/set {"owv.todos-url" url}
     :fx [[:dispatch [:hide-settings-pane]] ; does this belong here?
          [:dispatch [:load-todos url]]]}))

(reg-event-fx :load-todos
  (fn [{:keys [db]} [_ url]]
    {:db (assoc db :loading-todos? true)
     ::todo-api/fetch url}))

(reg-event-db :todos-loaded
  (fn [db [_ {:keys [todos updated-at]}]]
    (assoc db :todos todos :updated-at updated-at :loading-todos? false)))

(def persist-new-todos
  (after #(local-storage/set "owv.new-todos" (str (:new-todos %)))))

(reg-event-fx :add-todo
  [persist-new-todos]
  (fn [{:keys [db]} [_ todo]]
    {:db (update db :new-todos conj todo)
     :fx [[:dispatch [:hide-new-todo-panel]]]}))

(reg-event-db :delete-todo
  [persist-new-todos]
  (fn [db [_ todo]]
    (update db :new-todos (fn [new-todos]
                            (remove #(= % todo) new-todos)))))

;; TODO: clean up boilerplate

(defn show-settings-panel [db id]
  (assoc db :settings-pane-state id))

(reg-event-db :show-settings-pane
  #(show-settings-panel % :initial))

(reg-event-db :hide-settings-pane
  #(show-settings-panel % nil))

(reg-event-db :show-new-todo-panel
  #(show-settings-panel % :new-todo))

(reg-event-db :hide-new-todo-panel
  #(show-settings-panel % :initial))

(reg-event-db :show-review-new-todos-panel
  #(show-settings-panel % :review-new-todos))

(reg-event-db :hide-review-new-todos-panel
  #(show-settings-panel % :initial))

(reg-event-db :show-config-panel
  #(show-settings-panel % :config))

(reg-event-db :hide-config-panel
  #(show-settings-panel % :initial))
